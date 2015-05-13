package com.gogo.sampleconnector.connector.tools;


import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * USB data communication controller.
 */
public class UsbController extends Controller {
    public static final String TAG =  UsbController.class.getSimpleName();

    private final int TIMEOUT = 300;

    UsbBroadcastReceiver usbBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbDeviceConnection usbDeviceConnection;
    UsbInterface usbInterface;
    UsbEndpoint endpointIn;
    UsbEndpoint endpointOut;

    public void setDeviceInfo(UsbBroadcastReceiver receiver, UsbDevice device, UsbManager manager) throws NullPointerException {
        if (null == receiver) throw new NullPointerException("receiver is null!");
        usbBroadcastReceiver = receiver;
        usbDevice = device;
        usbManager = manager;
        usbInterface = usbDevice.getInterface(0);
        for (int i=0; i<usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_IN && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                endpointIn = ep;
            } else if (ep.getDirection() == UsbConstants.USB_DIR_OUT && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                endpointOut = ep;
            }
        }
    }

    @Override
    public boolean send(final byte[] data) {
        if (null != usbBroadcastReceiver && createConnection()) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    usbDeviceConnection.bulkTransfer(endpointOut, data, data.length, TIMEOUT);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private boolean createConnection() {
        final boolean result;
        if (usbBroadcastReceiver.hasGranted() && null == usbDeviceConnection) {
            usbDeviceConnection = usbManager.openDevice(usbDevice);
            usbDeviceConnection.claimInterface(usbInterface, true);
            Log.d(TAG, "Usb connection is established.");
            result = true;
        } else if (null != usbDeviceConnection) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public void closeConnection() throws NullPointerException, IOException {
        if (!usbBroadcastReceiver.hasGranted()) return;
        if (null == usbDeviceConnection) return;
        usbDeviceConnection.close();
    }
}