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

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.host.Host;
import com.cloud.storage.VMTemplateVO;
import com.cloud.vm.VirtualMachine;

/**
 * Capacity Manager manages the different capacities
 * available within the Cloud Stack.
 *
 */
public interface CapacityManager {

    static final String CpuOverprovisioningFactorCK = "cpu.overprovisioning.factor";
    static final String MemOverprovisioningFactorCK = "mem.overprovisioning.factor";
    static final String StorageCapacityDisableThresholdCK = "pool.storage.capacity.disablethreshold";
    static final String StorageOverprovisioningFactorCK = "storage.overprovisioning.factor";
    static final String StorageAllocatedCapacityDisableThresholdCK = "pool.storage.allocated.capacity.disablethreshold";

    static final ConfigKey<Float> CpuOverprovisioningFactor = new ConfigKey<Float>(Float.class, CpuOverprovisioningFactorCK, "Advanced", "1.0",
        "Used for CPU overprovisioning calculation; available CPU will be (actualCpuCapacity * cpu.overprovisioning.factor)", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Float> MemOverprovisioningFactor = new ConfigKey<Float>(Float.class, MemOverprovisioningFactorCK, "Advanced", "1.0",
        "Used for memory overprovisioning calculation", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> StorageCapacityDisableThreshold = new ConfigKey<Double>("Alert", Double.class, StorageCapacityDisableThresholdCK, "0.85",
        "Percentage (as a value between 0 and 1) of storage utilization above which allocators will disable using the pool for low storage available.", true, ConfigKey.Scope.Zone);
    static final ConfigKey<Double> StorageOverprovisioningFactor = new ConfigKey<Double>("Storage", Double.class, StorageOverprovisioningFactorCK, "2",
        "Used for storage overprovisioning calculation; available storage will be (actualStorageSize * storage.overprovisioning.factor)", true, ConfigKey.Scope.Zone);
    static final ConfigKey<Double> StorageAllocatedCapacityDisableThreshold = new ConfigKey<Double>("Alert", Double.class, StorageAllocatedCapacityDisableThresholdCK, "0.85",
        "Percentage (as a value between 0 and 1) of allocated storage utilization above which allocators will disable using the pool for low allocated storage available.", true,
        ConfigKey.Scope.Zone);

    public boolean releaseVmCapacity(VirtualMachine vm, boolean moveFromReserved, boolean moveToReservered, Long hostId);

    void allocateVmCapacity(VirtualMachine vm, boolean fromLastHost);
    
    /**
     * @param hostId Id of the host to check capacity
     * @param cpu required CPU
     * @param ram required RAM
     * @param cpuOverprovisioningFactor factor to apply to the actual host cpu
     */
    boolean checkIfHostHasCapacity(long hostId, Integer cpu, long ram, boolean checkFromReservedCapacity, float cpuOverprovisioningFactor, float memoryOvercommitRatio, boolean considerReservedCapacity);

	void updateCapacityForHost(Host host);
    
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
    boolean checkIfHostReachMaxGuestLimit(Host host);
}
