package com.gogo.sampleconnector;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

    private final int DEBUG = 0x0a;
    private final int WARN = 0x0b;
    private ListView mMessageBox;
    private ArrayAdapter<ColoredMessage> mMessageBoxAdapter;

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

        setupDisconnectButton();
        setupDebugger();
        setupInputBox();

        stackMessage("Please select a connection type ...", DEBUG);
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
        closeConnection();
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
     * Set up disconnect button.
     * If available connection exist, close the connection and stack message.
     * If no available connection, stack warning message.
     */
    private void setupDisconnectButton() {
        ((Button) findViewById(R.id.btn_disconnect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConnection();
            }
        });
    }

    /**
     * Close connection if it's available.
     */
    private void closeConnection() {
        if (null != controller) {
            try {
                controller.closeConnection();
                controller = null;
                stackMessage("Close connection.", DEBUG);
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "Failed close connection: " + e);
                stackMessage("Failed to close connection: " + e, WARN);
            }
        } else {
            stackMessage("No available connection.", WARN);
        }
    }

    /**
     * Set up ListView for debugging.
     */
    private void setupDebugger() {
        mMessageBox = (ListView) findViewById(R.id.lv_message_box);
        mMessageBoxAdapter = new ArrayAdapter<ColoredMessage>(this, R.layout.message) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = new TextView(Main.this);
                tv.setTextSize(getResources().getDimension(R.dimen.message_text_size));
                switch (getItem(pos).getLevel()) {
                    case DEBUG:
                        tv.setTextColor(getResources().getColor(R.color.debug_msg_color));
                        break;
                    case WARN:
                        tv.setTextColor(getResources().getColor(R.color.warning_msg_color));
                        break;
                }
                tv.setText(getItem(pos).getMessage());
                return tv;
            }
        };
        mMessageBox.setAdapter(mMessageBoxAdapter);
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
                    fragment.setOnConnectionEstablishedListener(new Connector.OnConnectionEstablishedListener() {
                        @Override
                        public void onConnectionEstablish(int status) {
                            String popMessage = "";
                            switch (status) {
                                case Connector.ConnectionStatus.SUCCEED:
                                    popMessage = "Connection established";
                                    break;
                                case Connector.ConnectionStatus.FAIL:
                                    controller = null;
                                    popMessage = "Failed to establish connection";
                                    break;
                                case Connector.ConnectionStatus.NO_DEVICE:
                                    controller = null;
                                    popMessage = "No device available";
                                    break;
                            }
                            Toast.makeText(Main.this, popMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                    fragment.show(getFragmentManager(), "tag");
                } catch (IllegalAccessException | InstantiationException e) {
                    Log.e(TAG, "Failed to init class " + cls.getSimpleName());
                }
            }
        });
    }

    private void stackMessage(String msg, int level) {
        ColoredMessage coloredMessage = new ColoredMessage(level, msg);
        mMessageBoxAdapter.add(coloredMessage);
        mMessageBox.setSelection(mMessageBoxAdapter.getCount() - 1);
    }

    private final UsbBroadcastReceiver usbBroadcastReceiver = new UsbBroadcastReceiver(Main.this);

    private class ColoredMessage {
        int level;
        String message;

        public ColoredMessage(int l, String msg) {
            this.level = l;
            this.message = msg;
        }

        public int getLevel() {
            return this.level;
        }

        public String getMessage() {
            return this.message;
        }

    }
}
