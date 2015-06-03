package com.gogo.sampleconnector.connector.tools;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.gogo.sampleconnector.connector.Controller;


/**
 * Base controller that define the receiving thread.
 */
public abstract class BaseController extends Controller {

    public static final int RECEIVED_MESSAGE = 0x0b;

    protected Handler mainThreadHandler;

    public OnMessageReceivedListener mOnMessageReceivedListener;

    public BaseController() {
        mainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RECEIVED_MESSAGE:
                        /* Modify here to print out string message or hex value
                        byte[] buffer = (byte[]) msg.obj;
                        String hexValue = "";
                        for (int i=0; i<buffer.length; i++) {
                            hexValue += String.format("%02X", buffer[i]);
                        }
                        sendbackMessage(hexValue);
                        */
                        sendbackMessage((String) msg.obj);
                        break;
                }
            }
        };
    }

    public abstract void beginReceiving();

    /**
     * Call this view's OnMessageReceivedListener if it
     * is defined.
     *
     * @return True there was an assigned OnMessageReceivedListener
     *          that was called, false otherwise is returned.
     */
    protected boolean sendbackMessage(String msg) {
        final boolean result;
        if (null != mOnMessageReceivedListener) {
            mOnMessageReceivedListener.onMessageReceive(msg);
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Register a callback to be invoked when message is received.
     *
     * @param li The callback that will run
     */
    public void setOnMessageReceivedListener(OnMessageReceivedListener li) {
        mOnMessageReceivedListener = li;
    }


}