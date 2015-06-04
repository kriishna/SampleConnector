package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.gogo.sampleconnector.R;
import com.gogo.sampleconnector.connector.ConnectFailException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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

    private boolean stopBroadcast = false;

    private WiFiController wifiController;

    private ArrayList<String> addrs = new ArrayList<>();
    private ListView addrListView;
    private ArrayAdapter<String> addrAdapter;

    private Button picker;

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
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.wifi_picker, null);

        picker = (Button) layout.findViewById(R.id.btn_address_set);

        LinearLayout containerLayout = (LinearLayout) layout.findViewById(R.id.ll_address_list_container);
        containerLayout.addView(lv);

        setupAddressEditor((ViewGroup) layout.findViewById(R.id.ll_editor_container));

        WiFiBroadcastRunnable r = new WiFiBroadcastRunnable(handler);
        Executors.newSingleThreadExecutor().submit(r);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(layout)
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
                String[] params = list.get(position).split(":");
                connect(params[0], Integer.parseInt(params[1]));
                performSelect();
            }
        });
        return addrListView;
    }

    /**
     * Set up address editor for user input.
     *
     * @param parent
     */
    private void setupAddressEditor(final ViewGroup parent) {
        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    EditText et_ip_1st = (EditText) parent.findViewById(R.id.et_ip_1th);
                    String ip_1st = et_ip_1st.getText().toString();
                    EditText et_ip_2nd = (EditText) parent.findViewById(R.id.et_ip_2nd);
                    String ip_2nd = et_ip_2nd.getText().toString();
                    EditText et_ip_3rd = (EditText) parent.findViewById(R.id.et_ip_3rd);
                    String ip_3rd = et_ip_3rd.getText().toString();
                    EditText et_ip_4th = (EditText) parent.findViewById(R.id.et_ip_4th);
                    String ip_4th = et_ip_4th.getText().toString();
                    EditText et_ip_port = (EditText) parent.findViewById(R.id.et_ip_port);
                    String ip_port = et_ip_port.getText().toString();

                    String ip = String.format("%s.%s.%s.%s", ip_1st, ip_2nd, ip_3rd, ip_4th);
                    int port = Integer.parseInt(ip_port);

                    connect(ip, port);
                    performSelect();

                } catch (NumberFormatException e) {
                    connect("0.0.0.0",0);
                }

            }
        });

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
        stopBroadcast = true;
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
            try {
                // Check wifi is connected.
                ConnectivityManager cManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (!cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                    // TODO: warning wifi is off
                    Log.e(TAG, "WiFi is off.");
                    return;
                }

                WifiManager wManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wManager.getConnectionInfo();
                DhcpInfo dhcpInfo = wManager.getDhcpInfo();
                int self_ip = info.getIpAddress();
                int netmask = dhcpInfo.netmask;
                /*
                Log.e(TAG, String.format("ipp = %d.%d.%d.%d",
                                          self_ip >> 24 & 0xff, self_ip >> 16 & 0xff,
                                          self_ip >> 8 & 0xff, self_ip & 0xff));
                Log.e(TAG, String.format("netmask = %d.%d.%d.%d",
                                          netmask >> 24 & 0xff, netmask >> 16 & 0xff,
                                          netmask >> 8 & 0xff, netmask & 0xff));
                                          */
                int broadcast_addr = (self_ip & netmask) | ~netmask;
                byte[] ip = {(byte)(broadcast_addr & 0xff), (byte)(broadcast_addr >> 8 & 0xff),
                                (byte)(broadcast_addr >> 16 & 0xff), (byte)(broadcast_addr >> 24 & 0xff)};

                // Send broadcast
                /*
                DatagramSocket socket = new DatagramSocket(null);
                socket.bind(new InetSocketAddress(PORT));
                //Log.e(TAG, "socket.getBroadcast() = " + socket.getBroadcast());
                InetAddress group = InetAddress.getByAddress(ip);
                String data = "Hello";
                DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, group, PORT);
                while (!stopBroadcast) {
                    socket.send(packet);
                    Thread.sleep(1000);
                }
                */

                // TODO: Apply "in searching" notice
                int bit = 0;
                int tmp = netmask;
                while (tmp > 0) {
                    bit++;
                    tmp = tmp >> 1;
                }
                int zero_bit = 32 - bit;
                int range = 0x01 << zero_bit;
                for (int i=1; i<range; i++) {
                    int to_check = (self_ip & netmask) | (i << bit);
                    byte[] to_check_bytes = ByteBuffer.allocate(4).putInt(to_check).array();

                    if (InetAddress.getByAddress(to_check_bytes).isReachable(500)) {
                        // TODO: Address display error
                        String checked_ip = String.format("%d.%d.%d.%d", (int)to_check_bytes[3], (int)to_check_bytes[2],
                                (int)to_check_bytes[1], (int)to_check_bytes[0]);
                        uiHandler.obtainMessage(ADDRESS_UPDATE_MESSAGE, checked_ip + ":" + PORT).sendToTarget();
                    }
                }

            } catch (IOException e) {
                // TODO: apply error handling.
                Log.e(TAG, "Failed to send broadcast: " + e);
            }

        }
    }

}