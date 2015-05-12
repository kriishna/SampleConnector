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

    /**
     * Application information for sending USB granting broadcast in UsbConnector.
     */
    private Context context;
    private boolean isGranted = false;

    public UsbBroadcastReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive() is called");
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "USB permission granted.");
                    isGranted = true;
                    //usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                } else {
                    Log.e(TAG, "Failed to get permission!");
                }
            }
        }
    }

    public Context getApplicationContextInfo() throws NullPointerException {
        if (null == context) throw new NullPointerException("Context is null!");
        return context;
    }

    public boolean hasGranted() {
        return isGranted;
    }

}