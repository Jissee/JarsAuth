package me.jissee.jarsauth.pending.base;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static me.jissee.jarsauth.data.TimeUtil.now;

/**
 * 严格串行、单轮验证的 PendingList 实现
 * 保留原有抽象接口，不引入轮次 ID
 */
public abstract class PendingList {

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

    protected static final class UserContext {
        final UUID userId;

        long lastVerificationTime;

        String randomData;
        String expectedResult;
        String clientResult;

        boolean inProgress;   // 当前是否存在未完成的验证
        boolean finished;     // 本轮是否已裁决

        ScheduledFuture<?> timeoutTask;

        UserContext(UUID userId) {
            this.userId = userId;
            this.lastVerificationTime = now();
        }
    }

    protected final MinecraftServer server;
    private final Map<UUID, UserContext> users = new ConcurrentHashMap<>();

    private long interval;
    private long timeout;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });

    protected PendingList(MinecraftServer server) {
        this.server = server;
        reload();
    }


    protected String generateRandom(){
        return UUID.randomUUID().toString();
    }

    protected abstract String calculateExpected(UUID userId, String random) throws Exception;

    protected abstract boolean compare(String expected, String actual);

    protected abstract void sendInfoToPlayer(UUID userId, String random);

    protected abstract void notifyFailure(UUID userId, String reason);

    protected abstract String formatReason(FailureType type, Exception e);

    protected abstract long getInterval();

    protected abstract long getTimeout();

    /* ---------------- 生命周期 ---------------- */

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

    /* ---------------- 客户端响应入口 ---------------- */

    public void onVerificationResponse(UUID userId, String clientResult) {
        UserContext ctx = users.get(userId);
        if (ctx == null) return;

        synchronized (ctx) {
            if (!ctx.inProgress || ctx.finished) {
                return; // 已超时 / 已完成 / 非当前轮
            }

            ctx.clientResult = clientResult;
            completeVerification(ctx);
        }
    }

    /* ---------------- 核心流程 ---------------- */

    private void startVerification(UserContext ctx) {
        synchronized (ctx) {
            if (ctx.inProgress) {
                return; // 理论上不应发生，防御性检查
            }

            ctx.inProgress = true;
            ctx.finished = false;
            ctx.clientResult = null;
            ctx.expectedResult = null;
            ctx.randomData = null;

            ctx.randomData = generateRandom();
            ctx.lastVerificationTime = now();

            scheduler.execute(() -> {
                synchronized (ctx) {
                    try {
                        sendInfoToPlayer(ctx.userId, ctx.randomData);
                        ctx.expectedResult = calculateExpected(ctx.userId, ctx.randomData);
                    } catch (Exception e) {
                        fail(ctx, FailureType.CALCULATION_ERROR, e);
                    }
                }
            });
            scheduleTimeout(ctx);
        }
    }

    private void completeVerification(UserContext ctx) {
        if (ctx.finished) return;

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
            if (!ctx.inProgress || ctx.finished) {
                return;
            }
            fail(ctx, FailureType.TIMEOUT, null);
        }
    }

    /* ---------------- 成功 / 失败处理 ---------------- */

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

    /* ---------------- 调度辅助 ---------------- */

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
