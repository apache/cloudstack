package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.vm.NicProfile;
import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class CreateTungstenVmInterfaceCommand extends TungstenCommand {
    private final NicProfile nic;
    private final String virtualNetworkUuid;
    private final String virtualMachineUuid;
    private final String projectUuid;
    private transient TungstenService tungstenService;

    public CreateTungstenVmInterfaceCommand(NicProfile nic, String virtualNetworkUuid, String virtualMachineUuid, String projectUuid, TungstenService tungstenService) {
        this.nic = nic;
        this.virtualNetworkUuid = virtualNetworkUuid;
        this.virtualMachineUuid = virtualMachineUuid;
        this.projectUuid = projectUuid;
        this.tungstenService = tungstenService;
    }

    public NicProfile getNic() {
        return nic;
    }

    public String getVirtualNetworkUuid() {
        return virtualNetworkUuid;
    }

    public String getVirtualMachineUuid() {
        return virtualMachineUuid;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
