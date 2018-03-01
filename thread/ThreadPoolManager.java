package com.chinatsp.tvoiceservice.tencent;

import android.os.HandlerThread;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    private ExecutorService service;

    private ThreadPoolManager() {
        service = Executors.newFixedThreadPool(1);
    }

    private static final ThreadPoolManager manager = new ThreadPoolManager();
    private static ThreadPool mWorkPool;
    private static ScheduledExecutorService mScheduledPool;
    private static Map<String, PostableHandler> serialThreads = new HashMap<>();

    public static ThreadPoolManager getInstance() {
        return manager;
    }

    /**
     * @return Thread Pool that can be used for any worker tasks.
     * @see Executors#newCachedThreadPool()
     */
    public static synchronized ThreadPool getWorkPool() {
        if (mWorkPool == null) {
            mWorkPool = new ThreadPool(2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
                ThreadFactory factory = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    LogUtil.d("Creating new thread for %s", mWorkPool);
                    return factory.newThread(r);
                }
            });
        }
        return mWorkPool;
    }

    /** Hook for unit testing */
    public static void setWorkPool(ThreadPool pool) {
        mWorkPool = pool;
    }

    /**
     * @param name Name used to identify thread.
     * @return Handler that can be used to post tasks to the thread that are run serially one at a time.
     */
    public static synchronized PostableHandler getSerialThread(@NonNull String name) {
        PostableHandler handler = serialThreads.get(name);
        if (handler == null) {
            HandlerThread thread = new HandlerThread(name);
            thread.start();
            handler = new PostableHandler(thread.getLooper());
            serialThreads.put(name, handler);
        }
        return handler;
    }

    /**
     * Call if a serial thread has hung and a new serial thread should be created.
     *
     * @param name Name used to identify thread.
     */
    public static synchronized void clearSerialThread(String name) {
        PostableHandler handler = serialThreads.get(name);
        if (handler != null) {
            handler.getLooper().getThread().interrupt();
            serialThreads.remove(name);
        }
    }

    /**
     * @return Thread Pool that can be used for any scheduled tasks.
     */
    public static synchronized ScheduledExecutorService getScheduledPool() {
        if (mScheduledPool == null) {
            mScheduledPool = new ScheduledThreadPoolExecutor(1) {
                /**
                 * Use a work pool to run the actual command because if one of the scheduled tasks takes a long time it
                 * will delay the other tasks.
                 */
                @Override
                public ScheduledFuture<?> schedule(final Runnable command, long delay, TimeUnit unit) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            getWorkPool().execute(command);
                        }
                    };
                    return super.schedule(runnable, delay, unit);
                }
            };
        }
        return mScheduledPool;
    }

    public void addTask(Runnable runnable) {
        service.execute(runnable);
    }

}