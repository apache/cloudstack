package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenVmCommand extends TungstenCommand {

    private final String virtualMachineUuid;

    public DeleteTungstenVmCommand(String virtualMachineUuid) {
        this.virtualMachineUuid = virtualMachineUuid;
    }

    public String getVirtualMachineUuid() {
        return virtualMachineUuid;
    }
}
