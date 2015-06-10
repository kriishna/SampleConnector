package com.gogo.sampleconnector.connector.tools;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gogo.sampleconnector.R;
import com.gogo.sampleconnector.connector.ConnectFailException;
import com.gogo.sampleconnector.connector.ConnectionInformation;
import com.gogo.sampleconnector.connector.scantools.FlashingTextView;
import com.gogo.sampleconnector.connector.scantools.ScanningRunnable;
import com.gogo.sampleconnector.connector.scantools.WiFiBroadcastRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Sii Wifi Connector
 */
public class WiFiConnector extends ScannableConnector {
    final static String TAG = WiFiConnector.class.getSimpleName();

    private WiFiController wifiController;

    private EditText[] et_ip;

    // Handler that handle the updating of address list.
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case ScanningRunnable.FOUND_ADDRESS_UPDATE_MESSAGE:
                    addressAdapter.add((String) message.obj);
                    break;
                case WiFiBroadcastRunnable.UPDATE_SUBNET_ADDRESS_MESSAGE:
                    et_ip[message.arg1].setText((String) message.obj);
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
        dialogTitle = "Connect by wifi ...";

        // Check wifi is connected.
        ConnectivityManager cManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            Message message = mHandler.obtainMessage();
            message.what = BaseConnector.CONNECT_STATUS;
            message.arg1 = ConnectionInformation.ConnectionStatus.FAIL.Value;
            message.obj = ConnectionInformation.ConnectionResult.WIFI_IS_DISABLED;
            message.sendToTarget();
            return null;
        }

        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public String getConnectorType() {
        return "WiFi";
    }

    protected AdapterView.OnItemClickListener setupAddressClickedListener(final ArrayList<String> list) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Index " + position + " is clicked: " + list.get(position));
                String[] params = list.get(position).split(":");
                connect(params[0], Integer.parseInt(params[1]));
                performSelect();
            }
        };
    }

    protected LinearLayout setupAddressEditor() {
        LayoutInflater inflater =
                (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout parent =
                (LinearLayout) inflater.inflate(R.layout.wifi_address_editor, null);

        et_ip = new EditText[4];
        et_ip[0] = (EditText) parent.findViewById(R.id.et_ip_1th);
        et_ip[1] = (EditText) parent.findViewById(R.id.et_ip_2nd);
        et_ip[2] = (EditText) parent.findViewById(R.id.et_ip_3rd);
        et_ip[3] = (EditText) parent.findViewById(R.id.et_ip_4th);
        btPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String ip_1st = et_ip[0].getText().toString();
                    String ip_2nd = et_ip[1].getText().toString();
                    String ip_3rd = et_ip[2].getText().toString();
                    String ip_4th = et_ip[3].getText().toString();
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
        return parent;
    }

    protected ScanningRunnable setupScanningRunnable() {
        FlashingTextView flashitem = new FlashingTextView(getActivity(), 300, flashingHandler);
        return new WiFiBroadcastRunnable(handler, getActivity(), flashitem);
    }

    /**
     * Create connection in another thread. Setup Controller.
     *
     * @param addr
     * @param port
     */
    private void connect(String addr, int port) {
        wifiController = new WiFiController();
        // TODO: Check "address unknown" showing too late bug
        executor.submit(new ConnectRunnable(wifiController, addr, port));
    }

    @Override
    protected boolean performSelect() {
        final boolean result = super.performSelect();
        if (null != scanningRunnable) scanningRunnable.stopScanning();
        executor.shutdown();
        WiFiConnector.this.dismiss();
        return result;
    }

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
                    sendConnectResultMessage(false, new ConnectFailException(
                            ConnectionInformation.ConnectionResult.FAIL_ESTABLISH));
                }
            } catch (UnknownHostException e) {
                sendConnectResultMessage(false, new ConnectFailException(
                        ConnectionInformation.ConnectionResult.ADDRESS_UNKNOWN));
            } catch (NullPointerException|IOException e) {
                sendConnectResultMessage(false, new ConnectFailException(
                        ConnectionInformation.ConnectionResult.FAIL_ESTABLISH));
                Log.e(TAG, "Failed to connect by WiFi: " + e);
            }
        }

        private void sendConnectResultMessage(final boolean result, ConnectFailException exception) {
            Message message = mHandler.obtainMessage();
            message.what = BaseConnector.CONNECT_STATUS;

            if (result) {
                message.arg1 = ConnectionInformation.ConnectionStatus.SUCCEED.Value;
            } else {
                message.arg1 = ConnectionInformation.ConnectionStatus.FAIL.Value;
                message.obj = exception.getMessage();
            }
            message.sendToTarget();
        }
    }

}