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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  ModifyTargetsCommand.class)
public final class LibvirtModifyTargetsCommandWrapper extends CommandWrapper<ModifyTargetsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final ModifyTargetsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        List<Map<String, String>> targets = command.getTargets();

        // When attempting to connect to one or more targets, place the successfully connected path into this List.
        List<String> connectedPaths = new ArrayList<>(targets.size());

        for (Map<String, String> target : targets) {
            StoragePoolType storagePoolType = StoragePoolType.valueOf(target.get(ModifyTargetsCommand.STORAGE_TYPE));
            String storageUuid = target.get(ModifyTargetsCommand.STORAGE_UUID);
            String path = target.get(ModifyTargetsCommand.IQN);

            if (command.getAdd()) {
                if (storagePoolMgr.connectPhysicalDisk(storagePoolType, storageUuid, path, target)) {
                    KVMPhysicalDisk kvmPhysicalDisk = storagePoolMgr.getPhysicalDisk(storagePoolType, storageUuid, path);

                    connectedPaths.add(kvmPhysicalDisk.getPath());
                }
                else {
                    throw new CloudRuntimeException("Unable to connect to the following target: " + path);
                }
            }
            else {
                if (!storagePoolMgr.disconnectPhysicalDisk(storagePoolType, storageUuid, path)) {
                    throw new CloudRuntimeException("Unable to disconnect from the following target: " + path);
                }
            }
        }

        ModifyTargetsAnswer modifyTargetsAnswer =  new ModifyTargetsAnswer();

        modifyTargetsAnswer.setConnectedPaths(connectedPaths);

        return modifyTargetsAnswer;
    }
}
