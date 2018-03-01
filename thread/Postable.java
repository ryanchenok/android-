package com.chinatsp.tvoiceservice;

/**
 * Created on 14-11-26.
 */
public interface Postable {

    boolean post(Runnable task);

    /** Will run immediately if already being executed on the same thread as the handler. */
    boolean postImmediately(Runnable task);
}