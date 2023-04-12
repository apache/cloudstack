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

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.template.TemplateProp;

@ResourceWrapper(handles =  ModifyStoragePoolCommand.class)
public final class LibvirtModifyStoragePoolCommandWrapper extends CommandWrapper<ModifyStoragePoolCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final ModifyStoragePoolCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        final KVMStoragePool storagepool =
                storagePoolMgr.createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                        .getUserInfo(), command.getPool().getType(), command.getDetails());
        if (storagepool == null) {
            return new Answer(command, false, " Failed to create storage pool");
        }

        final Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
        final ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(command, storagepool.getCapacity(), storagepool.getAvailable(), tInfo, storagepool.getDetails());

        return answer;
    }
}
