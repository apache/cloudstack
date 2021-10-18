package org.apache.cloudstack.network.tungsten.agent.api;

public class AddTungstenRoutingPolicyToNetworkCommand extends TungstenCommand {
    private final String networkUuid;
    private final String routingPolicyUuid;

    public AddTungstenRoutingPolicyToNetworkCommand(String networkUuid, String routingPolicyUuid) {
        this.networkUuid = networkUuid;
        this.routingPolicyUuid = routingPolicyUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getRoutingPolicyUuid() {
        return routingPolicyUuid;
    }
}
