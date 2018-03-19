package org.mozilla.iot.webthing;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Instant;

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