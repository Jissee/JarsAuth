package me.jissee.jarsauth;

import java.util.concurrent.*;

public class ThreadExecutor {

    private static volatile ThreadExecutor instance;

    // CPU 核心数
    private final int cpuCores =
            Runtime.getRuntime().availableProcessors();

    // CPU 密集型线程池
    private final ExecutorService executor;

    // 定时调度线程池（只做延时触发）
    private final ScheduledExecutorService scheduler;

    private ThreadExecutor() {
        this.executor = new ThreadPoolExecutor(
                cpuCores,
                cpuCores,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(cpuCores * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public static ThreadExecutor getInstance() {
        if (instance == null) {
            synchronized (ThreadExecutor.class) {
                if (instance == null) {
                    instance = new ThreadExecutor();
                }
            }
        }
        return instance;
    }

    /**
     * 立即执行（CPU 密集型）
     */
    public void execute(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable is null");
        }
        executor.execute(runnable);
    }

    /**
     * 在指定秒数后执行任务
     *
     * @return ScheduledFuture，用于取消/查询调度状态
     */
    public ScheduledFuture<?> schedule(Runnable runnable, long delaySeconds) {
        if (runnable == null) {
            throw new NullPointerException("runnable is null");
        }
        if (delaySeconds < 0) {
            throw new IllegalArgumentException("delaySeconds < 0");
        }

        return scheduler.schedule(
                () -> executor.execute(runnable),
                delaySeconds,
                TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        scheduler.shutdown();
        executor.shutdown();
    }
}
