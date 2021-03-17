package org.apache.cloudstack.network.tungsten.model;

public class TungstenLoadBalancerMember {
    private final String name;
    private final String ipAddress;
    private final int port;
    private final int weight;

    public TungstenLoadBalancerMember(final String name, final String ipAddress, final int port, final int weight) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getWeight() {
        return weight;
    }
}
