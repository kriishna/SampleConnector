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
}