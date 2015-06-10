package com.gogo.sampleconnector.connector.scantools;

import android.os.Handler;
/**
 * A HandlerThread to scan available machine.
 */
public abstract class ScanningRunnable implements Runnable {

    protected Handler mainThreadHandler;

    protected boolean isScanning;

    public static final int FOUND_ADDRESS_UPDATE_MESSAGE = 0x01;

    protected FlashingItem flashingItem;

    public ScanningRunnable(Handler h, FlashingItem f) {
        mainThreadHandler = h;
        flashingItem = f;
        isScanning = true;
    }

    public void stopScanning() {
        isScanning = false;
        flashingItem.stopFlash();
    }

}
