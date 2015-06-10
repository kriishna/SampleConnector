package com.gogo.sampleconnector.connector.scantools;

import android.content.Context;
import android.os.Handler;

import android.util.Log;

import com.gogo.sampleconnector.R;

/**
 * Flash "scanning" notice when scanning.
 */
public class FlashingTextView extends FlashingItem {
    public static final String TAG = FlashingTextView.class.getSimpleName();

    private String[] messages;
    private int      interval;

    Handler flashingHandler;

    public FlashingTextView(Context c, int i, Handler h) {
        interval = i;
        flashingHandler = h;

        messages = new String[]{
                c.getResources().getString(R.string.str_scanning_0),
                c.getResources().getString(R.string.str_scanning_1),
                c.getResources().getString(R.string.str_scanning_2),
                c.getResources().getString(R.string.str_scanning_3),
        };
    }

    protected void startFlash() throws InterruptedException {
        int count = 0;
        while (isFlashing) {
            flashingHandler.obtainMessage(
                    FlashingItem.FLASHING_MESSAGE,
                    messages[count % messages.length]).sendToTarget();
            count++;
            Thread.sleep(interval);
        }
    }

    public void stopFlash() {
        isFlashing = false;
        flashingHandler.obtainMessage(FlashingItem.STOP_FLASHING_MESSAGE)
                .sendToTarget();
    }
}
