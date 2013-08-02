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
package com.cloud.storage;

import java.util.Map;

import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Volume.Type;
import com.cloud.user.Account;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface VolumeManager extends VolumeApiService {
    VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId, Long destPoolPodId,
            Long destPoolClusterId, HypervisorType dataDiskHyperType)
            throws ConcurrentOperationException;

    @Override
    VolumeVO uploadVolume(UploadVolumeCmd cmd)
            throws ResourceAllocationException;

    VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId);

    boolean volumeOnSharedStoragePool(VolumeVO volume);

    boolean volumeInactive(Volume volume);

    String getVmNameOnVolume(Volume volume);

    @Override
    VolumeVO allocVolume(CreateVolumeCmd cmd)
            throws ResourceAllocationException;

    @Override
    VolumeVO createVolume(CreateVolumeCmd cmd);

    @Override
    VolumeVO resizeVolume(ResizeVolumeCmd cmd)
            throws ResourceAllocationException;

    @Override
    boolean deleteVolume(long volumeId, Account caller)
            throws ConcurrentOperationException;

    void destroyVolume(VolumeVO volume);

    DiskProfile allocateRawVolume(Type type, String name, DiskOfferingVO offering, Long size, VMInstanceVO vm, VMTemplateVO template, Account owner);
    @Override
    Volume attachVolumeToVM(AttachVolumeCmd command);

    @Override
    Volume detachVolumeFromVM(DetachVolumeCmd cmmd);

    void release(VirtualMachineProfile profile);

    void cleanupVolumes(long vmId) throws ConcurrentOperationException;

    @Override
    Volume migrateVolume(MigrateVolumeCmd cmd);

    void migrateVolumes(VirtualMachine vm, VirtualMachineTO vmTo, Host srcHost, Host destHost,
            Map<Volume, StoragePool> volumeToPool);

    boolean storageMigration(VirtualMachineProfile vm, StoragePool destPool);

    void prepareForMigration(VirtualMachineProfile vm, DeployDestination dest);

    void prepare(VirtualMachineProfile vm,
            DeployDestination dest) throws StorageUnavailableException,
            InsufficientStorageCapacityException, ConcurrentOperationException;

    boolean canVmRestartOnAnotherServer(long vmId);

    DiskProfile allocateTemplatedVolume(Type type, String name,
            DiskOfferingVO offering, VMTemplateVO template, VMInstanceVO vm,
            Account owner);


    String getVmNameFromVolumeId(long volumeId);

    String getStoragePoolOfVolume(long volumeId);

    boolean validateVolumeSizeRange(long size);
}
