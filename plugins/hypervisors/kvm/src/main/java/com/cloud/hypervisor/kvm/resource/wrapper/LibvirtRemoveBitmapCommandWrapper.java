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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.RemoveBitmapCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.json.JSONArray;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import org.json.JSONObject;

import java.io.File;


@ResourceWrapper(handles = RemoveBitmapCommand.class)
public class LibvirtRemoveBitmapCommandWrapper extends CommandWrapper<RemoveBitmapCommand, Answer, LibvirtComputingResource> {

    private static final String QEMU_MONITOR_REMOVE_BITMAP_COMMAND = "{\"execute\": \"block-dirty-bitmap-remove\", \"arguments\":{\"node\":\"%s\",\"name\":\"%s\" }}";
    private static final String QEMU_MONITOR_QUERY_BLOCK_COMMAND = "{\"execute\": \"query-block\"}";


    @Override
    public Answer execute(RemoveBitmapCommand command, LibvirtComputingResource resource) {
        SnapshotObjectTO snapshotObjectTO = command.getSnapshotObjectTO();

        try {
            if (command.isVmRunning()) {
                return removeBitmapForRunningVM(snapshotObjectTO, resource, command);
            }
            return removeBitmapForStoppedVM(snapshotObjectTO, resource, command);
        } catch (LibvirtException | QemuImgException exception) {
            logger.error("Exception while removing bitmap for volume [{}]. Caught exception is [{}].", snapshotObjectTO.getVolume().getName(), exception);
            return new Answer(command, exception);
        }
    }

    protected Answer removeBitmapForRunningVM(SnapshotObjectTO snapshotObjectTO, LibvirtComputingResource resource, RemoveBitmapCommand cmd) throws LibvirtException {
        Domain vm = resource.getDomain(resource.getLibvirtUtilitiesHelper().getConnection(), snapshotObjectTO.getVmName());
        String nodeName = getNodeName(vm, snapshotObjectTO);
        logger.debug("Got [{}] as node-name for volume [{}] of VM [{}].", nodeName, snapshotObjectTO.getVolume().getName(), snapshotObjectTO.getVmName());
        if (nodeName == null) {
            return new Answer(cmd, false, "Failed to get node-name to remove the bitmap.");
        }

        String bitmapName = getBitmapName(snapshotObjectTO);
        logger.debug("Removing bitmap [{}].", bitmapName);
        vm.qemuMonitorCommand(String.format(QEMU_MONITOR_REMOVE_BITMAP_COMMAND, nodeName, bitmapName), 0);
        return new Answer(cmd);
    }

    protected Answer removeBitmapForStoppedVM(SnapshotObjectTO snapshotObjectTO, LibvirtComputingResource resource, Command cmd) throws LibvirtException, QemuImgException {
        VolumeObjectTO volumeTo = snapshotObjectTO.getVolume();
        PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeTo.getDataStore();

        KVMStoragePool primaryPool = resource.getStoragePoolMgr().getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

        QemuImg qemuImg = new QemuImg(cmd.getWait());
        QemuImgFile volume = new QemuImgFile(primaryPool.getLocalPath() + File.separator + volumeTo.getPath(), QemuImg.PhysicalDiskFormat.QCOW2);

        String bitmap = getBitmapName(snapshotObjectTO);

        logger.debug("Removing bitmap [{}] for volume [{}].", bitmap, volumeTo.getName());

        try {
            qemuImg.bitmap(QemuImg.BitmapOperation.Remove, volume, bitmap);
        } catch (QemuImgException ex) {
            if (!(ex.getMessage().contains("Dirty bitmap") || ex.getMessage().contains("not found"))) {
                throw ex;
            }
            logger.warn("Could not delete dirty bitmap [{}] as it was not found. This will happen if the volume was migrated. If it is not the case, this should be reported.", bitmap);
        }
        return new Answer(cmd);
    }


    protected String getBitmapName(SnapshotObjectTO snapshotObjectTO) {
        String[] splitPath = snapshotObjectTO.getPath().split(File.separator);
        return splitPath[splitPath.length - 1];
    }

    protected String getNodeName(Domain vm, SnapshotObjectTO snapshotObjectTO) throws LibvirtException {
        logger.debug("Getting nodeName to remove bitmap for volume [{}] of VM [{}].", snapshotObjectTO.getVolume().getName(), snapshotObjectTO.getVmName());
        String vmBlockInfo = vm.qemuMonitorCommand(QEMU_MONITOR_QUERY_BLOCK_COMMAND, 0);
        logger.debug("Parsing [{}]", vmBlockInfo);
        JSONObject jsonObj = new JSONObject(vmBlockInfo);
        JSONArray returnArray = jsonObj.getJSONArray("return");

        for (int i = 0; i < returnArray.length(); i++) {
            JSONObject blockInfo = returnArray.getJSONObject(i);
            if (!blockInfo.has("inserted")) {
                continue;
            }
            JSONObject inserted = blockInfo.getJSONObject("inserted");
            String volumePath = inserted.getString("file");
            if (!volumePath.contains(snapshotObjectTO.getVolume().getPath())) {
                continue;
            }
            return inserted.getString("node-name");
        }
        return null;
    }
}
