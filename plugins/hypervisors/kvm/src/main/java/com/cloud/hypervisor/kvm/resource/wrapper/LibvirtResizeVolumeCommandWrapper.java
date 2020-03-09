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

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StorageVol;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

/*
 * Uses a local script now, eventually support for virStorageVolResize() will maybe work on qcow2 and lvm and we can do this in libvirt calls
 */
@ResourceWrapper(handles =  ResizeVolumeCommand.class)
public final class LibvirtResizeVolumeCommandWrapper extends CommandWrapper<ResizeVolumeCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtResizeVolumeCommandWrapper.class);

    @Override
    public Answer execute(final ResizeVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String volid = command.getPath();
        final long newSize = command.getNewSize();
        final long currentSize = command.getCurrentSize();
        final String vmInstanceName = command.getInstanceName();
        final boolean shrinkOk = command.getShrinkOk();
        final StorageFilerTO spool = command.getPool();
        final String notifyOnlyType = "NOTIFYONLY";

        if ( currentSize == newSize) {
            // nothing to do
            s_logger.info("No need to resize volume: current size " + currentSize + " is same as new size " + newSize);
            return new ResizeVolumeAnswer(command, true, "success", currentSize);
        }

        try {
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            KVMStoragePool pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());

            final KVMPhysicalDisk vol = pool.getPhysicalDisk(volid);
            final String path = vol.getPath();
            String type = notifyOnlyType;

            if (pool.getType() != StoragePoolType.RBD) {
                type = libvirtComputingResource.getResizeScriptType(pool, vol);
                if (type.equals("QCOW2") && shrinkOk) {
                    return new ResizeVolumeAnswer(command, false, "Unable to shrink volumes of type " + type);
                }
            } else {
                s_logger.debug("Volume " + path + " is on a RBD storage pool. No need to query for additional information.");
            }

            s_logger.debug("Resizing volume: " + path + "," + currentSize + "," + newSize + "," + type + "," + vmInstanceName + "," + shrinkOk);

            /* libvirt doesn't support resizing (C)LVM devices, and corrupts QCOW2 in some scenarios, so we have to do these via Bash script */
            if (pool.getType() != StoragePoolType.CLVM && vol.getFormat() != PhysicalDiskFormat.QCOW2) {
                s_logger.debug("Volume " + path +  " can be resized by libvirt. Asking libvirt to resize the volume.");
                try {
                    final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

                    final Connect conn = libvirtUtilitiesHelper.getConnection();
                    final StorageVol v = conn.storageVolLookupByPath(path);
                    int flags = 0;

                    if (conn.getLibVirVersion() > 1001000 && vol.getFormat() == PhysicalDiskFormat.RAW && pool.getType() != StoragePoolType.RBD) {
                        flags = 1;
                    }
                    if (shrinkOk) {
                        flags = 4;
                    }

                    v.resize(newSize, flags);
                } catch (final LibvirtException e) {
                    return new ResizeVolumeAnswer(command, false, e.toString());
                }
            }
            s_logger.debug("Invoking resize script to handle type " + type);

            final Script resizecmd = new Script(libvirtComputingResource.getResizeVolumePath(), libvirtComputingResource.getCmdsTimeout(), s_logger);
            resizecmd.add("-s", String.valueOf(newSize));
            resizecmd.add("-c", String.valueOf(currentSize));
            resizecmd.add("-p", path);
            resizecmd.add("-t", type);
            resizecmd.add("-r", String.valueOf(shrinkOk));
            resizecmd.add("-v", vmInstanceName);
            final String result = resizecmd.execute();

            if (result != null) {
                if(type.equals(notifyOnlyType)) {
                    return new ResizeVolumeAnswer(command, true, "Resize succeeded, but need reboot to notify guest");
                } else {
                    return new ResizeVolumeAnswer(command, false, result);
                }
            }

            /* fetch new size as seen from libvirt, don't want to assume anything */
            pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
            pool.refresh();
            final long finalSize = pool.getPhysicalDisk(volid).getVirtualSize();
            s_logger.debug("after resize, size reports as " + finalSize + ", requested " + newSize);
            return new ResizeVolumeAnswer(command, true, "success", finalSize);
        } catch (final CloudRuntimeException e) {
            final String error = "Failed to resize volume: " + e.getMessage();
            s_logger.debug(error);
            return new ResizeVolumeAnswer(command, false, error);
        }
    }
}