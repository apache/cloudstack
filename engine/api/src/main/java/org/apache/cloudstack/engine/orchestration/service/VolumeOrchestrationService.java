/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.orchestration.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.StorageAccessException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.Snapshot;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * VolumeOrchestrationService is a PURE orchestration service on CloudStack
 * volumes.  It does not understand resource limits, ACL, action events, or
 * anything that has to do with the self-service portion of CloudStack.  Its
 * job is to carry out any orchestration needed among the physical components
 * to provision volumes.
 */
public interface VolumeOrchestrationService {

    ConfigKey<Long> CustomDiskOfferingMinSize = new ConfigKey<Long>("Advanced",
        Long.class,
        "custom.diskoffering.size.min",
        "1",
        "Minimum size in GB for custom disk offering.",
        true
    );

    ConfigKey<Long> CustomDiskOfferingMaxSize = new ConfigKey<Long>("Advanced",
        Long.class,
        "custom.diskoffering.size.max",
        "1024",
        "Maximum size in GB for custom disk offering.",
        true
    );

    VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType)
        throws ConcurrentOperationException, StorageUnavailableException;

    Volume allocateDuplicateVolume(Volume oldVol, Long templateId);

    boolean volumeOnSharedStoragePool(Volume volume);

    boolean volumeInactive(Volume volume);

    String getVmNameOnVolume(Volume volume);

    StoragePool findChildDataStoreInDataStoreCluster(DataCenter dc, Pod pod, Long clusterId, Long hostId, VirtualMachine vm, Long datastoreClusterId);

    VolumeInfo createVolumeFromSnapshot(Volume volume, Snapshot snapshot, UserVm vm) throws StorageUnavailableException;

    Volume migrateVolume(Volume volume, StoragePool destPool) throws StorageUnavailableException;

    Volume liveMigrateVolume(Volume volume, StoragePool destPool);

    void cleanupStorageJobs();

    void destroyVolume(Volume volume);

    DiskProfile allocateRawVolume(Type type, String name, DiskOffering offering, Long size, Long minIops, Long maxIops, VirtualMachine vm, VirtualMachineTemplate template,
            Account owner, Long deviceId);

    VolumeInfo createVolumeOnPrimaryStorage(VirtualMachine vm, VolumeInfo volume, HypervisorType rootDiskHyperType, StoragePool storagePool) throws NoTransitionException;

    void release(VirtualMachineProfile profile);

    void release(long vmId, long hostId);

    void cleanupVolumes(long vmId) throws ConcurrentOperationException;

    void revokeAccess(DataObject dataObject, Host host, DataStore dataStore);

    void revokeAccess(long vmId, long hostId);

    void migrateVolumes(VirtualMachine vm, VirtualMachineTO vmTo, Host srcHost, Host destHost, Map<Volume, StoragePool> volumeToPool);

    boolean storageMigration(VirtualMachineProfile vm, Map<Volume, StoragePool> volumeToPool) throws StorageUnavailableException;

    void prepareForMigration(VirtualMachineProfile vm, DeployDestination dest);

    void prepare(VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException, ConcurrentOperationException, StorageAccessException;

    boolean canVmRestartOnAnotherServer(long vmId);

    /**
     * Allocate a volume or multiple volumes in case of template is registered with the 'deploy-as-is' option, allowing multiple disks
     */
    List<DiskProfile> allocateTemplatedVolumes(Type type, String name, DiskOffering offering, Long rootDisksize, Long minIops, Long maxIops, VirtualMachineTemplate template, VirtualMachine vm,
                                               Account owner);

    String getVmNameFromVolumeId(long volumeId);

    String getStoragePoolOfVolume(long volumeId);

    boolean validateVolumeSizeRange(long size);

    StoragePool findStoragePool(DiskProfile dskCh, DataCenter dc, Pod pod, Long clusterId, Long hostId, VirtualMachine vm, Set<StoragePool> avoid);

    void updateVolumeDiskChain(long volumeId, String path, String chainInfo, String updatedDataStoreUUID);

    /**
     * Imports an existing volume for a VM into database. Useful while ingesting an unmanaged VM.
     * @param type Type of the volume - ROOT, DATADISK, etc
     * @param name Name of the volume
     * @param offering DiskOffering for the volume
     * @param size DiskOffering for the volume
     * @param minIops minimum IOPS for the disk, if not passed DiskOffering value will be used
     * @param maxIops maximum IOPS for the disk, if not passed DiskOffering value will be used
     * @param vm VirtualMachine this volume is attached to
     * @param template Template of the VM of the volume
     * @param owner owner Account for the volume
     * @param deviceId device ID of the virtual disk
     * @param poolId ID of pool in which volume is stored
     * @param path image path of the volume
     * @param chainInfo chain info for the volume. Hypervisor specific.
     * @return  DiskProfile of imported volume
     */
    DiskProfile importVolume(Type type, String name, DiskOffering offering, Long size, Long minIops, Long maxIops, VirtualMachine vm, VirtualMachineTemplate template,
                             Account owner, Long deviceId, Long poolId, String path, String chainInfo);

    /**
     * Unmanage VM volumes
     */
    void unmanageVolumes(long vmId);
}
