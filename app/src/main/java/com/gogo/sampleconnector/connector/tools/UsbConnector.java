package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.gogo.sampleconnector.connector.ConnectFailException;

import java.io.IOException;

/**
 * USB Bluetooth Connector
 */
public class UsbConnector extends BaseConnector {

    UsbController usbController;
    UsbManager usbManager;
    UsbDevice usbDevice;

    public static UsbConnector newInstance() {
        UsbConnector frag = new UsbConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        UsbBroadcastReceiver usbBroadcastReceiver = new UsbBroadcastReceiver(this);
        getActivity().registerReceiver(usbBroadcastReceiver,
                new IntentFilter(UsbBroadcastReceiver.ACTION_USB_PERMISSION));
        usbController = new UsbController();
        performSelect();
        return new AlertDialog.Builder(getActivity()).create();
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
        boolean result = super.performSelect();

        final Context context = getActivity();
        // Create PendingIntent for permission request
        Intent intent = new Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager.getDeviceList().values().iterator().hasNext()) {
            // Device attached, request permission.
            usbDevice = usbManager.getDeviceList().values().iterator().next();
            usbManager.requestPermission(usbDevice, permissionIntent);
        } else {
            // No device found.
            result = false;
            Message message = mHandler.obtainMessage();
            message.what = BaseConnector.CONNECT_STATUS;
            message.arg1 = ConnectionStatus.FAIL;
            message.obj = ConnectionStatus.NO_DEVICE;
            message.sendToTarget();
        }

        UsbConnector.this.dismiss();
        return result;
    }

    /**
     * When permission is granted(or not), call this method
     * to send back result.
     *
     * @param hasPermission
     */
    public void retrieveConnectionResult(final boolean hasPermission) {
        Message message = mHandler.obtainMessage();
        message.what = BaseConnector.CONNECT_STATUS;
        try {
            if (hasPermission) {
                message.arg1 = ConnectionStatus.SUCCEED;
                usbController.setDeviceInfo(usbDevice, usbManager);
            } else {
                throw new ConnectFailException(ConnectionStatus.NO_PERMISSION);
            }
        } catch (ConnectFailException e) {
            message.arg1 = ConnectionStatus.FAIL;
            message.obj = e.getMessage();
        }
        message.sendToTarget();
    }

    @Override
    public BaseController getController() throws IOException {
        if (null == usbController) throw new IOException("UsbController is null!");
        return usbController;
    }
}