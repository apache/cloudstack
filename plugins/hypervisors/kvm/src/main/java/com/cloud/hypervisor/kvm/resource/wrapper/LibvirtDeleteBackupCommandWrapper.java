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
import org.apache.cloudstack.backup.DeleteBackupCommand;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = DeleteBackupCommand.class)
public class LibvirtDeleteBackupCommandWrapper extends CommandWrapper<DeleteBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(DeleteBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String backupPath = command.getBackupPath();
        final String backupRepoType = command.getBackupRepoType();
        final String backupRepoAddress = command.getBackupRepoAddress();
        final String mountOptions = command.getMountOptions();

        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{
                libvirtComputingResource.getNasBackupPath(),
                "-o", "delete",
                "-t", backupRepoType,
                "-s", backupRepoAddress,
                "-m", mountOptions,
                "-p", backupPath
        });

        Pair<Integer, String> result = Script.executePipedCommands(commands, libvirtComputingResource.getCmdsTimeout());

        logger.debug(String.format("Backup delete result: %s , exit code: %s", result.second(), result.first()));

        if (result.first() != 0) {
            logger.debug(String.format("Failed to delete VM backup: %s", result.second()));
            return new BackupAnswer(command, false, result.second());
        }
        return new BackupAnswer(command, true, null);
    }
}
