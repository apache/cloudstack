package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenNetworkCommand extends TungstenCommand {

    private final String networkUuid;

    public DeleteTungstenNetworkCommand(String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }
}
