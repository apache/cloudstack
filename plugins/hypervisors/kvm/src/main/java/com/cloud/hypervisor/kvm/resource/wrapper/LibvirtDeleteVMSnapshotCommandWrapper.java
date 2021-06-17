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

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  DeleteVMSnapshotCommand.class)
public final class LibvirtDeleteVMSnapshotCommandWrapper extends CommandWrapper<DeleteVMSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtDeleteVMSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final DeleteVMSnapshotCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String vmName = cmd.getVmName();

        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        Domain dm = null;
        DomainSnapshot snapshot = null;
        DomainInfo.DomainState oldState = null;
        boolean tryingResume = false;
        Connect conn = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);

            snapshot = dm.snapshotLookupByName(cmd.getTarget().getSnapshotName());

            oldState = dm.getInfo().state;
            if (oldState == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                s_logger.debug("Suspending domain " + vmName);
                dm.suspend(); // suspend the vm to avoid image corruption
            }

            snapshot.delete(0); // only remove this snapshot, not children

            if (oldState == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                // Resume the VM
                tryingResume = true;
                dm = libvirtComputingResource.getDomain(conn, vmName);
                if (dm.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                    dm.resume();
                }
            }

            return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
        } catch (LibvirtException e) {
            String msg = " Delete VM snapshot failed due to " + e.toString();

            if (dm == null) {
                s_logger.debug("Can not find running vm: " + vmName + ", now we are trying to delete the vm snapshot using qemu-img if the format of root volume is QCOW2");
                VolumeObjectTO rootVolume = null;
                for (VolumeObjectTO volume: cmd.getVolumeTOs()) {
                    if (volume.getVolumeType() == Volume.Type.ROOT) {
                        rootVolume = volume;
                        break;
                    }
                }
                if (rootVolume != null && ImageFormat.QCOW2.equals(rootVolume.getFormat())) {
                    PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) rootVolume.getDataStore();
                    KVMPhysicalDisk rootDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(),
                            primaryStore.getUuid(), rootVolume.getPath());
                    String qemu_img_snapshot = Script.runSimpleBashScript("qemu-img snapshot -l " + rootDisk.getPath() + " | tail -n +3 | awk -F ' ' '{print $2}' | grep ^" + cmd.getTarget().getSnapshotName() + "$");
                    if (qemu_img_snapshot == null) {
                        s_logger.info("Cannot find snapshot " + cmd.getTarget().getSnapshotName() + " in file " + rootDisk.getPath() + ", return true");
                        return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
                    }
                    int result = Script.runSimpleBashScriptForExitValue("qemu-img snapshot -d " + cmd.getTarget().getSnapshotName() + " " + rootDisk.getPath());
                    if (result != 0) {
                        return new DeleteVMSnapshotAnswer(cmd, false,
                                "Delete VM Snapshot Failed due to can not remove snapshot from image file " + rootDisk.getPath()  + " : " + result);
                    } else {
                        return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
                    }
                }
            } else if (snapshot == null) {
                s_logger.debug("Can not find vm snapshot " + cmd.getTarget().getSnapshotName() + " on vm: " + vmName + ", return true");
                return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
            } else if (tryingResume) {
                s_logger.error("Failed to resume vm after delete snapshot " + cmd.getTarget().getSnapshotName() + " on vm: " + vmName + " return true : " + e);
                return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
            }

            s_logger.warn(msg, e);
            return new DeleteVMSnapshotAnswer(cmd, false, msg);
        } finally {
            if (dm != null) {
                // Make sure if the VM is paused, then resume it, in case we got an exception during our delete() and didn't have the chance before
                try {
                    dm = libvirtComputingResource.getDomain(conn, vmName);
                    if (oldState == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && dm.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                        s_logger.debug("Resuming domain " + vmName);
                        dm.resume();
                    }
                    dm.free();
                } catch (LibvirtException e) {
                    s_logger.error("Failed to resume vm after delete snapshot " + cmd.getTarget().getSnapshotName() + " on vm: " + vmName + " return true : " + e);
                }
            }
        }
    }
}
