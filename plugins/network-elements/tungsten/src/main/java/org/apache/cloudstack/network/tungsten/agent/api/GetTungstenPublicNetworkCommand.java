package org.apache.cloudstack.network.tungsten.agent.api;

public class GetTungstenPublicNetworkCommand extends TungstenCommand {
    private final String parent;
    private final String name;

    public GetTungstenPublicNetworkCommand(final String parent, final String name) {
        this.parent = parent;
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }
}
