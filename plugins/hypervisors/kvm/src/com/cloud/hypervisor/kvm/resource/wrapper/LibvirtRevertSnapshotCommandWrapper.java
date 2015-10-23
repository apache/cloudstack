//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;

import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  RevertSnapshotCommand.class)
public final class LibvirtRevertSnapshotCommandWrapper extends CommandWrapper<RevertSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtRevertSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final RevertSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        SnapshotObjectTO snapshot = command.getData();
        VolumeObjectTO volume = snapshot.getVolume();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();
        DataStoreTO snapshotImageStore = snapshot.getDataStore();
        if (!(snapshotImageStore instanceof NfsTO)) {
            return new Answer(command, false, "revert snapshot on object storage is not implemented yet");
        }
        NfsTO nfsImageStore = (NfsTO) snapshotImageStore;

        String secondaryStoragePoolUrl = nfsImageStore.getUrl();

        String volumePath = volume.getPath();
        String snapshotPath = null;
        String snapshotRelPath = null;
        KVMStoragePool secondaryStoragePool = null;
        try {
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);
            String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = snapshot.getPath();
            snapshotPath = ssPmountPath + File.separator + snapshotRelPath;

            KVMPhysicalDisk snapshotDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(),
                    primaryStore.getUuid(), volumePath);
            KVMStoragePool primaryPool = snapshotDisk.getPool();

            if (primaryPool.getType() == StoragePoolType.RBD) {
                return new Answer(command, false, "revert snapshot to RBD is not implemented yet");
            } else {
                Script cmd = new Script(libvirtComputingResource.manageSnapshotPath(), libvirtComputingResource.getCmdsTimeout(), s_logger);
                cmd.add("-v", snapshotPath);
                cmd.add("-n", snapshotDisk.getName());
                cmd.add("-p", snapshotDisk.getPath());
                String result = cmd.execute();
                if (result != null) {
                    s_logger.debug("Failed to revert snaptshot: " + result);
                    return new Answer(command, false, result);
                }
            }

            return new Answer(command, true, "RevertSnapshotCommand executes successfully");
        } catch (CloudRuntimeException e) {
            return new Answer(command, false, e.toString());
        }
    }
}
