package com.gogo.sampleconnector.connector;

import android.app.DialogFragment;

import java.io.IOException;

/**
 * Connector class.
 */
public abstract class Connector extends DialogFragment {

    public abstract Controller getController() throws IOException;

    /**
     * Interface definition for a callback to be invoked when a address is clicked.
     */
    public interface OnAddressSelectedListener {
        void onAddressSelect(Connector connector);
    }

    /**
     * Interface definition for a callback to be invoked when a connection is established.
     */
    public interface OnConnectionEstablishedListener {
        public void onConnectionEstablish(int status);
    }

    public class ConnectionStatus {
        public static final int SUCCEED = 0x01;
        public static final int FAIL = 0x02;
        public static final int NO_DEVICE = 0x03;
    }
}