package com.gogo.sampleconnector.connector.tools;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;


/**
 * Usb BroadcastReceiver.
 * Use this class in main class for requesting usb permission.
 */
public class UsbBroadcastReceiver extends BroadcastReceiver {
    public final static String TAG = UsbBroadcastReceiver.class.getSimpleName();

    public static final String ACTION_USB_PERMISSION = "com.example.lichiachen.siitestconnector.siiusbconnector.USB_PERMISSION";

    private UsbConnector connector;

    public UsbBroadcastReceiver(UsbConnector connector) {
        this.connector = connector;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive() is called");
        if (null == connector) {
            Log.e(TAG, "Usb connector is not set!");
            return;
        }
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "USB permission granted.");
                    connector.retrieveConnectionResult(true);
                } else {
                    Log.e(TAG, "Failed to get permission!");
                    connector.retrieveConnectionResult(false);
                }
            }
        }
    }
}