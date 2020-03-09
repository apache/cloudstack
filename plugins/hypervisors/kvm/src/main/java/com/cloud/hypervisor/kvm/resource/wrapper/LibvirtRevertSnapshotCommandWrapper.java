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

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.exceptions.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
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

@ResourceWrapper(handles = RevertSnapshotCommand.class)
public final class LibvirtRevertSnapshotCommandWrapper extends CommandWrapper<RevertSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtRevertSnapshotCommandWrapper.class);
    private static final String MON_HOST = "mon_host";
    private static final String KEY = "key";
    private static final String CLIENT_MOUNT_TIMEOUT = "client_mount_timeout";
    private static final String RADOS_CONNECTION_TIMEOUT = "30";

    @Override
    public Answer execute(final RevertSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        SnapshotObjectTO snapshot = command.getData();
        VolumeObjectTO volume = snapshot.getVolume();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();
        DataStoreTO snapshotImageStore = snapshot.getDataStore();
        if (!(snapshotImageStore instanceof NfsTO) && primaryStore.getPoolType() != StoragePoolType.RBD) {
            return new Answer(command, false,
                    String.format("Revert snapshot does not support storage pool of type [%s]. Revert snapshot is supported by storage pools of type 'NFS' or 'RBD'",
                            primaryStore.getPoolType()));
        }

        String volumePath = volume.getPath();
        String snapshotPath = null;
        String snapshotRelPath = null;
        KVMStoragePool secondaryStoragePool = null;
        try {
            snapshotRelPath = snapshot.getPath();
            KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

            KVMPhysicalDisk snapshotDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volumePath);
            KVMStoragePool primaryPool = snapshotDisk.getPool();

            if (primaryPool.getType() == StoragePoolType.RBD) {
                Rados rados = new Rados(primaryPool.getAuthUserName());
                rados.confSet(MON_HOST, primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                rados.confSet(KEY, primaryPool.getAuthSecret());
                rados.confSet(CLIENT_MOUNT_TIMEOUT, RADOS_CONNECTION_TIMEOUT);
                rados.connect();

                String[] rbdPoolAndVolumeAndSnapshot = snapshotRelPath.split("/");
                int snapshotIndex = rbdPoolAndVolumeAndSnapshot.length - 1;
                String rbdSnapshotId = rbdPoolAndVolumeAndSnapshot[snapshotIndex];

                IoCTX io = rados.ioCtxCreate(primaryPool.getSourceDir());
                Rbd rbd = new Rbd(io);

                s_logger.debug(String.format("Attempting to rollback RBD snapshot [name:%s], [volumeid:%s], [snapshotid:%s]", snapshot.getName(), volumePath, rbdSnapshotId));

                RbdImage image = rbd.open(volumePath);
                image.snapRollBack(rbdSnapshotId);

                rbd.close(image);
                rados.ioCtxDestroy(io);
            } else {
                NfsTO nfsImageStore = (NfsTO)snapshotImageStore;
                String secondaryStoragePoolUrl = nfsImageStore.getUrl();
                secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);
                String ssPmountPath = secondaryStoragePool.getLocalPath();
                snapshotPath = ssPmountPath + File.separator + snapshotRelPath;

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
        } catch (RadosException e) {
            s_logger.error("Failed to connect to Rados pool while trying to revert snapshot. Exception: ", e);
            return new Answer(command, false, e.toString());
        } catch (RbdException e) {
            s_logger.error("Failed to connect to revert snapshot due to RBD exception: ", e);
            return new Answer(command, false, e.toString());
        }
    }
}
