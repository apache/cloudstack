package org.apache.cloudstack.network.tungsten.service;

import org.apache.cloudstack.network.tungsten.vrouter.Gateway;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class TungstenVRouterApi {
    private static final Logger S_LOGGER = Logger.getLogger(TungstenVRouterApi.class);

    private static VRouterApiConnector getvRouterApiConnector(String host) {
        return VRouterApiConnectorFactory.getInstance(host);
    }

    public static boolean addTungstenVrouterPort(String host, Port port) {
        try {
            return getvRouterApiConnector(host).addPort(port);
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean deleteTungstenVrouterPort(String host, String portId) {
        return getvRouterApiConnector(host).deletePort(portId);
    }

    public static boolean addTungstenRoute(String host, List<Gateway> gateways) {
        try {
            return getvRouterApiConnector(host).addGateway(gateways);
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean deleteTungstenRoute(String host, List<Gateway> gateways) {
        try {
            return getvRouterApiConnector(host).deleteGateway(gateways);
        } catch (IOException ex) {
            return false;
        }
    }
}
