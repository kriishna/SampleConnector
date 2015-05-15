package com.gogo.sampleconnector.connector.tools;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * USB Bluetooth Connector
 */
public class UsbConnector extends BaseConnector {
    public static final String TAG = UsbConnector.class.getSimpleName();

    UsbBroadcastReceiver usbBroadcastReceiver;
    UsbController usbController;
    UsbManager usbManager;
    UsbDevice usbDevice;

    public static UsbConnector newInstance() {
        UsbConnector frag = new UsbConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = "Connect by USB ?";
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Set up relate information after permission is granted.
                        usbController = new UsbController();
                        performSelect();
                    }
                })
                .create();

    }

    @Override
    public String getConnectorType() {
        return "USB";
    }

    /**
     * Create permission request intent.
     *
     * @return true if listener is set, false if listener
     * or broadcastreceiver is not set
     */
    protected boolean performSelect() {
        // Register broadcast receiver in caller.
        boolean tmp = super.performSelect();

        // If UsbBroadcastReceiver is not set, return.
        if (null == usbBroadcastReceiver) {
            Log.e(TAG, "UsbBroadcastReceiver is not set!");
            return false;
        }
        Context context = usbBroadcastReceiver.getApplicationContextInfo();
        // Create PendingIntent for permission request
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION), 0);
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager.getDeviceList().values().iterator().hasNext()) {
            usbDevice = usbManager.getDeviceList().values().iterator().next();
            usbManager.requestPermission(usbDevice, permissionIntent);
            usbController.setDeviceInfo(usbBroadcastReceiver, usbDevice, usbManager);
        } else {
            tmp = false;
        }
        final boolean result = tmp;
        Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                if (result) {
                    mHandler.obtainMessage(BaseConnector.CONNECT_STATUS, ConnectionStatus.SUCCEED).sendToTarget();
                } else {
                    mHandler.obtainMessage(BaseConnector.CONNECT_STATUS, ConnectionStatus.NO_DEVICE).sendToTarget();
                }
            }
        }, 500, TimeUnit.MILLISECONDS);
        UsbConnector.this.dismiss();
        return result;
    }

    /**
     * Get application context info for creating USB permission broadcast,
     * retrieving USB device.
     *
     * @param receiver UsbBroadcastReceiver that receives USB permission broadcast.
     */
    public void setUsbBroadcastReceiver(UsbBroadcastReceiver receiver) {
        this.usbBroadcastReceiver = receiver;
    }

    @Override
    public BaseController getController() throws IOException {
        if (null == usbController) throw new IOException("UsbController is null!");
        return usbController;
    }
}