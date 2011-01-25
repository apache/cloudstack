package com.cloud.network.ovs;

import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsTunnelManager extends Manager {
	boolean isOvsTunnelEnabled();

    public void UserVmCheckAndCreateTunnel(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest);

    public void RouterCheckAndCreateTunnel(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest);
    
    public void CheckAndDestroyTunnel(VMInstanceVO vm);
}
