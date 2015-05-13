package com.gogo.sampleconnector.connector.tools;


import android.util.Log;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * WiFi data communication controller.
 */
public class WiFiController extends Controller {
    static final String TAG = WiFiController.class.getSimpleName();

    Socket mSocket;

    /**
     * Setup Input/Output Stream by socket.
     *
     * @param socket
     * @throws IOException socket is null or not connected
     */
    public void setupConnection(Socket socket) throws NullPointerException {
        if (null == socket) throw new NullPointerException("Socket is null!");
        mSocket = socket;
    }

    /**
     * Close connection.
     *
     * @throws IOException if close failed
     */
    public void closeConnection() throws NullPointerException, IOException {
        try {
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

}