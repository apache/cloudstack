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
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.RebaseBackupCommand;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = RebaseBackupCommand.class)
public class LibvirtRebaseBackupCommandWrapper extends CommandWrapper<RebaseBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(RebaseBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{
                libvirtComputingResource.getNasBackupPath(),
                "-o", "rebase",
                "-t", command.getBackupRepoType(),
                "-s", command.getBackupRepoAddress(),
                "-m", command.getMountOptions(),
                "--rebase-target", command.getTargetPath(),
                "--rebase-new-backing", command.getNewBackingPath()
        });

        Pair<Integer, String> result = Script.executePipedCommands(commands, libvirtComputingResource.getCmdsTimeout());
        logger.debug("Backup rebase result: {} , exit code: {}", result.second(), result.first());

        if (result.first() != 0) {
            logger.warn("Failed to rebase backup file {} onto {}: {}",
                    command.getTargetPath(), command.getNewBackingPath(), result.second());
            return new BackupAnswer(command, false, result.second());
        }
        return new BackupAnswer(command, true, null);
    }
}
