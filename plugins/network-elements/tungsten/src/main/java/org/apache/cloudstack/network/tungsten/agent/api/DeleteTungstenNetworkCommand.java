package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class DeleteTungstenNetworkCommand extends TungstenCommand {

    private final String networkUuid;
    private transient TungstenService tungstenService;

    public DeleteTungstenNetworkCommand(String networkUuid, TungstenService tungstenService) {
        this.networkUuid = networkUuid;
        this.tungstenService = tungstenService;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
