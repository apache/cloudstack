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

import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareStorageClientAnswer;
import com.cloud.agent.api.PrepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Ternary;

@ResourceWrapper(handles = PrepareStorageClientCommand.class)
public class LibvirtPrepareStorageClientCommandWrapper extends CommandWrapper<PrepareStorageClientCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(PrepareStorageClientCommand cmd, LibvirtComputingResource libvirtComputingResource) {
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        Ternary<Boolean, Map<String, String>, String> prepareStorageClientResult = storagePoolMgr.prepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid(), cmd.getDetails());
        if (!prepareStorageClientResult.first()) {
            String msg = prepareStorageClientResult.third();
            logger.debug("Unable to prepare storage client, due to: " + msg);
            return new PrepareStorageClientAnswer(cmd, false, msg);
        }
        Map<String, String> details = prepareStorageClientResult.second();
        return new PrepareStorageClientAnswer(cmd, true, details);
    }
}
