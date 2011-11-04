/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.ovs;

import java.util.List;
import java.util.Set;

import com.cloud.deploy.DeployDestination;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;

public interface OvsNetworkManager extends Manager {
	public boolean isOvsNetworkEnabled();

	public void VmCheckAndCreateTunnel(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest);
	
	public void handleVmStateTransition(VMInstanceVO userVm, State vmState);
	
	public void fullSync(List<Pair<String, Long>> states);
	
	public void scheduleFlowUpdateToHosts(Set<Long> affectedVms, boolean updateSeqno, Long delayMs);

    String applyDefaultFlow(VirtualMachine instance, DeployDestination dest);
}
