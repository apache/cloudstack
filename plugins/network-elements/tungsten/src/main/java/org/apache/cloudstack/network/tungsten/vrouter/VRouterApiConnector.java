package org.apache.cloudstack.network.tungsten.vrouter;

import java.io.IOException;
import java.util.List;

public interface VRouterApiConnector {
    boolean addPort(Port port) throws IOException;

    boolean deletePort(String portId);

    boolean enablePort(String portId);

    boolean disablePort(String portId);

    boolean addGateway(List<Gateway> gatewayList) throws IOException;

    boolean deleteGateway(List<Gateway> gatewayList) throws IOException;
}
