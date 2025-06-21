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

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConvertSnapshotAnswer;
import com.cloud.agent.api.ConvertSnapshotCommand;
import com.cloud.agent.api.to.DataStoreTO;

import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;

import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.io.File;
import java.util.Set;

@ResourceWrapper(handles = ConvertSnapshotCommand.class)
public class LibvirtConvertSnapshotCommandWrapper extends CommandWrapper<ConvertSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(ConvertSnapshotCommand command, LibvirtComputingResource serverResource) {
        SnapshotObjectTO snapshotObjectTO = command.getSnapshotObjectTO();
        DataStoreTO imageStore = snapshotObjectTO.getDataStore();

        logger.debug(String.format("Converting snapshot [%s] in image store [%s].", snapshotObjectTO.getId(), imageStore.getUuid()));

        if (!(imageStore instanceof NfsTO)) {
            return new Answer(command, false, "Image Store must be NFS.");
        }
        NfsTO nfsImageStore = (NfsTO)imageStore;

        String secondaryStoragePoolUrl = nfsImageStore.getUrl();

        Set<KVMStoragePool> storagePoolSet = null;
        KVMStoragePool secondaryStorage = null;
        try {
            secondaryStorage = serverResource.getStoragePoolMgr().getStoragePoolByURI(secondaryStoragePoolUrl);
            storagePoolSet =  serverResource.connectToAllVolumeSnapshotSecondaryStorages(snapshotObjectTO.getVolume());

            String snapshotRelativePath = snapshotObjectTO.getPath();
            String snapshotPath = secondaryStorage.getLocalPathFor(snapshotRelativePath);

            String tempSnapshotPath = snapshotPath + ConvertSnapshotCommand.TEMP_SNAPSHOT_NAME;

            logger.debug(String.format("Converting snapshot [%s] to [%s]. The original snapshot is at [%s].", snapshotObjectTO.getId(), tempSnapshotPath, snapshotPath));

            QemuImg qemuImg = new QemuImg(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.INCREMENTAL_SNAPSHOT_TIMEOUT) * 1000);

            QemuImgFile snapshot = new QemuImgFile(snapshotPath, QemuImg.PhysicalDiskFormat.QCOW2);
            QemuImgFile tempSnapshot = new QemuImgFile(tempSnapshotPath, QemuImg.PhysicalDiskFormat.QCOW2);

            qemuImg.convert(snapshot, tempSnapshot);

            SnapshotObjectTO convertedSnapshot = new SnapshotObjectTO();
            convertedSnapshot.setPath(snapshotRelativePath + ConvertSnapshotCommand.TEMP_SNAPSHOT_NAME);

            final File snapFile = new File(tempSnapshotPath);

            if (!snapFile.exists()) {
                return new Answer(command, false, "Failed to convert snapshot.");
            }

            convertedSnapshot.setPhysicalSize(snapFile.length());
            logger.debug(String.format("Successfully converted snapshot [%s] to [%s].", snapshotObjectTO.getId(), tempSnapshotPath));

            return new ConvertSnapshotAnswer(convertedSnapshot);
        } catch (LibvirtException | QemuImgException ex) {
            logger.error(String.format("Failed to convert snapshot [%s] due to %s.", snapshotObjectTO, ex.getMessage()), ex);
            return new Answer(command, ex);
        } finally {
            if (secondaryStorage != null) {
                serverResource.getStoragePoolMgr().deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
            if (storagePoolSet != null) {
                serverResource.disconnectAllVolumeSnapshotSecondaryStorages(storagePoolSet);
            }
        }
    }
}
