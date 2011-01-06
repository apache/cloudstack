package com.cloud.network.ovs;

import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsNetworkManager extends Manager {
	public boolean isOvsNetworkEnabled();

	public long askVlanId(long accountId, long hostId);

	public String getVlanMapping(long accountId);

	public void UserVmCheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest);

	public void applyDefaultFlowToUserVm(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest);

	public void applyDefaultFlowToRouter(Commands cmds,
			VirtualMachineProfile<DomainRouterVO> profile,
			DeployDestination dest);

	public void CheckAndUpdateDhcpFlow(Network nw);

	public void handleVmStateTransition(UserVm userVm, State vmState);

	public void RouterCheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<DomainRouterVO> profile,
			DeployDestination dest);
}
