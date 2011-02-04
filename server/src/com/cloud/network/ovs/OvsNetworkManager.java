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
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsNetworkManager extends Manager {
	public boolean isOvsNetworkEnabled();

	public void VmCheckAndCreateTunnel(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest);
	
	public void handleVmStateTransition(VMInstanceVO userVm, State vmState);
	
	public void fullSync(List<Pair<String, Long>> states);
	
	public void scheduleFlowUpdateToHosts(Set<Long> affectedVms, boolean updateSeqno, Long delayMs);

    String applyDefaultFlow(VirtualMachine instance, DeployDestination dest);
}
