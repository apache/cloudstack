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
package com.cloud.storage.allocator;

import java.util.List;
import java.util.Set;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Allocator for a disk.  This determines which StoragePool should
 * a disk be allocated to.
 */
public interface StoragePoolAllocator extends Adapter {
	
	//keeping since storageMgr is using this API for some existing functionalities
	List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, long dcId, long podId, Long clusterId, Set<? extends StoragePool> avoids, int returnUpTo);	
	
	String chooseStorageIp(VirtualMachine vm, Host host, Host storage);

	/** 
	* Determines which storage pools are suitable for the guest virtual machine 
	* 
	* @param DiskProfile dskCh
	* @param VirtualMachineProfile vmProfile
	* @param DeploymentPlan plan
	* @param ExcludeList avoid
	* @param int returnUpTo (use -1 to return all possible pools)
	* @return List<StoragePool> List of storage pools that are suitable for the VM 
	**/ 
	List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo);	
	
	public static int RETURN_UPTO_ALL = -1;
}
