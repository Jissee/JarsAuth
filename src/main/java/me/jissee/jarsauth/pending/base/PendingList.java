package me.jissee.jarsauth.pending.base;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象的验证任务执行器
 * 子类只需实现生成随机数据、计算预期值、比较结果
 */
public abstract class PendingList {
    public enum FailureType {
        CALCULATION_ERROR("internal"),
        COMPARE_ERROR("mismatch"),
        TIMEOUT("timeout"),
        EXPECTED_NOT_COMPUTED("internal"),
        RESULT_MISMATCH("mismatch");

        private final String key;

        FailureType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    // 用户信息
    static class UserContext {
        final UUID userId;
        long lastVerificationTime; // 上一次验证时间（秒）
        String randomData;
        String expectedResult;
        String clientResult;
        final AtomicBoolean verified = new AtomicBoolean(false);

        // deadline 定时任务，用于超时检查
        ScheduledFuture<?> deadlineTask;

        UserContext(UUID userId) {
            this.userId = userId;
            this.lastVerificationTime = System.currentTimeMillis() / 1000;
        }
    }

    private final Map<UUID, UserContext> users = new ConcurrentHashMap<>();

    protected final MinecraftServer server;
    private long interval; // 验证间隔 (秒)
    private long timeout;  // 超时阈值 (秒)

    private final ScheduledExecutorService scheduler;


    public PendingList(MinecraftServer server) {
        this.server = server;
        reload();
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    private String generateRandom(){
        return UUID.randomUUID().toString();
    }

    protected abstract String calculateExpected(String random) throws Exception;
    protected abstract boolean compare(String expected, String actual);
    protected abstract void sendRandom(UUID userId, String random);
    protected abstract void notifyFailure(UUID userId, String reason);
    protected abstract String formatReason(FailureType type, Exception e);

    protected abstract long getInterval();
    protected abstract long getTimeout();

    public void reload(){
        this.interval = getInterval();
        this.timeout = getTimeout();
    }

    /** 添加用户并立即进行首次验证 */
    public void addUser(UUID userId) {
        UserContext ctx = new UserContext(userId);
        users.put(userId, ctx);
        startVerification(ctx, true); // 首次立即执行
    }

    /** 移除用户 */
    public void removeUser(UUID userId) {
        UserContext ctx = users.remove(userId);
        if (ctx != null && ctx.deadlineTask != null) {
            ctx.deadlineTask.cancel(false);
        }
    }

    /** 网络线程：收到客户端返回 */
    public void onVerificationResponse(UUID userId, String clientResult) {
        UserContext ctx = users.get(userId);
        if (ctx == null) return;
        ctx.clientResult = clientResult;

        // 收到响应后立即校验
        if (ctx.expectedResult != null) {
            boolean success = false;
            try {
                success = compare(ctx.expectedResult, ctx.clientResult);
            } catch (Exception e) {
                notifyFailureWithType(userId, FailureType.COMPARE_ERROR, e);
            }

            if (!success) {
                notifyFailureWithType(userId, FailureType.RESULT_MISMATCH, null);
            }

            // 成功或失败都取消 deadline 任务
            if (ctx.deadlineTask != null) {
                ctx.deadlineTask.cancel(false);
                ctx.deadlineTask = null;
            }

            // 准备下一次验证
            scheduleVerification(ctx);
        }
    }

    private void notifyFailureWithType(UUID userId, FailureType type, Exception e) {
        String reason = formatReason(type, e);
        notifyFailure(userId, reason);
        removeUser(userId); // 出错后移除用户
    }


    /** 统一的验证调度入口 */
    private void startVerification(UserContext ctx, boolean immediate) {
        Runnable task = () -> {
            // 阶段1：生成随机数据并发送
            ctx.randomData = generateRandom();
            ctx.lastVerificationTime = System.currentTimeMillis() / 1000;
            sendRandom(ctx.userId, ctx.randomData);

            try {
                ctx.expectedResult = calculateExpected(ctx.randomData);
            } catch (Exception e) {
                notifyFailureWithType(ctx.userId, FailureType.CALCULATION_ERROR, e);
                return;
            }

            // 阶段2：安排超时检查
            ctx.deadlineTask = scheduler.schedule(() -> {
                if (ctx.clientResult == null) {
                    notifyFailureWithType(ctx.userId, FailureType.TIMEOUT, null);
                } else if (ctx.expectedResult == null) {
                    notifyFailureWithType(ctx.userId, FailureType.EXPECTED_NOT_COMPUTED, null);
                }
            }, timeout, TimeUnit.SECONDS);
        };

        if (immediate) {
            // 立即执行
            scheduler.execute(task);
        } else {
            // 延迟 interval 秒后执行
            scheduler.schedule(task, interval, TimeUnit.SECONDS);
        }
    }

    /** 在 onVerificationResponse 成功/失败后安排下一次 */
    private void scheduleVerification(UserContext ctx) {
        startVerification(ctx, false);
    }
}
