package com.gogo.sampleconnector.connector.tools;


import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.gogo.sampleconnector.connector.Connector;
import com.gogo.sampleconnector.connector.Controller;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Base Connector for Connectors
 */
public class BaseConnector extends Connector {

    public OnAddressSelectedListener mOnAddressSelectedListener;

    /**
     * Create ListView for dialog.
     *
     * @param list address list
     * @return address listview
     */
    protected ListView prepareListView(final ArrayList<String> list) {
        ListView listview = new ListView(BaseConnector.this.getActivity());
        listview.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        ListAdapter adapter = new AddressAdapter(list, BaseConnector.this.getActivity());
        listview.setAdapter(adapter);

        return listview;
    }

    /**
     * Call this view's OnAddressSelectedListener if it is defined.
     *
     * @return True there was an assigned OnAddressSelectedListener
     *          that was called, false otherwise is returned.
     */
    protected boolean performSelect() {
        final boolean result;
        if (null != mOnAddressSelectedListener) {
            mOnAddressSelectedListener.onAddressSelect(this);
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Register a callback to be invoked when address is clicked.
     *
     * @param li The callback that will run
     */
    public void setOnAddressSelectedListener(Connector.OnAddressSelectedListener li) {
        mOnAddressSelectedListener = li;
    }

    public Controller getController() throws IOException {
        throw new IOException("BaseContainer has no controller defined!");
    }
}







