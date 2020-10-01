package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class CreateTungstenVmCommand extends TungstenCommand {
    private final String vmUuid;
    private final String vmName;
    private transient TungstenService tungstenService;

    public CreateTungstenVmCommand(String vmUuid, String vmName, TungstenService tungstenService) {
        this.vmUuid = vmUuid;
        this.vmName = vmName;
        this.tungstenService = tungstenService;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
