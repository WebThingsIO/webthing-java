/**
 * Utility functions.
 */
package org.mozilla.iot.webthing;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.time.Instant;
import java.util.Enumeration;

public class Utils {
    /**
     * Get the default local IP address.
     *
     * @return The IP address, or null if not found.
     */
    public static String getIP() {
        try {
            final InetAddress address = Inet4Address.getLocalHost();
            if (isValidAddress(address)) {
                return formatAddress(address);
            }
        } catch (UnknownHostException e) {
            // fall through
        }
        try {
            final Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface iface =
                        (NetworkInterface)interfaces.nextElement();
                final Enumeration addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress address = (InetAddress)addresses.nextElement();
                    if (!isValidAddress(address)) {
                        continue;
                    }
                    return formatAddress(address);
                }
            }
        } catch (SocketException e) {
            // return null
        }
        return null;
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

    /**
     * Ensures the address is not a local loopback or multicast address and has
     * no IPv6 scope.
     *
     * @param address Address to check.
     * @return True if the address is not a loopback, multicast or IPv6 with scope address.
     */
    private static boolean isValidAddress(InetAddress address) {
        return !address.isLoopbackAddress() && !address.isMulticastAddress() && !address.getHostAddress().contains("%");
    }

    /**
     * Format the address for consumption by browsers.
     *
     * @param address
     * @return IPv6 address in square brackets, IPv4 address just as IP string.
     */
    private static String formatAddress(InetAddress address) {
        final String hostname = address.getHostAddress();
        if (hostname.contains(":")) {
            return "[" + hostname + "]";
        }
        return hostname;
    }
}