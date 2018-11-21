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
package org.apache.cloudstack.storage.motion;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;

/**
 * Extends {@link StorageSystemDataMotionStrategy}, allowing KVM hosts to migrate VMs with the ROOT volume on a non managed local storage pool.
 * As {@link StorageSystemDataMotionStrategy} is considering KVM, this implementation also migrates only from/to KVM hosts.
 */
public class KvmNonManagedStorageDataMotionStrategy extends StorageSystemDataMotionStrategy {

    @Inject
    private TemplateDataFactory templateDataFactory;

    /**
     * Uses the canHandle from the Super class {@link StorageSystemDataMotionStrategy}. If the storage pool is of file and the internalCanHandle from {@link StorageSystemDataMotionStrategy} CANT_HANDLE, returns the StrategyPriority.HYPERVISOR strategy priority. otherwise returns CANT_HANDLE.
     * Note that the super implementation (override) is called by {@link #canHandle(Map, Host, Host)} which ensures that {@link #internalCanHandle(Map)} will be executed only if the source host is KVM.
     */
    @Override
    protected StrategyPriority internalCanHandle(Map<VolumeInfo, DataStore> volumeMap) {
        if (super.internalCanHandle(volumeMap) == StrategyPriority.CANT_HANDLE) {
            Set<VolumeInfo> volumeInfoSet = volumeMap.keySet();

            for (VolumeInfo volumeInfo : volumeInfoSet) {
                StoragePoolVO storagePoolVO = _storagePoolDao.findById(volumeInfo.getPoolId());
                if (storagePoolVO.getPoolType() != StoragePoolType.Filesystem && storagePoolVO.getPoolType() != StoragePoolType.NetworkFilesystem) {
                    return StrategyPriority.CANT_HANDLE;
                }
            }
            return StrategyPriority.HYPERVISOR;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * Configures a {@link MigrateDiskInfo} object configured for migrating a File System volume and calls rootImageProvisioning.
     */
    @Override
    protected MigrateCommand.MigrateDiskInfo configureMigrateDiskInfo(VolumeInfo srcVolumeInfo, String destPath) {
        return new MigrateCommand.MigrateDiskInfo(srcVolumeInfo.getPath(), MigrateCommand.MigrateDiskInfo.DiskType.FILE, MigrateCommand.MigrateDiskInfo.DriverType.QCOW2,
                MigrateCommand.MigrateDiskInfo.Source.FILE, destPath);
    }

    /**
     * Generates the volume path by appending the Volume UUID to the Libvirt destiny images path.</br>
     * Example: /var/lib/libvirt/images/f3d49ecc-870c-475a-89fa-fd0124420a9b
     */
    @Override
    protected String generateDestPath(VirtualMachineTO vmTO, VolumeVO srcVolume, Host destHost, StoragePoolVO destStoragePool, VolumeInfo destVolumeInfo) {
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(srcVolume.getDiskOfferingId());
        DiskProfile diskProfile = new DiskProfile(destVolumeInfo, diskOffering, HypervisorType.KVM);
        String templateUuid = getTemplateUuid(destVolumeInfo.getTemplateId());
        CreateCommand rootImageProvisioningCommand = new CreateCommand(diskProfile, templateUuid, destStoragePool, true);

        Answer rootImageProvisioningAnswer = _agentMgr.easySend(destHost.getId(), rootImageProvisioningCommand);

        if (rootImageProvisioningAnswer == null) {
            throw new CloudRuntimeException(String.format("Migration with storage of vm [%s] failed while provisioning root image", vmTO.getName()));
        }

        if (!rootImageProvisioningAnswer.getResult()) {
            throw new CloudRuntimeException(String.format("Unable to modify target volume on the host [host id:%s, name:%s]", destHost.getId(), destHost.getName()));
        }

        String libvirtDestImgsPath = null;
        if (rootImageProvisioningAnswer instanceof CreateAnswer) {
            libvirtDestImgsPath = ((CreateAnswer)rootImageProvisioningAnswer).getVolume().getName();
        }
        // File.getAbsolutePath is used to keep the file separator as it should be and eliminate a verification to check if exists a file separator in the last character of libvirtDestImgsPath.
        return new File(libvirtDestImgsPath, destVolumeInfo.getUuid()).getAbsolutePath();
    }

    /**
     * Returns the template UUID with the given id. If the template ID is null, it returns null.
     */
    protected String getTemplateUuid(Long templateId) {
        if (templateId == null) {
            return null;
        }
        TemplateInfo templateImage = templateDataFactory.getTemplate(templateId, DataStoreRole.Image);
        return templateImage.getUuid();
    }

    /**
     * Sets the volume path as the volume UUID.
     */
    @Override
    protected void setVolumePath(VolumeVO volume) {
        volume.setPath(volume.getUuid());
    }

    /**
     * Return true if the volume should be migrated. Currently only supports migrating volumes on storage pool of the type StoragePoolType.Filesystem.
     * This ensures that volumes on shared storage are not migrated and those on local storage pools are migrated.
     */
    @Override
    protected boolean shouldMigrateVolume(StoragePoolVO sourceStoragePool, Host destHost, StoragePoolVO destStoragePool) {
        return sourceStoragePool.getPoolType() == StoragePoolType.Filesystem;
    }
}
