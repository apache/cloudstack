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

package com.cloud.capacity;

import com.cloud.host.HostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine;

/**
 * Capacity Manager manages the different capacities
 * available within the Cloud Stack.
 *
 */
public interface CapacityManager extends Manager {
    public boolean releaseVmCapacity(VirtualMachine vm, boolean moveFromReserved, boolean moveToReservered, Long hostId);

    void allocateVmCapacity(VirtualMachine vm, boolean fromLastHost);
    
    /**
     * @param hostId Id of the host to check capacity
     * @param cpu required CPU
     * @param ram required RAM
     * @param checkFromReservedCapacity set to true if ONLY reserved capacity of the host should be looked at
     * @param cpuOverprovisioningFactor factor to apply to the actual host cpu
     * @param considerReservedCapacity (default should be true, set to false if host capacity calculation should not look at reserved capacity at all)
     */
    boolean checkIfHostHasCapacity(long hostId, Integer cpu, long ram, boolean checkFromReservedCapacity, float cpuOverprovisioningFactor, boolean considerReservedCapacity);
    
	void updateCapacityForHost(HostVO host);
    
	/**
     * Returns the allocated capacity for the storage pool. If template is passed in it will include its size if its not already present on the pool
     * @param pool storage pool
     * @param templateForVmCreation template that will be used for vm creation 
     * @return total allocated capacity for the storage pool
     */
    long getAllocatedPoolCapacity(StoragePoolVO pool, VMTemplateVO templateForVmCreation);
}
