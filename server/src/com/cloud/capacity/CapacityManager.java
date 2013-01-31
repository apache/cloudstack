// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.capacity;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.host.HostVO;
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
     * @param cpuOverprovisioningFactor factor to apply to the actual host cpu
     */
    boolean checkIfHostHasCapacity(long hostId, Integer cpu, long ram, boolean checkFromReservedCapacity, float cpuOverprovisioningFactor, boolean considerReservedCapacity);
    
	void updateCapacityForHost(HostVO host);
    
	/**
     * @param pool storage pool
     * @param templateForVmCreation template that will be used for vm creation 
     * @return total allocated capacity for the storage pool
     */
    long getAllocatedPoolCapacity(StoragePoolVO pool, VMTemplateVO templateForVmCreation);
    
    /**
     * Check if specified host's running VM count has reach hypervisor limit
     * @param host the host to be checked
     * @return true if the count of host's running VMs >= hypervisor limit
     */
    boolean checkIfHostReachMaxGuestLimit(HostVO host);
}
