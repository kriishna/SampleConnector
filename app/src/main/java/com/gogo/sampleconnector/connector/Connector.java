package com.gogo.sampleconnector.connector;

import android.app.DialogFragment;

import java.util.HashMap;
import java.util.Map;

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

}