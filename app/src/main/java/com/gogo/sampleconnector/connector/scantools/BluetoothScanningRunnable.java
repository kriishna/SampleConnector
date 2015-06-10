package com.gogo.sampleconnector.connector.scantools;

import android.content.Context;
import android.os.Handler;

/**
 * Runnable that perform a bluetooth scan and update address list.
 */
public class BluetoothScanningRunnable extends ScanningRunnable {

    Context context;

    public BluetoothScanningRunnable(Handler h, Context c, FlashingItem f) {
        super(h, f);
        context = c;
    }

    @Override
    public void run() {

    }
}
