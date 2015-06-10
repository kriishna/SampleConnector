package com.gogo.sampleconnector.connector;

import java.util.HashMap;
import java.util.Map;

/**
 * Information for connection status and result.
 */
public class ConnectionInformation {


    public static enum ConnectionStatus {
        SUCCEED(0),
        BUILDING(1),
        FAIL(2);

        public final int Value;

        private ConnectionStatus(int value) {
            Value = value;
        }

        private static final Map<Integer, ConnectionStatus> _map =
                new HashMap<Integer, ConnectionStatus>();

        static {
            for (ConnectionStatus status : ConnectionStatus.values()) {
                _map.put(status.Value, status);
            }
        }

        public static ConnectionStatus getValue(int n) {
            return _map.get(n);
        }
    }

    public class ConnectionResult {
        public static final String FAIL_ESTABLISH = "Failed to establish connection";
        public static final String NO_DEVICE = "No device available";
        public static final String NO_PERMISSION = "No USB device permission";
        public static final String WAITING_TIMEOUT = "No response, timeout.";
        public static final String ADDRESS_UNKNOWN = "Address is unknown";
        public static final String BUILDING_CONNECTION = "Connecting ...";
        public static final String WIFI_IS_DISABLED = "WiFi is off";
    }
}
