package com.gogo.sampleconnector.connector.tools;


import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.gogo.sampleconnector.connector.Controller;
import com.gogo.sampleconnector.connector.ReceivingDataThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Bluetooth data communication controller.
 */
public class BluetoothController extends BaseController {
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
            if (mReceivingDataThread.isAlive()) mReceivingDataThread.quit();
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream out = mSocket.getOutputStream();
                        out.write(data);
                        // Do not close the stream. This caused the data
                        // sent incomplete.
                        //out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write: " + e);
                    }
                }
            }, "Send by bluetooth").start();
            return true;
        } else {
            return false;
        }
    }

    public void beginReceiving() {
        mReceivingDataThread.setSocket(mSocket);
        mReceivingDataThread.start();
        new Handler(mReceivingDataThread.getLooper())
                .post(mReceivingDataThread.getReceiveRunnable());
    }

    /**
     * HandlerThread to receiving data in a loop through WiFi.
     */
    private class BluetoothReceivingDataThread extends ReceivingDataThread {

        boolean isStop = false;

        BluetoothSocket connectionSocket;

        public BluetoothReceivingDataThread(Handler h) {
            super("Receive data through bluetooth", h);
        }

        public void setSocket(BluetoothSocket socket) {
            this.connectionSocket = socket;
        }

        @Override
        protected boolean close() {
            isStop = true;
            return isStop;
        }

        public Runnable getReceiveRunnable() {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream in = connectionSocket.getInputStream();
                        for (; connectionSocket.isConnected(); ) {
                            if (!isStop) {
                                byte[] buffer = new byte[128];
                                in.read(buffer, 0, buffer.length);
                                mainThreadHandler
                                        .obtainMessage(BaseController.RECEIVED_MESSAGE, buffer)
                                        .sendToTarget();
                            } else {
                                break;
                            }
                        }
                        in.close();
                    } catch (IOException e) {}
                }
            };
        }
    }
    BluetoothReceivingDataThread mReceivingDataThread = new BluetoothReceivingDataThread(mainThreadHandler);
}