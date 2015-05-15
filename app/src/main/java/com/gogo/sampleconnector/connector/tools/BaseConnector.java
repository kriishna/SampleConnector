package com.gogo.sampleconnector.connector.tools;

import android.app.FragmentManager;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.gogo.sampleconnector.connector.Connector;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Base Connector for Connectors
 */
public abstract class BaseConnector extends Connector {

    public static final int CONNECT_STATUS = 0x0a;

    protected Handler mHandler;

    public OnAddressSelectedListener mOnAddressSelectedListener;
    public OnConnectionEstablishedListener mOnConnectionEstablishedListener;

    /**
     * Set up Handler when fragment is ready to show.
     *
     * @param manager
     * @param tag
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        // A handler to run notify method on main thread.
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECT_STATUS:
                        if ( msg.arg1 == ConnectionStatus.SUCCEED) {
                            notifyConnectionEstablished(true, null);
                        } else {
                            notifyConnectionEstablished(false, (String) msg.obj);
                        }
                        break;
                }
            }
        };
    }

    /**
     * Get connector type. Override this method in sub class.
     *
     * @return connector type
     */
    public abstract String getConnectorType();

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
     * Call this view's OnConnectionEstablishedListener if it
     * is defined.
     *
     * @return True there was an assigned OnConnectionEstablishedListener
     *          that was called, false otherwise is returned.
     */
    protected boolean notifyConnectionEstablished(boolean connectResult, String reason) {
        final boolean result;
        if (null != mOnConnectionEstablishedListener) {
            mOnConnectionEstablishedListener.onConnectionEstablish(connectResult, reason, this);
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

    /**
     * Register a callback to be invoked when connection is established.
     *
     * @param li The callback that will run
     */
    public void setOnConnectionEstablishedListener(OnConnectionEstablishedListener li) {
        mOnConnectionEstablishedListener = li;
    }

    /**
     * Get connection controller.
     *
     * @return BaseController of established connection.
     * @throws IOException
     */
    public abstract BaseController getController() throws IOException;

    /**
     * Interface definition for a callback to be invoked when a connection is established.
     */
    public interface OnConnectionEstablishedListener {
        public void onConnectionEstablish(boolean result, String reason, BaseConnector connector) ;
    }
}







