package com.cloud.network.ovs;

import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsTunnelManager extends Manager {
	boolean isOvsTunnelEnabled();

    public void VmCheckAndCreateTunnel(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest);
    
    public void CheckAndDestroyTunnel(VirtualMachine vm);
}
