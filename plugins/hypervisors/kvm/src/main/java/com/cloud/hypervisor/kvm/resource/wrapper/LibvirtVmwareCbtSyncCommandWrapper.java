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

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = VmwareCbtSyncCommand.class)
public class LibvirtVmwareCbtSyncCommandWrapper extends CommandWrapper<VmwareCbtSyncCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(VmwareCbtSyncCommand cmd, LibvirtComputingResource serverResource) {
        if (!serverResource.hostSupportsVmwareCbtMigration()) {
            String msg = String.format("Cannot synchronize VMware CBT migration %s on host %s. VDDK, qemu-img and qemu-nbd are required.",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    0, 0, 0, false, null);
        }

        long startTime = System.currentTimeMillis();
        List<VmwareCbtChangedBlockRangeTO> changedBlocks = cmd.getChangedBlocks();
        if (changedBlocks == null || changedBlocks.isEmpty()) {
            long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
            String msg = String.format("VMware CBT cycle %s for migration %s completed with no changed blocks.",
                    cmd.getCycleNumber(), cmd.getMigrationUuid());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    0, 0, durationSeconds, true, null);
        }

        long changedBytes = getChangedBytes(changedBlocks);
        String msg = String.format("VMware CBT cycle %s for migration %s received %s changed block range(s) " +
                        "totaling %s bytes, but changed-block copy is not implemented yet.",
                cmd.getCycleNumber(), cmd.getMigrationUuid(), changedBlocks.size(), changedBytes);
        logger.info(msg);
        return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                changedBytes, 0, 0, false, null);
    }

    private long getChangedBytes(List<VmwareCbtChangedBlockRangeTO> changedBlocks) {
        long changedBytes = 0;
        for (VmwareCbtChangedBlockRangeTO changedBlock : changedBlocks) {
            changedBytes += changedBlock.getLength();
        }
        return changedBytes;
    }
}
