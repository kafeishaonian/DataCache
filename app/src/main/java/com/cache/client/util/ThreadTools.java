package com.cache.client.util;
/**
 * 线程
 * Created by Hongmingwei on 2017/11/20.
 * Email: 648600445@qq.com
 */
public final class ThreadTools {
    /**
     * TAG
     */
    private static final String TAG = ThreadTools.class.getSimpleName();

    public static Thread startMinPriorityThread(Runnable action) {
        Thread thread = new Thread(action);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return thread;
    }

    public static Thread startNormalPriorityThread(Runnable action) {
        Thread thread = new Thread(action);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
        return thread;
    }

    public static Thread startMaxPriorityThread(Runnable action) {
        Thread thread = new Thread(action);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        return thread;
    }
}