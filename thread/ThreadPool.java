package com.chinatsp.tvoiceservice.tencent;

import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.blackbase.BuildConfig;
import com.chinatsp.tvoiceservice.Postable;
import com.chinatsp.tvoiceservice.PostableHandler;
import com.incall.proxy.bt.LogUtil;

/**
 * Created on 14-12-21
 */
public class ThreadPool extends ThreadPoolExecutor implements Postable {

    public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * Calls execute() using a FutureTask so that exceptions on worker threads do not crash the app.
     * Instead exceptions are only logged in afterExecute()
     * If you really want the exceptions to crash the app then use execute() directly.
     */
    @Override
    public boolean post(final Runnable task) {
        execute(new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        }));
        return true;
    }

    /**
     * Uses thread pool if we are on a thread with a looper, such as the UI or FC thread, otherwise executes immediately
     * synchronously if we are NOT on a thread with a looper such as the UI thread.
     */
    @Override
    public boolean postImmediately(Runnable task) {
        if (Looper.myLooper() != null) {
            // On a looper thread like the UI thread, post.
            return post(task);
        } else {
            // On a worker thread, run immediately.
            task.run();
            return true;
        }
    }

    /**
     * Log a message when an executed task fails.
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            LogUtil.e("Error running "+r, t);
        }
        // t is always null when a FutureTask throws an exception, so use get() to see if it threw an exception.
        if (r instanceof Future) {
            try {
                ((Future) r).get();
            } catch (final Exception ex) {
                LogUtil.e("Error running thread pool task "+r, ex);
//                if (BuildConfig.DEBUG) {
                    PostableHandler.crash(ex);
//                }
            }
        }
    }

}