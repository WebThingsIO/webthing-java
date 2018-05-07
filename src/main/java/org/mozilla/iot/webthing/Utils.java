/**
 * Utility functions.
 */
package org.mozilla.iot.webthing;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Instant;

public class Utils {
    /**
     * Get the default local IP address.
     *
     * @return The IP address, or null if not found.
     */
    public static String getIP() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Get the current time.
     *
     * @return The current time in the form YYYY-mm-ddTHH:MM:SS+00.00
     */
    public static String timestamp() {
        String now = Instant.now().toString().split("\\.")[0];
        return now + "+00:00";
    }
}