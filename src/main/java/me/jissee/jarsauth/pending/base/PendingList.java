package me.jissee.jarsauth.pending.base;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static me.jissee.jarsauth.data.TimeUtil.now;

/**
 * 严格串行、单轮验证的 PendingList
 *
 * 验证模式（构造期决定）：
 * - clientRequired = true  : 客户端参与校验
 * - clientRequired = false : 纯服务端校验
 *
 * 完全事件驱动（notify）模型
 */
public abstract class PendingList {

    /* ================= 失败类型 ================= */

    public enum FailureType {
        CALCULATION_ERROR("internal"),
        COMPARE_ERROR("mismatch"),
        TIMEOUT("timeout"),
        RESULT_MISMATCH("mismatch");

        private final String key;

        FailureType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /* ================= Context（状态机） ================= */

    protected static final class UserContext {
        final UUID userId;

        long lastVerificationTime;

        String randomData;
        String expectedResult;
        String clientResult;

        boolean inProgress;
        boolean finished;

        ScheduledFuture<?> timeoutTask;

        UserContext(UUID userId) {
            this.userId = userId;
            this.lastVerificationTime = now();
        }

        boolean expectedReady() {
            return expectedResult != null;
        }

        boolean clientReady() {
            return clientResult != null;
        }
    }

    /* ================= PendingList 级别配置 ================= */

    protected final MinecraftServer server;

    /** 是否需要客户端参与验证（构造期决定） */
    protected final boolean clientRequired;

    private final Map<UUID, UserContext> users = new ConcurrentHashMap<>();

    private long interval;
    private long timeout;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });

    /* ================= 构造 ================= */

    protected PendingList(MinecraftServer server, boolean clientRequired) {
        this.server = server;
        this.clientRequired = clientRequired;
        reload();
    }

    /* ================= 抽象接口 ================= */

    protected String generateRandom() {
        return UUID.randomUUID().toString();
    }

    protected abstract String calculateExpected(UUID userId, String random) throws Exception;

    /**
     * 裁决逻辑：
     * - clientRequired == true  : expected vs client
     * - clientRequired == false : 只看 expected（client 可能为 null）
     */
    protected abstract boolean compare(String expected, String actual);

    /**
     * 仅在 clientRequired == true 时调用
     */
    protected abstract void sendInfoToPlayer(UUID userId, String random);

    protected abstract void notifyFailure(UUID userId, String reason);

    protected abstract String formatReason(FailureType type, Exception e);

    protected abstract long getInterval();

    protected abstract long getTimeout();

    /* ================= 生命周期 ================= */

    public void reload() {
        this.interval = getInterval();
        this.timeout = getTimeout();
    }

    public void addUser(UUID userId) {
        UserContext ctx = new UserContext(userId);
        users.put(userId, ctx);
        scheduleNext(ctx, true);
    }

    public void removeUser(UUID userId) {
        UserContext ctx = users.remove(userId);
        if (ctx != null) {
            synchronized (ctx) {
                cancelTimeout(ctx);
                ctx.inProgress = false;
                ctx.finished = true;
            }
        }
    }

    /* ================= 客户端 notify ================= */

    public void onVerificationResponse(UUID userId, String clientResult) {
        if (!clientRequired) {
            // 纯服务端校验，不接受客户端返回
            return;
        }

        UserContext ctx = users.get(userId);
        if (ctx == null) return;

        synchronized (ctx) {
            if (!ctx.inProgress || ctx.finished) return;

            ctx.clientResult = clientResult;
            notifyContext(ctx);
        }
    }

    /* ================= 核心流程 ================= */

    private void startVerification(UserContext ctx) {
        synchronized (ctx) {
            if (ctx.inProgress) return;

            ctx.inProgress = true;
            ctx.finished = false;
            ctx.expectedResult = null;
            ctx.clientResult = null;

            ctx.randomData = generateRandom();
            ctx.lastVerificationTime = now();

            // 仅在需要客户端参与时发送 challenge
            if (clientRequired) {
                sendInfoToPlayer(ctx.userId, ctx.randomData);
            }

            scheduleTimeout(ctx);

            scheduler.execute(() -> {
                try {
                    String expected = calculateExpected(ctx.userId, ctx.randomData);
                    synchronized (ctx) {
                        if (ctx.finished) return;
                        ctx.expectedResult = expected;
                        notifyContext(ctx);
                    }
                } catch (Exception e) {
                    synchronized (ctx) {
                        fail(ctx, FailureType.CALCULATION_ERROR, e);
                    }
                }
            });
        }
    }

    /**
     * Context 的唯一状态推进点
     */
    private void notifyContext(UserContext ctx) {
        if (ctx.finished) return;
        if (!ctx.expectedReady()) return;

        // 客户端参与模式下才等待 client
        if (clientRequired && !ctx.clientReady()) return;

        completeVerification(ctx);
    }

    private void completeVerification(UserContext ctx) {
        boolean success;
        try {
            success = compare(ctx.expectedResult, ctx.clientResult);
        } catch (Exception e) {
            fail(ctx, FailureType.COMPARE_ERROR, e);
            return;
        }

        if (!success) {
            fail(ctx, FailureType.RESULT_MISMATCH, null);
            return;
        }

        succeed(ctx);
    }

    private void onTimeout(UserContext ctx) {
        synchronized (ctx) {
            if (!ctx.inProgress || ctx.finished) return;
            fail(ctx, FailureType.TIMEOUT, null);
        }
    }

    /* ================= 成功 / 失败 ================= */

    private void succeed(UserContext ctx) {
        ctx.finished = true;
        ctx.inProgress = false;
        cancelTimeout(ctx);
        scheduleNext(ctx, false);
    }

    private void fail(UserContext ctx, FailureType type, Exception e) {
        ctx.finished = true;
        ctx.inProgress = false;
        cancelTimeout(ctx);
        notifyFailure(ctx.userId, formatReason(type, e));
        removeUser(ctx.userId);
    }

    /* ================= 调度辅助 ================= */

    private void scheduleTimeout(UserContext ctx) {
        cancelTimeout(ctx);
        ctx.timeoutTask = scheduler.schedule(
                () -> onTimeout(ctx),
                timeout,
                TimeUnit.SECONDS
        );
    }

    private void cancelTimeout(UserContext ctx) {
        if (ctx.timeoutTask != null) {
            ctx.timeoutTask.cancel(false);
            ctx.timeoutTask = null;
        }
    }

    private void scheduleNext(UserContext ctx, boolean immediate) {
        if (immediate) {
            scheduler.execute(() -> startVerification(ctx));
        } else {
            scheduler.schedule(
                    () -> startVerification(ctx),
                    interval,
                    TimeUnit.SECONDS
            );
        }
    }
}
