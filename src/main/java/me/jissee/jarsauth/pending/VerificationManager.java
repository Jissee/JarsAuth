package me.jissee.jarsauth.pending;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

public class VerificationManager {

    private static class UserState {
        final int userId;
        final AtomicLong lastVerified = new AtomicLong(System.currentTimeMillis() / 1000);
        volatile boolean awaitingResponse = false;

        volatile String randomData = null;       // 本次生成的随机数据
        volatile String expectedAnswer = null;   // 本地计算结果
        volatile String pendingResponse = null;  // 客户端先返回时暂存

        private UserState(int userId) {
            this.userId = userId;
        }
    }

    /** 超时任务 */
    private static class ExpireTask implements Delayed {
        final int userId;
        final long expireAtSec; // 秒级时间戳

        ExpireTask(int userId, long expireAtSec) {
            this.userId = userId;
            this.expireAtSec = expireAtSec;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delaySec = expireAtSec - (System.currentTimeMillis() / 1000);
            return unit.convert(delaySec, TimeUnit.SECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.expireAtSec, ((ExpireTask) o).expireAtSec);
        }
    }

    private final Map<Integer, UserState> userMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService intervalChecker = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService computePool = Executors.newFixedThreadPool(2);
    private final DelayQueue<ExpireTask> delayQueue = new DelayQueue<>();
    private final ExecutorService timeoutWorker = Executors.newSingleThreadExecutor();

    private final long intervalSec;
    private final long timeoutSec;

    public VerificationManager(long intervalSec, long timeoutSec) {
        this.intervalSec = intervalSec;
        this.timeoutSec = timeoutSec;

        // 定期检查是否需要发起验证
        intervalChecker.scheduleAtFixedRate(this::checkInterval, 0, 1, TimeUnit.SECONDS);

        // 独立线程消费超时任务
        timeoutWorker.submit(this::processTimeouts);
    }

    /** 添加用户 */
    public void addUser(int userId) {
        userMap.putIfAbsent(userId, new UserState(userId));
    }

    /** 收到客户端返回 */
    public void onVerificationReceived(int userId, String response) {
        UserState state = userMap.get(userId);
        if (state != null && state.awaitingResponse) {
            if (state.expectedAnswer == null) {
                // 本地计算未完成，先存起来
                state.pendingResponse = response;
            } else {
                // 本地已算好，直接比对
                handleVerificationResult(state, response);
            }
        }
    }

    /** 定期触发验证（间隔检测） */
    private void checkInterval() {
        long nowSec = System.currentTimeMillis() / 1000;
        for (UserState state : userMap.values()) {
            if (!state.awaitingResponse && nowSec - state.lastVerified.get() >= intervalSec) {
                startVerification(state);
            }
        }
    }

    /** 启动一次验证流程 */
    private void startVerification(UserState state) {
        state.awaitingResponse = true;
        state.randomData = generateRandomData();
        state.expectedAnswer = null;
        state.pendingResponse = null;

        // 发送给客户端
        sendVerificationRequest(state.userId, state.randomData);

        // 启动后台计算
        computePool.submit(() -> {
            String result = computeExpectedAnswer(state.randomData);
            state.expectedAnswer = result;
            if (state.pendingResponse != null) {
                handleVerificationResult(state, state.pendingResponse);
                state.pendingResponse = null;
            }
        });

        // 放入延时队列
        delayQueue.offer(new ExpireTask(state.userId, (System.currentTimeMillis() / 1000) + timeoutSec));
    }

    /** 处理超时任务 */
    private void processTimeouts() {
        try {
            while (true) {
                ExpireTask task = delayQueue.take();
                UserState state = userMap.get(task.userId);
                if (state != null && state.awaitingResponse) {
                    // 仍在等待 → 判定失败
                    state.awaitingResponse = false;
                    state.randomData = null;
                    state.expectedAnswer = null;
                    state.pendingResponse = null;
                    notifyVerificationFailed(state.userId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 比对结果 */
    private void handleVerificationResult(UserState state, String response) {
        if (response.equals(state.expectedAnswer)) {
            state.lastVerified.set(System.currentTimeMillis() / 1000);
            state.awaitingResponse = false;
            state.randomData = null;
            state.expectedAnswer = null;
            state.pendingResponse = null;
        } else {
            state.awaitingResponse = false;
            notifyVerificationFailed(state.userId);
        }
    }

    // ===== 插桩方法 =====
    protected String generateRandomData() {
        return "R" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    protected String computeExpectedAnswer(String randomData) {
        try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException ignored) {}
        return randomData + "_ANS";
    }

    protected void sendVerificationRequest(int userId, String randomData) {
        System.out.println("[Stub] sendVerificationRequest user=" + userId + " random=" + randomData);
    }

    protected void notifyVerificationFailed(int userId) {
        System.out.println("[Stub] notifyVerificationFailed user=" + userId);
    }

    public void shutdown() {
        intervalChecker.shutdownNow();
        computePool.shutdownNow();
        timeoutWorker.shutdownNow();
    }
}
