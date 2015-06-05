package com.gogo.sampleconnector.connector.scantools;

import android.util.Log;

/**
 * Flash view in another thread.
 */
public abstract class FlashingItem implements Runnable {
    public static final String TAG = FlashingItem.class.getSimpleName();

    public static final int FLASHING_MESSAGE = 0x0a0a;
    public static final int STOP_FLASHING_MESSAGE = 0x0a0b;

    boolean isFlashing;

    protected abstract void startFlash() throws InterruptedException;
    public abstract void stopFlash();

    @Override
    public void run() {
        isFlashing = true;
        try {
            startFlash();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted exception is caught, stop flash: " + e);
            isFlashing = false;
        }
    }

}
