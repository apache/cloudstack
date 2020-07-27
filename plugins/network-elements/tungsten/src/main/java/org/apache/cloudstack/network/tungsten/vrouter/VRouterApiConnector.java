package org.apache.cloudstack.network.tungsten.vrouter;

import java.io.IOException;

public interface VRouterApiConnector {
  boolean addPort(Port port) throws IOException;

  boolean deletePort(String portId);

  boolean enablePort(String portId);

  boolean disablePort(String portId);
}
