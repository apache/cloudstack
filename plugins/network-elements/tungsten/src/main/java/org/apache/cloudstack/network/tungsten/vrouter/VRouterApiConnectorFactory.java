package org.apache.cloudstack.network.tungsten.vrouter;

public class VRouterApiConnectorFactory {
  private static VRouterApiConnector vrouterApiConnector;

  public static VRouterApiConnector getInstance(String host, String port) {
    if (vrouterApiConnector == null) {
      vrouterApiConnector = new VRouterApiConnectorImpl(host, port);
    }

    return vrouterApiConnector;
  }
}
