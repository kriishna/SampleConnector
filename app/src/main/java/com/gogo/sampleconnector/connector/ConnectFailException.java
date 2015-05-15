package com.gogo.sampleconnector.connector;

/**
 * Exception thrown by connection establish fail.
 */
public class ConnectFailException extends Exception {

    public ConnectFailException() {
        super("Unknown reason");
    }

    public ConnectFailException(String detailMessage) {
        super(detailMessage);
    }

}
