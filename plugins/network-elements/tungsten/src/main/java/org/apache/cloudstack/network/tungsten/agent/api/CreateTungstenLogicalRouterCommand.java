package org.apache.cloudstack.network.tungsten.agent.api;

public class CreateTungstenLogicalRouterCommand extends TungstenCommand {
    private final String name;
    private final String parentUuid;
    private final String pubNetworkUuid;

    public CreateTungstenLogicalRouterCommand(final String name, final String parentUuid, final String pubNetworkUuid) {
        this.name = name;
        this.parentUuid = parentUuid;
        this.pubNetworkUuid = pubNetworkUuid;
    }

    public String getName() {
        return name;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public String getPubNetworkUuid() {
        return pubNetworkUuid;
    }
}
