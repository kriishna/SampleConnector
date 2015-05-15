package com.gogo.sampleconnector.connector;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * A HandlerThread to recursively receive data sent by printer.
 * Call quit() to stop loop.
 */
public abstract class ReceivingDataThread extends HandlerThread {
    /**
     * Main thread handler for sending back received data.
     */
    protected Handler mainThreadHandler;

    /**
     * A Runnable for recursively receiving data.
     */
    protected Runnable mReceiveRunnable;

    public ReceivingDataThread(String name, Handler h) {
        super(name);
        mainThreadHandler = h;
    }

    @Override
    public boolean quit() {
        if (close()) return super.quit();
        else return false;
    }

    public Runnable getReceiveRunnable() {
        return mReceiveRunnable;
    }

    protected abstract boolean close();
}