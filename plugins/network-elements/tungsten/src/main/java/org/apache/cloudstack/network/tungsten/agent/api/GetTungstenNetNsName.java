package org.apache.cloudstack.network.tungsten.agent.api;

public class GetTungstenNetNsName extends TungstenCommand {
    private final String logicalRouterUuid;

    public GetTungstenNetNsName(final String logicalRouterUuid) {
        this.logicalRouterUuid = logicalRouterUuid;
    }

    public String getLogicalRouterUuid() {
        return logicalRouterUuid;
    }
}
