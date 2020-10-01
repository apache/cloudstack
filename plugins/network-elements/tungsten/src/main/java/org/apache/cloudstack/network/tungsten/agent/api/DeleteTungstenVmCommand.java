package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class DeleteTungstenVmCommand extends TungstenCommand {

    private final String virtualMachineUuid;
    private transient TungstenService tungstenService;

    public DeleteTungstenVmCommand(String virtualMachineUuid, TungstenService tungstenService) {
        this.virtualMachineUuid = virtualMachineUuid;
        this.tungstenService = tungstenService;
    }

    public String getVirtualMachineUuid() {
        return virtualMachineUuid;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
