package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.vm.NicProfile;
import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class CreateTungstenInstanceIpCommand extends TungstenCommand {

    private final NicProfile nic;
    private final String virtualNetworkUuid;
    private final String vmInterfaceUuid;
    private transient TungstenService tungstenService;

    public CreateTungstenInstanceIpCommand(NicProfile nic, String virtualNetworkUuid, String vmInterfaceUuid, TungstenService tungstenService) {
        this.nic = nic;
        this.virtualNetworkUuid = virtualNetworkUuid;
        this.vmInterfaceUuid = vmInterfaceUuid;
        this.tungstenService = tungstenService;
    }

    public NicProfile getNic() {
        return nic;
    }

    public String getVirtualNetworkUuid() {
        return virtualNetworkUuid;
    }

    public String getVmInterfaceUuid() {
        return vmInterfaceUuid;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }
}
