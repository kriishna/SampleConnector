package com.gogo.sampleconnector.connector.scantools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

/**
 * Runnable that perform a bluetooth scan and update address list.
 */
public class BluetoothScanningRunnable extends ScanningRunnable {
    public static final String TAG = BluetoothScanningRunnable.class.getSimpleName();

    Context context;

    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothScanningRunnable(Handler h, BluetoothAdapter a, Context c, FlashingItem f) {
        super(h, f);
        context = c;
        mBluetoothAdapter = a;
    }

    @Override
    public void run() {

        /* If we are already discovering, stop it. */
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        context.registerReceiver(btReceiver,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));
        context.registerReceiver(btReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        /* Start discovery */
        mBluetoothAdapter.startDiscovery();

        // Notice user the scanning is running
        Thread t = new Thread(flashingItem, "ScanningBluetooth");
        t.start();

    }

    @Override
    public void stopScanning() {
        super.stopScanning();
        context.unregisterReceiver(btReceiver);
    }

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Update address list
                mainThreadHandler.obtainMessage(
                        ScanningRunnable.FOUND_ADDRESS_UPDATE_MESSAGE,
                        device.getName() + " - " + device.getAddress())
                        .sendToTarget();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopScanning();
            }
        }
    };
}
