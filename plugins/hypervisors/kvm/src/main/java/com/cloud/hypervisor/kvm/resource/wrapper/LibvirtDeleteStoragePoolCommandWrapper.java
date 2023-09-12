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
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  DeleteStoragePoolCommand.class)
public final class LibvirtDeleteStoragePoolCommandWrapper extends CommandWrapper<DeleteStoragePoolCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(final DeleteStoragePoolCommand command, final LibvirtComputingResource libvirtComputingResource) {
        try {
            // if getRemoveDatastore() is true, then we are dealing with managed storage and can skip the delete logic here
            if (!command.getRemoveDatastore()) {
                final StorageFilerTO pool = command.getPool();
                final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

                storagePoolMgr.deleteStoragePool(pool.getType(), pool.getUuid());
            }

            return new Answer(command);
        } catch (final CloudRuntimeException e) {
            return new Answer(command, false, e.toString());
        }
    }
}
