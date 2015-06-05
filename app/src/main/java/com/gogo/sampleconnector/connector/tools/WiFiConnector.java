package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.widget.TextView;

import com.gogo.sampleconnector.R;
import com.gogo.sampleconnector.connector.ConnectFailException;
import com.gogo.sampleconnector.connector.scantools.FlashingItem;
import com.gogo.sampleconnector.connector.scantools.FlashingTextView;
import com.gogo.sampleconnector.connector.scantools.ScanningRunnable;
import com.gogo.sampleconnector.connector.scantools.WiFiBroadcastRunnable;

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

    private WiFiController wifiController;

    private ArrayList<String> addrs = new ArrayList<>();
    private ListView addrListView;
    private ArrayAdapter<String> addrAdapter;

    private ScanningRunnable scanningRunnable;
    private Button picker;

    private LinearLayout dialogLayout;
    private TextView tvScanning;

    private EditText[] et_ip;

    // Handler that handle the updating of address list.
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case ScanningRunnable.ADDRESS_UPDATE_MESSAGE:
                    addrAdapter.add((String) message.obj);
                    break;
                case FlashingItem.FLASHING_MESSAGE:
                    tvScanning.setText((String) message.obj);
                    break;
                case FlashingItem.STOP_FLASHING_MESSAGE:
                    tvScanning.setVisibility(View.INVISIBLE);
                    break;
                case WiFiBroadcastRunnable.UPDATE_ADDRESS_MESSAGE:
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
        String title = "Connect by wifi ...";
        ListView lv = prepareListView(addrs);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = (LinearLayout) inflater.inflate(R.layout.wifi_picker, null);
        tvScanning = (TextView) dialogLayout.findViewById(R.id.tv_scanning);
        picker = (Button) dialogLayout.findViewById(R.id.btn_address_set);

        LinearLayout containerLayout = (LinearLayout) dialogLayout.findViewById(R.id.ll_address_list_container);
        containerLayout.addView(lv);

        setupAddressEditor((ViewGroup) dialogLayout.findViewById(R.id.ll_editor_container));

        String[] messages = {
                getActivity().getResources().getString(R.string.str_scanning_0),
                getActivity().getResources().getString(R.string.str_scanning_1),
                getActivity().getResources().getString(R.string.str_scanning_2),
                getActivity().getResources().getString(R.string.str_scanning_3),
        };
        FlashingTextView flashitem = new FlashingTextView(messages, 300, handler);
        scanningRunnable = new WiFiBroadcastRunnable(handler, getActivity(), flashitem);
        Executors.newSingleThreadExecutor().submit(scanningRunnable);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(dialogLayout)
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
        et_ip = new EditText[4];
        et_ip[0] = (EditText) parent.findViewById(R.id.et_ip_1th);
        et_ip[1] = (EditText) parent.findViewById(R.id.et_ip_2nd);
        et_ip[2] = (EditText) parent.findViewById(R.id.et_ip_3rd);
        et_ip[3] = (EditText) parent.findViewById(R.id.et_ip_4th);
        picker.setOnClickListener(new View.OnClickListener() {
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new ConnectRunnable(wifiController, addr, port));
    }

    @Override
    protected boolean performSelect() {
        final boolean result = super.performSelect();
        if (null != scanningRunnable) scanningRunnable.stopScanning();
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

}