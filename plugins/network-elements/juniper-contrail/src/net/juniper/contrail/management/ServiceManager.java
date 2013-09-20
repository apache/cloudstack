package net.juniper.contrail.management;

import com.cloud.dc.DataCenter;
import com.cloud.network.Network;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

import net.juniper.contrail.api.response.ServiceInstanceResponse;

public interface ServiceManager {
    /**
     * Create a virtual machine that executes a network service appliance (e.g. vSRX)
     * @param left Left or inside network (e.g. project network).
     * @param right Right or outside network (e.g. public network).
     * @return
     */
    public ServiceVirtualMachine createServiceInstance(DataCenter zone, Account owner, VirtualMachineTemplate template,
            ServiceOffering serviceOffering, String name, Network left, Network right);

    public void startServiceInstance(long instanceId);
    public ServiceInstanceResponse createServiceInstanceResponse(long instanceId);
}
