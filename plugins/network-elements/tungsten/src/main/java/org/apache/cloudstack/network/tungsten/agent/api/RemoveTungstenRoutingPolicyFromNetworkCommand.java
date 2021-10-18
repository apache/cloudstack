package org.apache.cloudstack.network.tungsten.agent.api;

public class RemoveTungstenRoutingPolicyFromNetworkCommand extends TungstenCommand {
    private final String networkUuid;
    private final String routingPolicyUuid;

    public RemoveTungstenRoutingPolicyFromNetworkCommand(String networkUuid, String routingPolicyUuid) {
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
