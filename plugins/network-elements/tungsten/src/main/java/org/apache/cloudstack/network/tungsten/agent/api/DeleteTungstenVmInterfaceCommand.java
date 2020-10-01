package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class DeleteTungstenVmInterfaceCommand extends TungstenCommand {

    private final String vmInterfaceUuid;
    private transient TungstenService tungstenService;

    public DeleteTungstenVmInterfaceCommand(String vmInterfaceUuid, TungstenService tungstenService) {
        this.vmInterfaceUuid = vmInterfaceUuid;
        this.tungstenService = tungstenService;
    }

    public String getVmInterfaceUuid() {
        return vmInterfaceUuid;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
