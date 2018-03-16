package org.mozilla_iot.webthing;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

public class Utils {
    public static String getIP() {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), 0);
            NetworkInterface iface =  NetworkInterface.getByInetAddress(s.getLocalAddress());
            List<InterfaceAddress> addrs = iface.getInterfaceAddresses();

            for (int i = 0; i < addrs.size(); ++i) {
                InetAddress addr = addrs.get(i).getAddress();
                if (addr.isAnyLocalAddress()) {
                    continue;
                }

                String str = addr.toString();
                if (str.indexOf(':') < 0) {
                    return str;
                }
            }
        } catch (SocketException | UnknownHostException e) {
            return null;
        }

        return null;
    }

    public static String timestamp() {
        return Instant.now().toString().replace("Z", "+00:00");
    }
}