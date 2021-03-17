package org.apache.cloudstack.network.tungsten.agent.api;

public class UpdateLoadBalancerServiceInstanceCommand extends TungstenCommand {
    private String publicNetworkUuid;
    private String floatingPoolName;
    private String floatingIpName;

    public UpdateLoadBalancerServiceInstanceCommand(final String publicNetworkUuid, final String floatingPoolName,
        final String floatingIpName) {
        this.publicNetworkUuid = publicNetworkUuid;
        this.floatingPoolName = floatingPoolName;
        this.floatingIpName = floatingIpName;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public void setPublicNetworkUuid(final String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }

    public String getFloatingPoolName() {
        return floatingPoolName;
    }

    public void setFloatingPoolName(final String floatingPoolName) {
        this.floatingPoolName = floatingPoolName;
    }

    public String getFloatingIpName() {
        return floatingIpName;
    }

    public void setFloatingIpName(final String floatingIpName) {
        this.floatingIpName = floatingIpName;
    }
}
