package com.cloud.network.ovs;

import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsNetworkManager extends Manager {
	public boolean isOvsNetworkEnabled();

	public long askVlanId(long accountId, long hostId);

	public String getVlanMapping(long accountId);

	public void CheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest);

	public void applyDefaultFlow(Commands cmds,
			VirtualMachineProfile<UserVmVO> profile, DeployDestination dest);
	
	public void CheckAndUpdateDhcpFlow(Network nw);
	public void handleVmStateTransition(UserVm userVm, State vmState);
}
