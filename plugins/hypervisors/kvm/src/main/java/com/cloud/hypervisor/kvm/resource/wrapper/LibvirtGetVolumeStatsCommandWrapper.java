//
//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.HashMap;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ResourceWrapper(handles = GetVolumeStatsCommand.class)
public final class LibvirtGetVolumeStatsCommandWrapper extends CommandWrapper<GetVolumeStatsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final GetVolumeStatsCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            String storeUuid = cmd.getPoolUuid();
            StoragePoolType poolType = cmd.getPoolType();
            HashMap<String, VolumeStatsEntry> statEntry = new HashMap<String, VolumeStatsEntry>();
            for (String volumeUuid : cmd.getVolumeUuids()) {
                VolumeStatsEntry volumeStatsEntry = getVolumeStat(libvirtComputingResource, conn, volumeUuid, storeUuid, poolType);
                if (volumeStatsEntry == null) {
                    String msg = "Can't get disk stats as pool or disk details unavailable for volume: " + volumeUuid + " on the storage pool: " + storeUuid;
                    return new GetVolumeStatsAnswer(cmd, msg, null);
                }
                statEntry.put(volumeUuid, volumeStatsEntry);
            }
            return new GetVolumeStatsAnswer(cmd, "", statEntry);
        } catch (LibvirtException | CloudRuntimeException e) {
            return new GetVolumeStatsAnswer(cmd, "Can't get vm disk stats: " + e.getMessage(), null);
        }
    }

    private VolumeStatsEntry getVolumeStat(final LibvirtComputingResource libvirtComputingResource, final Connect conn, final String volumeUuid, final String storeUuid, final StoragePoolType poolType) throws LibvirtException {
        KVMStoragePool sourceKVMPool = libvirtComputingResource.getStoragePoolMgr().getStoragePool(poolType, storeUuid);
        if (sourceKVMPool == null) {
            return null;
        }

        KVMPhysicalDisk sourceKVMVolume = sourceKVMPool.getPhysicalDisk(volumeUuid);
        if (sourceKVMVolume == null) {
            return null;
        }

        VolumeStatsEntry entry = new VolumeStatsEntry(volumeUuid, sourceKVMVolume.getSize(),
                sourceKVMVolume.getVirtualSize());
        entry.setTotalPhysicalSize(retrieveTotalPhysicalSize(volumeUuid, sourceKVMVolume.getPath(), logger));

        return entry;
    }


    private static Long retrieveTotalPhysicalSize(String volumeUuid, String volumePath, Logger logger) {
        logger.trace("Retrieving total physical size for volume: {} with path: {}", volumeUuid, volumePath);
        Long totalPhysicalSize = null;
        try {
            QemuImg qemu = new QemuImg(10000);
            QemuImgFile file = new QemuImgFile(volumePath);
            String info = qemu.infoBackingJson(file);
            if (StringUtils.isBlank(info)) {
                logger.debug("Failed to get qemu info for volume: {} as the output is empty");
                return null;
            }
            totalPhysicalSize = parseTotalActualSize(info, logger);
            if (totalPhysicalSize != null) {
                logger.trace("Total physical size for volume {} is: {}", volumeUuid, totalPhysicalSize);
            }
        } catch (LibvirtException | QemuImgException e) {
            logger.debug("Failed to get qemu info for volume: {} due to: {}", volumeUuid, e.getMessage());
        }
        return totalPhysicalSize;
    }

    private static Long parseTotalActualSize(String json, Logger logger) {
        JsonElement root = JsonParser.parseString(json);

        Long total = null;

        if (root.isJsonArray()) {
            JsonArray arr = root.getAsJsonArray();
            for (JsonElement elem : arr) {
                JsonObject obj = elem.getAsJsonObject();
                if (!obj.has("actual-size")) {
                    continue;
                }
                if (total == null) {
                    total = 0L;
                }
                total += obj.get("actual-size").getAsLong();
            }
        } else if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("actual-size")) {
                total = obj.get("actual-size").getAsLong();
            }
        } else {
            logger.debug("Unexpected JSON format when parsing qemu info: {}", json);
        }

        return total;
    }
}
