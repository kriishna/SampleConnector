package com.gogo.sampleconnector;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.gogo244.commandcombiner.CommandCombiner;

import com.gogo.sampleconnector.connector.Connector;
import com.gogo.sampleconnector.connector.Controller;
import com.gogo.sampleconnector.connector.tools.BaseConnector;
import com.gogo.sampleconnector.connector.tools.BluetoothConnector;
import com.gogo.sampleconnector.connector.tools.UsbBroadcastReceiver;
import com.gogo.sampleconnector.connector.tools.UsbConnector;
import com.gogo.sampleconnector.connector.tools.WiFiConnector;

import java.io.IOException;


public class Main extends Activity {

    public static final String TAG = Main.class.getSimpleName();
    public static final int COMMAND_REQUEST = 0x01;

    private Controller controller;

    private byte[] command = null;
    private EditText etInputBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setListener((Button) findViewById(R.id.btn_wifi_connect), WiFiConnector.class);
        setListener((Button) findViewById(R.id.btn_bluetooth_connect), BluetoothConnector.class);
        setListener((Button) findViewById(R.id.btn_usb_connect), UsbConnector.class);

        setupInputBox();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(usbBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "" + e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != controller) {
            try {
                controller.closeConnection();
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "Failed close connection: " + e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case COMMAND_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    String cmd = data.getStringExtra(CommandCombiner.EXTRA_COMMAND_STRING);
                    String tmp = etInputBox.getText().toString().isEmpty() ?
                            cmd : etInputBox.getText().toString() + " " + cmd;
                    etInputBox.setText(tmp);
                    byte[] raw = data.getByteArrayExtra(CommandCombiner.EXTRA_COMMAND_BYTES);
                    command = command == null ? raw : CommandCombiner.combineByteArrays(command, raw);
                }
                break;
        }
    }

    /**
     * Set up EditText and sender button.
     */
    private void setupInputBox() {
        etInputBox = (EditText) findViewById(R.id.et_input_box);
        ((Button) findViewById(R.id.btn_add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing if no valid connection.
                if (null == controller) {
                    Toast.makeText(Main.this, "No connection available", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Call command combiner.
                Intent intent = new Intent(Main.this, CommandCombiner.class);
                startActivityForResult(intent, COMMAND_REQUEST);
            }
        });
        ((Button) findViewById(R.id.btn_send)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing if no valid connection.
                if (null == controller || !controller.send(command)) {
                    Toast.makeText(Main.this, "No connection available", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Clean up input box.
                etInputBox.setText("");
                command = null;
            }
        });
    }

    private void setListener(Button b, final Class<? extends BaseConnector> cls) {
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    BaseConnector fragment = cls.newInstance();
                    fragment.setOnAddressSelectedListener(new Connector.OnAddressSelectedListener() {
                        @Override
                        public void onAddressSelect(Connector connector) {
                            // Grant usb permission
                            if (connector instanceof UsbConnector) {
                                ((UsbConnector) connector).setUsbBroadcastReceiver(usbBroadcastReceiver);
                                Main.this.registerReceiver(usbBroadcastReceiver,
                                        new IntentFilter(UsbBroadcastReceiver.ACTION_USB_PERMISSION));
                            }
                            try {

                                controller = connector.getController();
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to get controller: " + e);
                            }
                        }
                    });
                    fragment.show(getFragmentManager(), "tag");
                } catch (IllegalAccessException | InstantiationException e) {
                    Log.e(TAG, "Failed to init class " + cls.getSimpleName());
                }
            }
        });
    }

    private final UsbBroadcastReceiver usbBroadcastReceiver = new UsbBroadcastReceiver(Main.this);
}
