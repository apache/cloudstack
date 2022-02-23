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

import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.StorPoolResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;


@ResourceWrapper(handles = StorPoolResizeVolumeCommand.class)
public final class StorPoolResizeVolumeCommandWrapper extends CommandWrapper<StorPoolResizeVolumeCommand, ResizeVolumeAnswer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(StorPoolResizeVolumeCommandWrapper.class);

    @Override
    public ResizeVolumeAnswer execute(final StorPoolResizeVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String volid = command.getPath();
        final long newSize = command.getNewSize();
        final long currentSize = command.getCurrentSize();
        final String vmInstanceName = command.getInstanceName();
        final boolean shrinkOk = command.getShrinkOk();
        final StorageFilerTO spool = command.getPool();
        String volPath = null;

        if (currentSize == newSize) {
            // nothing to do
            s_logger.info("No need to resize volume: current size " + currentSize + " is same as new size " + newSize);
            return new ResizeVolumeAnswer(command, true, "success", currentSize);
        }

        try {
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            KVMStoragePool pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());

            final KVMPhysicalDisk vol = pool.getPhysicalDisk(volid);
            final String path = vol.getPath();
            volPath = path;
            if (!command.isAttached()) {
                StorPoolStorageAdaptor.attachOrDetachVolume("attach", "volume", path);
            }
            final Script resizecmd = new Script(libvirtComputingResource.getResizeVolumePath(), libvirtComputingResource.getCmdsTimeout(), s_logger);
            resizecmd.add("-s", String.valueOf(newSize));
            resizecmd.add("-c", String.valueOf(currentSize));
            resizecmd.add("-p", path);
            resizecmd.add("-t", "NOTIFYONLY");
            resizecmd.add("-r", String.valueOf(shrinkOk));
            resizecmd.add("-v", vmInstanceName);
            final String result = resizecmd.execute();

            if (result != null) {
                return new ResizeVolumeAnswer(command, true, "Resize succeeded, but need reboot to notify guest");
            }

            /* fetch new size as seen from libvirt, don't want to assume anything */
            pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
            pool.refresh();

            final long finalSize = pool.getPhysicalDisk(volid).getVirtualSize();
            s_logger.debug("after resize, size reports as " + finalSize + ", requested " + newSize);
            return new ResizeVolumeAnswer(command, true, "success", finalSize);
        } catch (final Exception e) {
            final String error = "Failed to resize volume: " + e.getMessage();
            s_logger.debug(error);
            return new ResizeVolumeAnswer(command, false, error);
        } finally {
            if (!command.isAttached() && volPath != null) {
                StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", volPath);
            }
        }
    }
}
