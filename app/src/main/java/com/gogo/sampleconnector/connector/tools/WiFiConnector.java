package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gogo.sampleconnector.R;
import com.gogo.sampleconnector.connector.ConnectFailException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sii Wifi Connector
 */
public class WiFiConnector extends BaseConnector {
    final static String TAG = WiFiConnector.class.getSimpleName();

    final int ADDRESS_UPDATE_MESSAGE = 0x01;

    final int PORT = 9100;

    WiFiController wifiController;

    ArrayList<String> addrs = new ArrayList<>();
    ListView addrListView;
    ArrayAdapter<String> addrAdapter;

    // Handler that handle the updating of address list.
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case ADDRESS_UPDATE_MESSAGE:
                    addrAdapter.add((String) message.obj);
                    break;
            }
        }
    };

    public static WiFiConnector newInstance() {
        WiFiConnector frag = new WiFiConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = "Connect by wifi ...";
        ListView lv = prepareListView(addrs);

        WiFiBroadcastRunnable r = new WiFiBroadcastRunnable(handler);
        Executors.newSingleThreadExecutor().submit(r);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(lv)
                .create();
    }

    @Override
    public String getConnectorType() {
        return "WiFi";
    }

    @Override
    protected ListView prepareListView(final ArrayList<String> list) {
        addrListView = super.prepareListView(list);
        addrAdapter = new ArrayAdapter<String>(getActivity(), R.layout.address, addrs);
        addrListView.setAdapter(addrAdapter);

        addrListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Index " + position + " is clicked: " + list.get(position));
                connect(list.get(position), PORT);
                performSelect();
            }
        });
        return addrListView;
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
    public BaseController getController() throws IOException {
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
                    sendConnectResultMessage(true, null);
                    controller.setupConnection(wifiSocket);
                } else {
                    Log.e(TAG, "Socket is not connected.");
                    sendConnectResultMessage(false, new ConnectFailException(ConnectionStatus.FAIL_ESTABLISH));
                }
            } catch (UnknownHostException e) {
                sendConnectResultMessage(false, new ConnectFailException(ConnectionStatus.ADDRESS_UNKNOWN));
            } catch (NullPointerException|IOException e) {
                sendConnectResultMessage(false, new ConnectFailException(ConnectionStatus.FAIL_ESTABLISH));
                Log.e(TAG, "Failed to connect by WiFi: " + e);
            }
        }

        private void sendConnectResultMessage(final boolean result, ConnectFailException exception) {
            Message message = mHandler.obtainMessage();
            message.what = BaseConnector.CONNECT_STATUS;

            if (result) {
                message.arg1 = ConnectionStatus.SUCCEED;
            } else {
                message.arg1 = ConnectionStatus.FAIL;
                message.obj = exception.getMessage();
            }
            message.sendToTarget();
        }
    }

    /**
     * Runnable that perform a wifi broadcast and update address list.
     */
    private class WiFiBroadcastRunnable implements Runnable {
        Handler uiHandler;

        public WiFiBroadcastRunnable(Handler h) {
            uiHandler = h;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Log.e(TAG, "run()");
                    //performBroadcast(getBroadcastAddress());
                    if (getBroadcastAddress() == null) {
                        Log.e(TAG, "dhcp is null !!!");
                    } else {
                        Log.e(TAG, "dhcp is not null!!");
                    }
                    //uiHandler.obtainMessage(ADDRESS_UPDATE_MESSAGE, addr).sendToTarget();
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Got :" + e);
                    break;
                }
            }
        }

        private InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();
            Log.e(TAG, "dhcp info got.");
            if (null == dhcp) throw new IOException("DHCP not found!");

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k=0; k<4; k++) {
                quads[k] = (byte) ((broadcast >> k*8) & 0xff);
            }

            return InetAddress.getByAddress(quads);
        }

        private void performBroadcast(InetAddress addr) throws SocketException {
            DatagramSocket udpSocket = new DatagramSocket(PORT);

        }
    }

}