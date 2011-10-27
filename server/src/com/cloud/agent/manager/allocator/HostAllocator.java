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
package com.cloud.agent.manager.allocator;

import java.util.List;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface HostAllocator extends Adapter {

	/**
	 * Checks if the VM can be upgraded to the specified ServiceOffering
	 * @param UserVm vm
	 * @param ServiceOffering offering
	 * @return boolean true if the VM can be upgraded
	 **/
	boolean isVirtualMachineUpgradable(final UserVm vm, final ServiceOffering offering);

	/** 
	* Determines which physical hosts are suitable to 
	* allocate the guest virtual machines on 
	* 
	* @param VirtualMachineProfile vmProfile
	* @param DeploymentPlan plan
	* @param GuestType type
	* @param ExcludeList avoid
	* @param int returnUpTo (use -1 to return all possible hosts)
	* @return List<Host> List of hosts that are suitable for VM allocation
	**/ 
	
	public List<Host> allocateTo(VirtualMachineProfile<?extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo);
	
    /** 
    * Determines which physical hosts are suitable to 
    * allocate the guest virtual machines on 
    * 
    * @param VirtualMachineProfile vmProfile
    * @param DeploymentPlan plan
    * @param GuestType type
    * @param ExcludeList avoid
    * @param int returnUpTo (use -1 to return all possible hosts)
    * @param boolean considerReservedCapacity (default should be true, set to false if host capacity calculation should not look at reserved capacity)
    * @return List<Host> List of hosts that are suitable for VM allocation
    **/ 
    
    public List<Host> allocateTo(VirtualMachineProfile<?extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity);
	
	
	public static int RETURN_UPTO_ALL = -1;
		
}
