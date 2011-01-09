package com.cloud.network.ovs;

import java.util.List;
import java.util.Set;

import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
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

	public void handleVmStateTransition(VMInstanceVO userVm, State vmState);

	public void RouterCheckAndCreateTunnel(Commands cmds,
			VirtualMachineProfile<DomainRouterVO> profile,
			DeployDestination dest);
	
	public void fullSync(List<Pair<String, Long>> states);
	
	public void scheduleFlowUpdateToHosts(Set<Long> affectedVms, boolean updateSeqno, Long delayMs);
}
