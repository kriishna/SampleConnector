package com.gogo.sampleconnector.connector.tools;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
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
import com.gogo.sampleconnector.connector.ConnectionInformation;
import com.gogo.sampleconnector.connector.scantools.BluetoothScanningRunnable;
import com.gogo.sampleconnector.connector.scantools.FlashingTextView;
import com.gogo.sampleconnector.connector.scantools.ScanningRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Sii Bluetooth Connector
 */
public class BluetoothConnector extends ScannableConnector {
    final static String TAG = BluetoothConnector.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothController bluetoothController;

    public static UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Handler that handle the updating of address list.
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case ScanningRunnable.FOUND_ADDRESS_UPDATE_MESSAGE:
                    addressAdapter.add((String) message.obj);
                    break;
            }
        }
    };

    public static BluetoothConnector newInstance() {
        BluetoothConnector frag = new BluetoothConnector();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialogTitle = "Connect by bluetooth ...";

        /* Check bluetooth device */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            // TODO: pop bluetooth not support warning
            Log.e(TAG, "Bluetooth not support!");
            return null;
        } else if (!mBluetoothAdapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return null;
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public String getConnectorType() {
        return "Bluetooth";
    }

    protected AdapterView.OnItemClickListener setupAddressClickedListener(final ArrayList<String> list) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = list.get(position);
                connect(info.substring(info.length() - 17));
                performSelect();
            }
        };
    }

    protected ScanningRunnable setupScanningRunnable() {
        FlashingTextView flashitem = new FlashingTextView(getActivity(), 300, flashingHandler);
        return new BluetoothScanningRunnable(handler, mBluetoothAdapter, getActivity(), flashitem);
    }

    protected LinearLayout setupAddressEditor() {
        LayoutInflater inflater =
                (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout parent =
                (LinearLayout) inflater.inflate(R.layout.bluetooth_address_editor, null);

        EditText[] et = new EditText[6];
        et[0] = (EditText) parent.findViewById(R.id.et_btaddr_1th);
        et[1] = (EditText) parent.findViewById(R.id.et_btaddr_2nd);
        et[2] = (EditText) parent.findViewById(R.id.et_btaddr_3rd);
        et[3] = (EditText) parent.findViewById(R.id.et_btaddr_4th);
        et[4] = (EditText) parent.findViewById(R.id.et_btaddr_5th);
        et[5] = (EditText) parent.findViewById(R.id.et_btaddr_6th);
        final EditText[] et_btaddr = et;
        btPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String addr_1st = et_btaddr[0].getText().toString();
                    String addr_2nd = et_btaddr[1].getText().toString();
                    String addr_3rd = et_btaddr[2].getText().toString();
                    String addr_4th = et_btaddr[3].getText().toString();
                    String addr_5th = et_btaddr[4].getText().toString();
                    String addr_6th = et_btaddr[5].getText().toString();

                    String address = String.format("%s:%s:%s:%s:%s:%s",
                            addr_1st, addr_2nd, addr_3rd, addr_4th, addr_5th, addr_6th);

                    connect(address);
                    performSelect();

                } catch (NumberFormatException e) {
                    connect("00:00:00:00:00:00");
                }

            }
        });
        return parent;
    }

    /**
     * Create connection in another thread. Setup Controller.
     *
     * @param addr
     */
    private void connect(String addr) {
        bluetoothController = new BluetoothController();
        executor.submit(new ConnectRunnable(bluetoothController, addr));

    }

    @Override
    protected boolean performSelect() {
        final boolean result = super.performSelect();
        if (null != scanningRunnable) scanningRunnable.stopScanning();
        executor.shutdown();
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
            /* Notice user the connection is building */

            Message waiting = mHandler.obtainMessage();
            waiting.what = BaseConnector.CONNECT_STATUS;
            waiting.arg1 = ConnectionInformation.ConnectionStatus.BUILDING.Value;
            waiting.obj = ConnectionInformation.ConnectionResult.BUILDING_CONNECTION;
            waiting.sendToTarget();

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = adapter.getRemoteDevice(address);
            Message message = mHandler.obtainMessage();
            message.what = BaseConnector.CONNECT_STATUS;
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                socket.connect();
                controller.setupConnection(socket);
                message.arg1 = ConnectionInformation.ConnectionStatus.SUCCEED.Value;
                message.sendToTarget();
            } catch (IOException e) {
                message.arg1 = ConnectionInformation.ConnectionStatus.FAIL.Value;
                message.obj = ConnectionInformation.ConnectionResult.FAIL_ESTABLISH;
                Log.e(TAG, "Failed to build bluetooth connection: " + e);
                message.sendToTarget();
            }
        }
    }
}