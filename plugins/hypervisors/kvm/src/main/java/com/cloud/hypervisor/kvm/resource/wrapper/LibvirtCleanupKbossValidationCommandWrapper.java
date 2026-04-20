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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.backup.CleanupKbossValidationCommand;

@ResourceWrapper(handles = CleanupKbossValidationCommand.class)
public class LibvirtCleanupKbossValidationCommandWrapper extends CommandWrapper<CleanupKbossValidationCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(CleanupKbossValidationCommand command, LibvirtComputingResource serverResource) {
        KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        cleanupSecondaryStorages(command, storagePoolMgr);
        return new Answer(command);
    }

    /**
     * The objective of this command is to remove the secondary storage references after the validation VM was stopped.
     * Since the `getStoragePoolByURI` and `deleteStoragePool` have a reference counter, where the first method increases the count and the second one decreases the count,
     * we must call the deleteStoragePool twice so that the command is count negative.
     * */
    private void cleanupSecondaryStorages(CleanupKbossValidationCommand command, KVMStoragePoolManager storagePoolMgr) {
        logger.info("Cleaning up secondary storage references after backup validation process using VM [{}].", command.getVmName());
        for (String secondaryUrl : command.getSecondaryStorages()) {
            logger.debug("Cleaning up secondary storage reference for secondary at [{}].", secondaryUrl);
            KVMStoragePool secondary = storagePoolMgr.getStoragePoolByURI(secondaryUrl);
            storagePoolMgr.deleteStoragePool(secondary.getType(), secondary.getUuid());
            storagePoolMgr.deleteStoragePool(secondary.getType(), secondary.getUuid());
        }
    }
}
