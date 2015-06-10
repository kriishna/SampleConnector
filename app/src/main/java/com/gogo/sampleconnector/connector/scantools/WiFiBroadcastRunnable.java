package com.gogo.sampleconnector.connector.scantools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Runnable that perform a wifi broadcast and update address list.
 */
public class WiFiBroadcastRunnable extends ScanningRunnable {
    public static final String TAG = WiFiBroadcastRunnable.class.getSimpleName();

    public static final int UPDATE_SUBNET_ADDRESS_MESSAGE = 0xaa;

    Context context;

    final int PORT = 9100;

    public WiFiBroadcastRunnable(Handler h, Context c, FlashingItem f) {
        super(h, f);
        context = c;
    }

    @Override
    public void run() {
        try {
            // Notice user the scanning is running
            Thread t = new Thread(flashingItem, "ScanningSubnet");
            t.start();

            WifiManager wManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wManager.getConnectionInfo();
            DhcpInfo dhcpInfo = wManager.getDhcpInfo();
            int self_ip = info.getIpAddress();
            int netmask = dhcpInfo.netmask;
                /*
                Log.e(TAG, String.format("ipp = %d.%d.%d.%d",
                                          self_ip >> 24 & 0xff, self_ip >> 16 & 0xff,
                                          self_ip >> 8 & 0xff, self_ip & 0xff));
                Log.e(TAG, String.format("netmask = %d.%d.%d.%d",
                                          netmask >> 24 & 0xff, netmask >> 16 & 0xff,
                                          netmask >> 8 & 0xff, netmask & 0xff));
                                          */
            int broadcast_addr = (self_ip & netmask) | ~netmask;
            byte[] ip = {(byte)(broadcast_addr & 0xff), (byte)(broadcast_addr >> 8 & 0xff),
                    (byte)(broadcast_addr >> 16 & 0xff), (byte)(broadcast_addr >> 24 & 0xff)};

            int bit = 0;
            int tmp = netmask;
            while (tmp > 0) {
                bit++;
                tmp = tmp >> 1;
            }

            // Update subnet ip address
            if (bit > 7) fillAddress((int)broadcast_addr & 0xff, 0);
            if (bit > 15) fillAddress((int)(broadcast_addr >> 8) & 0xff, 1);
            if (bit > 23) fillAddress((int)(broadcast_addr >> 16) & 0xff, 2);

            // Start scanning
            int zero_bit = 32 - bit;
            int range = 0x01 << zero_bit;
            for (int i=1; i<range; i++) {
                if (!isScanning) break;
                int to_check = (self_ip & netmask) | (i << bit);
                byte[] to_check_bytes = ByteBuffer.allocate(4).putInt(to_check).array();
                if (InetAddress.getByAddress(to_check_bytes).isReachable(500)) {
                    String checked_ip = String.format("%d.%d.%d.%d",
                            (int)to_check_bytes[3] & 0xff, (int)to_check_bytes[2] & 0xff,
                            (int)to_check_bytes[1] & 0xff, (int)to_check_bytes[0] & 0xff);
                    mainThreadHandler.obtainMessage(FOUND_ADDRESS_UPDATE_MESSAGE, checked_ip + ":" + PORT).sendToTarget();
                }
            }
            stopScanning();

            // Send broadcast
            /*
            DatagramSocket socket = new DatagramSocket(null);
            socket.bind(new InetSocketAddress(PORT));
            //Log.e(TAG, "socket.getBroadcast() = " + socket.getBroadcast());
            InetAddress group = InetAddress.getByAddress(ip);
            String data = "Hello";
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, group, PORT);
            while (!stopBroadcast) {
                socket.send(packet);
                Thread.sleep(1000);
            }
            */

        } catch (IOException e) {
            // TODO: apply error handling.
            Log.e(TAG, "Failed to send broadcast: " + e);
        }

    }

    private void fillAddress(int part, int order) {
        Message msg = mainThreadHandler.obtainMessage();
        msg.what = UPDATE_SUBNET_ADDRESS_MESSAGE;
        msg.arg1 = order;
        msg.obj = "" + part;
        msg.sendToTarget();
    }

}