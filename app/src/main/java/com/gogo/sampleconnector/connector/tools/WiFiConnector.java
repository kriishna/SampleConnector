package com.gogo.sampleconnector.connector.tools;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sii Wifi Connector
 */
public class WiFiConnector extends BaseConnector {
    final static String TAG = WiFiConnector.class.getSimpleName();

    final int PRINTER_PORT = 9100;

    WiFiController wifiController;

    public static WiFiConnector newInstance() {
        WiFiConnector frag = new WiFiConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create address list
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("192.168.1.23");
        tmp.add("192.168.1.24");

        final ArrayList<String> addrs = tmp;

        String title = "Connect by wifi ...";
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(prepareListView(addrs))
                .create();
    }

    @Override
    public String getConnectorType() {
        return "WiFi";
    }

    @Override
    protected ListView prepareListView(final ArrayList<String> list) {
        ListView listview = super.prepareListView(list);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Index " + position + " is clicked: " + list.get(position));
                connect(list.get(position), PRINTER_PORT);
                performSelect();
            }
        });
        return listview;
    }

    /**
     * Create connection in another thread. Setup Controller.
     *
     * @param addr
     * @param port
     */
    private void connect(String addr, int port) {
        wifiController = new WiFiController();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new ConnectRunnable(wifiController, addr, port));
    }

    @Override
    protected boolean performSelect() {
        final boolean result = super.performSelect();
        WiFiConnector.this.dismiss();
        return result;
    }

    /**
     * Get wifi connection controller.
     *
     * @return WiFiController
     * @throws IOException if WiFiController is null
     */
    @Override
    public Controller getController() throws IOException {
        if (null == wifiController) throw new IOException("WiFiController is null!");
        return wifiController;
    }


    /**
     * Runnable that construct connection and setup controller.
     */
    private class ConnectRunnable implements Runnable {
        WiFiController controller;
        String address;
        int port;

        public ConnectRunnable(WiFiController controller, String address, int port) {
            this.controller = controller;
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            Socket wifiSocket;
            try {
                wifiSocket = new Socket();
                wifiSocket.connect(new InetSocketAddress(this.address, this.port), 1000);
                if (wifiSocket.isConnected()) {
                    Log.d(TAG, "Connection established successfully.");
                    sendConnectResultMessage(true);
                    controller.setupConnection(wifiSocket);
                } else {
                    Log.e(TAG, "Socket is not connected.");
                    sendConnectResultMessage(false);
                    throw new IOException("Socket is not connected!");
                }
            } catch (UnknownHostException e) {
                sendConnectResultMessage(false);
                Log.e(TAG, "Address is unknown: " + e);
            } catch (NullPointerException|IOException e) {
                sendConnectResultMessage(false);
                Log.e(TAG, "Failed to create socket: " + e);
            }
        }

        private void sendConnectResultMessage(final boolean result) {
            if (result) {
                mHandler.obtainMessage(BaseConnector.CONNECT_STATUS,
                        ConnectionStatus.SUCCEED).sendToTarget();
            } else {
                mHandler.obtainMessage(BaseConnector.CONNECT_STATUS,
                        ConnectionStatus.FAIL).sendToTarget();
            }
        }
    }

}