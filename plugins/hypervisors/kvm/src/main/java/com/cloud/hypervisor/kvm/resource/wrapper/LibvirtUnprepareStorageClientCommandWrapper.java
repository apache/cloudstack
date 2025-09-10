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
import com.cloud.agent.api.UnprepareStorageClientAnswer;
import com.cloud.agent.api.UnprepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;

@ResourceWrapper(handles = UnprepareStorageClientCommand.class)
public class LibvirtUnprepareStorageClientCommandWrapper extends CommandWrapper<UnprepareStorageClientCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(UnprepareStorageClientCommand cmd, LibvirtComputingResource libvirtComputingResource) {
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        Pair<Boolean, String> unprepareStorageClientResult = storagePoolMgr.unprepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid());
        if (!unprepareStorageClientResult.first()) {
            String msg = unprepareStorageClientResult.second();
            logger.debug("Couldn't unprepare storage client, due to: " + msg);
            return new UnprepareStorageClientAnswer(cmd, false, msg);
        }
        return new UnprepareStorageClientAnswer(cmd, true);
    }
}
