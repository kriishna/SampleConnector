package com.gogo.sampleconnector.connector.scantools;

import android.os.Handler;

import android.util.Log;

/**
 * Flash "scanning" notice when scanning.
 */
public class FlashingTextView extends FlashingItem {
    public static final String TAG = FlashingTextView.class.getSimpleName();

    private String[] messages;
    private int      interval;

    Handler mainThreadHandler;

    public FlashingTextView(String[] m, int i, Handler h) {
        messages = m;
        interval = i;
        mainThreadHandler = h;
    }

    protected void startFlash() throws InterruptedException {
        int count = 0;
        while (isFlashing) {
            mainThreadHandler.obtainMessage(
                    FlashingItem.FLASHING_MESSAGE,
                    messages[count % messages.length]).sendToTarget();
            count++;
            Thread.sleep(interval);
        }
    }

    public void stopFlash() {
        isFlashing = false;
        mainThreadHandler.obtainMessage(FlashingItem.STOP_FLASHING_MESSAGE)
                .sendToTarget();
    }
}
