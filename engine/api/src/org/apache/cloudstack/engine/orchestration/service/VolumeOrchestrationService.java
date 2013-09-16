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

import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
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
    VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType) throws ConcurrentOperationException, StorageUnavailableException;

    Volume allocateDuplicateVolume(Volume oldVol, Long templateId);

    boolean volumeOnSharedStoragePool(Volume volume);

    boolean volumeInactive(Volume volume);

    String getVmNameOnVolume(Volume volume);

    VolumeInfo createVolumeFromSnapshot(Volume volume, Snapshot snapshot) throws StorageUnavailableException;

    Volume migrateVolume(Volume volume, StoragePool destPool) throws StorageUnavailableException;

    void destroyVolume(Volume volume);

    DiskProfile allocateRawVolume(Type type, String name, DiskOffering offering, Long size, VirtualMachine vm, VirtualMachineTemplate template, Account owner);

    VolumeInfo createVolumeOnPrimaryStorage(VirtualMachine vm, Volume rootVolumeOfVm, VolumeInfo volume, HypervisorType rootDiskHyperType) throws NoTransitionException;

    void release(VirtualMachineProfile profile);

    void cleanupVolumes(long vmId) throws ConcurrentOperationException;

    void migrateVolumes(VirtualMachine vm, VirtualMachineTO vmTo, Host srcHost, Host destHost, Map<Volume, StoragePool> volumeToPool);

    boolean storageMigration(VirtualMachineProfile vm, StoragePool destPool) throws StorageUnavailableException;

    void prepareForMigration(VirtualMachineProfile vm, DeployDestination dest);

    void prepare(VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException, ConcurrentOperationException;

    boolean canVmRestartOnAnotherServer(long vmId);

    DiskProfile allocateTemplatedVolume(Type type, String name, DiskOffering offering, VirtualMachineTemplate template, VirtualMachine vm, Account owner);

    String getVmNameFromVolumeId(long volumeId);

    String getStoragePoolOfVolume(long volumeId);

    boolean validateVolumeSizeRange(long size);

    StoragePool findStoragePool(DiskProfile dskCh, DataCenter dc, Pod pod, Long clusterId, Long hostId, VirtualMachine vm, Set<StoragePool> avoid);

    void updateVolumeDiskChain(long volumeId, String path, String chainInfo);
}
