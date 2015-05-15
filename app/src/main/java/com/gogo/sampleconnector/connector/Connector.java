package com.gogo.sampleconnector.connector;

import android.app.DialogFragment;

import java.io.IOException;

/**
 * Connector class.
 */
public abstract class Connector extends DialogFragment {

    /**
     * Interface definition for a callback to be invoked when a address is clicked.
     */
    public interface OnAddressSelectedListener {
        void onAddressSelect(Connector connector);
    }

    public class ConnectionStatus {
        public static final int SUCCEED = 0x01;
        public static final int FAIL = 0x02;
        public static final String FAIL_ESTABLISH = "Failed to establish connection";
        public static final String NO_DEVICE = "No device available";
        public static final String NO_PERMISSION = "No USB device permission";
        public static final String WAITING_TIMEOUT = "No response, timeout.";
    }
}