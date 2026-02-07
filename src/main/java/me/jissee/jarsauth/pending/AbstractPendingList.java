package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.JarsAuth;
import me.jissee.jarsauth.ThreadExecutor;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static me.jissee.jarsauth.data.TimeUtil.now;

public abstract class AbstractPendingList<T> {
    private static final Map<Class<? extends AbstractPendingList<?>>, AbstractPendingList<?>> pendingLists = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger("PendingList");
    public static void init(MinecraftServer server){
        if(server instanceof DedicatedServer){
            DataManager dataManager = DataManager.getServerInstance();
            ConfigService configService = dataManager.getService(ConfigService.class);
            long fce = configService.getValue(ConfigKey.FILE_CHECKSUM_ENABLED);
            long cae = configService.getValue(ConfigKey.CLIENT_AUTH_ENABLED);
            long sle = configService.getValue(ConfigKey.SERVER_LICENSE_ENABLED);
            if(fce != 0){
                pendingLists.put(FCPendingList.class, new FCPendingList(server));
            }
            if(cae != 0){
                pendingLists.put(CAPendingList.class, new CAPendingList(server));
            }
            if(sle != 0){
                pendingLists.put(SLPendingList.class, new SLPendingList(server));
            }
            File jar = JarsAuth.getJarFile();

            Path dest = Path.of("./" + jar.getName());

            if(jar.isFile() && jar.exists() && !Files.exists(dest)){
                try {
                    Files.copy(jar.toPath(), dest);
                } catch (IOException e) {
                    LOGGER.error("Cannot copy jar file", e);
                }
            }
        }
    }
    @SuppressWarnings("unchecked")
    public static <T extends AbstractPendingList<?>> T get(Class<T> clazz){
        return (T) pendingLists.get(clazz);
    }
    @SuppressWarnings("unchecked")
    public static <T extends AbstractPendingList<?>> void forEach(Consumer<T> consumer){
        pendingLists.values().forEach(list -> consumer.accept((T) list));
    }
    /* ================= 失败类型 ================= */

    public enum FailureType {
        CALCULATION_ERROR("internal"),
        COMPARE_ERROR("cmp.error"),
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

    protected final class UserContext {
        final UUID userId;
        long lastVerificationTime;
        PublicKey key;
        String randomData;

        T expectedResult;
        T clientResult;

        boolean inProgress;
        boolean finished;
        boolean canceled;

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

    protected final DataManager dataManager;

    /* ================= 构造 ================= */

    protected AbstractPendingList(MinecraftServer server, boolean clientRequired) {
        this.server = server;
        this.clientRequired = clientRequired;
        dataManager = DataManager.getServerInstance();
    }

    /* ================= 抽象接口 ================= */

    protected String generateRandom() {
        return UUID.randomUUID().toString();
    }

    protected abstract T calculateExpected(UUID userId, String random) throws Exception;

    /**
     * 裁决逻辑：
     * - clientRequired == true  : expected vs client
     * - clientRequired == false : 只看 expected（client 可能为 null）
     */
    protected abstract boolean compare(UserContext context, T expected, T actual);

    /**
     * 仅在 clientRequired == true 时调用
     */
    protected abstract void sendInfoToPlayer(UserContext ctx, String random);

    protected abstract void notifyFailure(UUID userId, String reason);

    protected abstract String formatReason(FailureType type, Exception e);

    protected abstract long getInterval();

    protected abstract long getTimeout();

    /* ================= 生命周期 ================= */

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
                ctx.canceled = true;
            }
        }
    }

    /* ================= 客户端 notify ================= */

    public void onVerificationResponse(UUID userId, T clientResult) {
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
            if (ctx.inProgress || ctx.canceled) return;

            ctx.inProgress = true;
            ctx.finished = false;
            ctx.expectedResult = null;
            ctx.clientResult = null;

            ctx.randomData = generateRandom();
            ctx.lastVerificationTime = now();

            // 仅在需要客户端参与时发送 challenge
            if (clientRequired) {
                sendInfoToPlayer(ctx, ctx.randomData);
            }

            scheduleTimeout(ctx);

            ThreadExecutor.getInstance().execute(() -> {
                try {
                    T expected = calculateExpected(ctx.userId, ctx.randomData);
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
            success = compare(ctx, ctx.expectedResult, ctx.clientResult);
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
            if (ctx.canceled) return;
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
        ctx.timeoutTask = ThreadExecutor.getInstance().schedule(
                () -> onTimeout(ctx),
                getTimeout()
        );
    }

    private void cancelTimeout(UserContext ctx) {
        if (ctx.timeoutTask != null) {
            ctx.timeoutTask.cancel(false);
            ctx.timeoutTask = null;
        }
    }

    private void scheduleNext(UserContext ctx, boolean immediate) {
        if (ctx.canceled) return;
        if (immediate) {
            ThreadExecutor.getInstance().execute(() -> startVerification(ctx));
        } else {
            ThreadExecutor.getInstance().schedule(
                    () -> startVerification(ctx),
                    getInterval()
            );
        }
    }

}
