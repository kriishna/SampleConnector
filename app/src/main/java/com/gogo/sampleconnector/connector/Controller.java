package com.gogo.sampleconnector.connector;

import java.io.IOException;

/**
 * Controller class for manage connection with printer.
 */
public abstract class Controller {

    public abstract boolean send(final byte[] data);
    public abstract void closeConnection() throws NullPointerException, IOException;

    /**
     * Interface definition for a callback to be invoked when a message is received.
     */
    public interface OnMessageReceivedListener {
        public void onMessageReceive(String message);
    }
}
