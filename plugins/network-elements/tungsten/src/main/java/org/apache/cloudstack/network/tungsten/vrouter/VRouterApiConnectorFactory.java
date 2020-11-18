package org.apache.cloudstack.network.tungsten.vrouter;

import java.util.HashMap;
import java.util.Map;

public class VRouterApiConnectorFactory {
    private static String port = "9091";
    private static Map<String, VRouterApiConnector> vrouterApiConnectors = new HashMap<>();

    public static VRouterApiConnector getInstance(String host) {
        if (vrouterApiConnectors.get(host) == null) {
            vrouterApiConnectors.put(host, new VRouterApiConnectorImpl(host, port));
        }
        return vrouterApiConnectors.get(host);
    }
}
