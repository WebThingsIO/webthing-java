package org.mozilla.iot.webthing;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

public class Utils {
    public static String getIP() {
        try {
            return Inet4Address.getLocalHost().getHostAddress().toString();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static String timestamp() {
        String now = Instant.now().toString().split("\\.")[0];
        return now + "+00:00";
    }
}