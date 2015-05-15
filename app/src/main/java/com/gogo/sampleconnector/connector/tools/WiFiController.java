package com.gogo.sampleconnector.connector.tools;


import android.os.Handler;
import android.util.Log;

import com.gogo.sampleconnector.connector.ReceivingDataThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * WiFi data communication controller.
 */
public class WiFiController extends BaseController {
    static final String TAG = WiFiController.class.getSimpleName();

    Socket mSocket;

    /**
     * Setup Input/Output Stream by socket.
     *
     * @param socket
     * @throws IOException socket is null or not connected
     */
    public void setupConnection(Socket socket) throws NullPointerException, IOException {
        if (null == socket) throw new NullPointerException("Socket is null!");
        if (!socket.isConnected()) throw new IOException("Socket is not connected!");
        mSocket = socket;
    }

    /**
     * Close connection.
     *
     * @throws IOException if close failed
     */
    public void closeConnection() throws NullPointerException, IOException {
        try {
            if (mReceivingDataThread.isAlive()) mReceivingDataThread.quit();
            mSocket.close();
        } catch (NullPointerException e) {
            throw new NullPointerException("Connection does not exist!");
        } catch (IOException e) {
            throw new IOException("" + e);
        }
        Log.d(TAG, "Close wifi connection.");
    }

    public boolean send(final byte[] data) {
        if (null == data) return true;
        if (null != mSocket && mSocket.isConnected()) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream out = mSocket.getOutputStream();
                        out.write(data);
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to send data: " + e);
                    }
                }
            });
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
    private class WiFiReceivingDataThread extends ReceivingDataThread {

        boolean isStop = false;

        Socket connectionSocket;

        public WiFiReceivingDataThread(Handler h) {
            super("Receive data through wifi", h);
        }

        public void setSocket(Socket socket) {
            this.connectionSocket = socket;
        }

        @Override
        protected boolean close() {
            isStop = true;
            return isStop;
        }

        @Override
        public void run() {
            mReceiveRunnable = new Runnable() {
                @Override
                public void run() {
                    for (; connectionSocket.isConnected(); ) {
                        if (!isStop) {
                            try {
                                byte[] buffer = new byte[128];
                                InputStream in = connectionSocket.getInputStream();
                                in.read(buffer, 0, buffer.length);
                                mainThreadHandler
                                        .obtainMessage(BaseController.RECEIVED_MESSAGE, new String(buffer))
                                        .sendToTarget();
                                in.close();
                            } catch (IOException e) {}
                        } else {
                            return;
                        }
                    }
                }
            };
            super.run();
        }
    }
    WiFiReceivingDataThread mReceivingDataThread = new WiFiReceivingDataThread(mainThreadHandler);
}