package com.gogo.sampleconnector.connector.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sii Bluetooth Connector
 */
public class BluetoothConnector extends BaseConnector {
    final static String TAG = BluetoothConnector.class.getSimpleName();

    BluetoothController bluetoothController;

    public static UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static BluetoothConnector newInstance() {
        BluetoothConnector frag = new BluetoothConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create address list
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("00:0B:5D:B4:CB:EE");
        tmp.add("00:0B:5D:B4:CB:EF");

        final ArrayList<String> addrs = tmp;

        String title = "Connect by bluetooth ...";
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(prepareListView(addrs))
                .create();
    }

    @Override
    public String getConnectorType() {
        return "Bluetooth";
    }

    @Override
    protected ListView prepareListView(final ArrayList<String> list) {
        ListView listview = super.prepareListView(list);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Index " + position + " is clicked: " + list.get(position));
                connect(list.get(position));
                performSelect();
            }
        });
        return listview;
    }

    /**
     * Create connection in another thread. Setup Controller.
     *
     * @param addr
     */
    private void connect(String addr) {
        bluetoothController = new BluetoothController();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new ConnectRunnable(bluetoothController, addr));

    }

    @Override
    protected boolean performSelect() {
        final boolean result = super.performSelect();
        BluetoothConnector.this.dismiss();
        return result;
    }

    /**
     * Get bluetooth connection controller.
     *
     * @return BluetoothController
     * @throws IOException if BluetoothController is null
     */
    @Override
    public BaseController getController() throws IOException {
        if (null == bluetoothController) throw new IOException("BluetoothController is null!");
        return bluetoothController;
    }

    /**
     * Runnable that construct connection and setup controller.
     */
    private class ConnectRunnable implements Runnable {

        BluetoothController controller;
        String address;

        public ConnectRunnable(BluetoothController controller, String address) {
            this.controller = controller;
            this.address = address;
        }

        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = adapter.getRemoteDevice(address);
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                socket.connect();
                mHandler.obtainMessage(BaseConnector.CONNECT_STATUS,
                        ConnectionStatus.SUCCEED).sendToTarget();
                controller.setupConnection(socket);
            } catch (IOException e) {
                mHandler.obtainMessage(BaseConnector.CONNECT_STATUS,
                        ConnectionStatus.FAIL).sendToTarget();
                Log.e(TAG, "Failed to create bluetooth socket: " + e);
            }
        }
    }
}