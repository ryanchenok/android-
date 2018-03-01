package com.chinatsp.tvoiceservice;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

/**
 * Handler that also has postImmediately.
 *
 * Created on 14-11-26.
 */
public class PostableHandler extends Handler implements Postable {

    /**
     * UI Thread, use as you would Activity.runOnUIThread.
     */
    public static final PostableHandler UI_THREAD = new PostableHandler(Looper.getMainLooper());

    public PostableHandler(Looper looper) {
        super(looper);
    }

    /**
     * If the current thread is the same as the handler than the task is executed immediately; otherwise it is posted
     * as normal.
     */
    public boolean postImmediately(Runnable task) {
        if (Looper.myLooper() == getLooper()) {
            task.run();
            return true;
        } else {
            return post(task);
        }
    }

    public static void crash(@Nullable Throwable cause) {
        final RuntimeException ex = new RuntimeException("Force crashing", cause);
        UI_THREAD.postImmediately(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException(ex);
            }
        });
    }

}