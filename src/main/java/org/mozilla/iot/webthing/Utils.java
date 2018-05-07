/**
 * Utility functions.
 */
package org.mozilla.iot.webthing;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.Enumeration;

public class Utils {
    /**
     * Get the default local IP address.
     *
     * @return The IP address, or "127.0.0.1" if not found.
     */
    public static String getIP() {
        try {
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface =
                        (NetworkInterface)interfaces.nextElement();
                Enumeration addresses = iface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = (InetAddress)addresses.nextElement();
                    if (address.isLoopbackAddress() ||
                            address.isMulticastAddress() ||
                            address.isLinkLocalAddress() ||
                            address.getHostAddress().indexOf(":") >= 0) {
                        continue;
                    }

                    return address.getHostAddress();
                }
            }

            return "127.0.0.1";
        } catch (SocketException e) {
            return "127.0.0.1";
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