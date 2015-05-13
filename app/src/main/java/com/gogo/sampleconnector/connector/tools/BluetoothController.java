package com.gogo.sampleconnector.connector.tools;


import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;

/**
 * Bluetooth data communication controller.
 */
public class BluetoothController extends Controller {
    static final String TAG = BluetoothController.class.getSimpleName();

    private BluetoothSocket mSocket;

    /**
     * Setup Input/Output Stream by BluetoothSocket.
     *
     * @param socket
     * @throws java.io.IOException socket is null or not connected
     */
    public void setupConnection(BluetoothSocket socket) throws NullPointerException, IOException {
        if (null == socket) throw new NullPointerException("Socket is null!");
        if (!socket.isConnected()) throw new IOException("Socket is not connected!");
        mSocket = socket;
    }

    public void closeConnection() throws NullPointerException, IOException {
        try {
            mSocket.close();
        } catch (NullPointerException e) {
            throw new NullPointerException("Bluetooth connection does not exist!");
        } catch (IOException e) {
            throw new IOException("" + e);
        }
        Log.d(TAG, "Close bluetooth connection.");
    }

    public boolean send(final byte[] data) {
        if (null == data) return true;
        if (null != mSocket && mSocket.isConnected()) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream out = mSocket.getOutputStream();
                        out.write(data);
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write: " + e);
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }
}