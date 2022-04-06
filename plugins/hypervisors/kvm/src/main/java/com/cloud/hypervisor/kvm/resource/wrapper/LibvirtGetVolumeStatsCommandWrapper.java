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

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.VolumeStatsEntry;

@ResourceWrapper(handles = GetVolumeStatsCommand.class)
public final class LibvirtGetVolumeStatsCommandWrapper extends CommandWrapper<GetVolumeStatsCommand, Answer, LibvirtComputingResource> {
    private static final Logger s_logger = Logger.getLogger(LibvirtGetVmDiskStatsCommandWrapper.class);

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

        return new VolumeStatsEntry(volumeUuid, sourceKVMVolume.getSize(), sourceKVMVolume.getVirtualSize());
    }
}
