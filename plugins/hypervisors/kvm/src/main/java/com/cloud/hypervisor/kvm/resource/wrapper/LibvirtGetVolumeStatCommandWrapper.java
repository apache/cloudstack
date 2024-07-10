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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVolumeStatAnswer;
import com.cloud.agent.api.GetVolumeStatCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles = GetVolumeStatCommand.class)
public final class LibvirtGetVolumeStatCommandWrapper extends CommandWrapper<GetVolumeStatCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final GetVolumeStatCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        try {
            String volumePath = cmd.getVolumePath();
            StoragePoolType poolType = cmd.getPoolType();
            String poolUuid = cmd.getPoolUuid();

            KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(poolType, poolUuid);
            if (primaryPool == null) {
                String msg = "Can't get volume stats as pool details unavailable for volume: " + volumePath + " on the storage pool: " + poolUuid;
                return new GetVolumeStatAnswer(cmd, false, msg);
            }

            KVMPhysicalDisk disk = primaryPool.getPhysicalDisk(volumePath);
            if (disk == null) {
                String msg = "Can't get volume stats as disk details unavailable for volume: " + volumePath + " on the storage pool: " + poolUuid;
                return new GetVolumeStatAnswer(cmd, false, msg);
            }

            return new GetVolumeStatAnswer(cmd, disk.getSize(), disk.getVirtualSize());
        } catch (CloudRuntimeException e) {
            logger.error("Can't get volume stats, due to: " + e.getMessage(), e);
            return new GetVolumeStatAnswer(cmd, false, "Can't get volume stats, due to: " + e.getMessage());
        }
    }
}
