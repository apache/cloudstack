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
import org.apache.cloudstack.backup.TakeBackupCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = TakeBackupCommand.class)
public class LibvirtTakeBackupCommandWrapper extends CommandWrapper<TakeBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(TakeBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final String backupStoragePath = command.getBackupStoragePath();
        final String backupFolder = command.getBackupPath();

        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{libvirtComputingResource.getNasBackupPath(),
                "-b", vmName,
                "-s", backupStoragePath,
                "-p", String.format("%s%s%s", vmName, File.separator, backupFolder) });

        Pair<Integer, String> result = Script.executePipedCommands(commands, libvirtComputingResource.getCmdsTimeout());

        if (result.first() != 0) {
            logger.debug("Failed to take VM backup: " + result.second());
            return new BackupAnswer(command, false, result.second());
        }

        BackupAnswer answer = new BackupAnswer(command, true, null);
        answer.setSize(Long.valueOf(result.second()));
        return answer;
    }
}
