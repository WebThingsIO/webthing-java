/**
 * Utility functions.
 */
package io.webthings.webthing;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {
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
     * Get all IP addresses.
     *
     * @return List of addresses.
     */
    public static List<String> getAddresses() {
        Set<String> addresses = new HashSet<>();
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                    // Sometimes, IPv6 addresses will have the interface name
                    // appended as, e.g. %eth0. Handle that.
                    String s = address.getHostAddress()
                                      .split("%")[0].toLowerCase();

                    // Filter out link-local addresses.
                    if (s.contains(":")) {
                        if (!s.startsWith("fe80:")) {
                            s = s.replaceFirst("(^|:)(0+(:|$)){2,8}", "::");
                            addresses.add(String.format("[%s]", s));
                        }
                    } else {
                        if (!s.startsWith("169.254.")) {
                            addresses.add(s);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            return Arrays.asList("127.0.0.1");
        }

        List<String> ret = new ArrayList<>(addresses);
        Collections.sort(ret);
        return ret;
    }
}
