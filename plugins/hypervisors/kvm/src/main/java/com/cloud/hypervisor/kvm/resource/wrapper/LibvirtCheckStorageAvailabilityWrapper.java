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
import com.cloud.agent.api.storage.CheckStorageAvailabilityCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.util.Map;

@ResourceWrapper(handles =  CheckStorageAvailabilityCommand.class)
public class LibvirtCheckStorageAvailabilityWrapper extends CommandWrapper<CheckStorageAvailabilityCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCheckStorageAvailabilityWrapper.class);

    @Override
    public Answer execute(CheckStorageAvailabilityCommand command, LibvirtComputingResource resource) {
        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();
        Map<String, Storage.StoragePoolType> poolsMap = command.getPoolsMap();

        for (String poolUuid : poolsMap.keySet()) {
            Storage.StoragePoolType type = poolsMap.get(poolUuid);
            s_logger.debug("Checking if storage pool " + poolUuid + " (" + type + ") is mounted on this host");
            try {
                KVMStoragePool storagePool = storagePoolMgr.getStoragePool(type, poolUuid);
                if (storagePool == null) {
                    s_logger.info("Storage pool " + poolUuid + " is not available");
                    return new Answer(command, false, "Storage pool " + poolUuid + " not available");
                }
            } catch (CloudRuntimeException e) {
                s_logger.info("Storage pool " + poolUuid + " is not available");
                return new Answer(command, e);
            }
        }
        return new Answer(command);
    }
}
