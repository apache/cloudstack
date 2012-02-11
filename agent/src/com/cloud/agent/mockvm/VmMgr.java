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

package com.cloud.agent.mockvm;

import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.vm.VirtualMachine.State;

public interface VmMgr {
	public Set<String> getCurrentVMs();

	public String startVM(String vmName, String vnetId, String gateway,
			String dns, String privateIP, String privateMac,
			String privateMask, String publicIP, String publicMac,
			String publicMask, int cpuCount, int cpuUtilization, long ramSize,
			String localPath, String vncPassword);

	public String stopVM(String vmName, boolean force);

	public String rebootVM(String vmName);

	public void cleanupVM(String vmName, String local, String vnet);

	public boolean migrate(String vmName, String params);

	public MockVm getVm(String vmName);

	public State checkVmState(String vmName);

	public Map<String, State> getVmStates();

	public Integer getVncPort(String name);

	public String cleanupVnet(String vnetId);

	public double getHostCpuUtilization();

	public int getHostCpuCount();

	public long getHostCpuSpeed();

	public long getHostTotalMemory();

	public long getHostFreeMemory();

	public long getHostDom0Memory();

	public MockVm createVmFromSpec(VirtualMachineTO vmSpec);

	public void createVbd(VirtualMachineTO vmSpec, String vmName, MockVm vm);

	public void createVif(VirtualMachineTO vmSpec, String vmName, MockVm vm);

	public void configure(Map<String, Object> params);
}
