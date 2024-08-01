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
import com.cloud.utils.script.Script;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.TakeBackupCommand;

import java.io.File;

@ResourceWrapper(handles = TakeBackupCommand.class)
public class LibvirtTakeBackupCommandWrapper extends CommandWrapper<TakeBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(TakeBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final String backupStoragePath = command.getBackupStoragePath();
        final String backupFolder = command.getBackupPath();
        Script cmd = new Script(libvirtComputingResource.getNasBackupPath(), libvirtComputingResource.getCmdsTimeout(), logger);
        cmd.add("-b", vmName);
        cmd.add("-s", backupStoragePath);
        cmd.add("-p", String.format("%s%s%s", vmName, File.separator, backupFolder));
        String result = cmd.execute();
        if (result == null) {
            logger.debug("Failed to take VM backup: " + result);
            return new BackupAnswer(command, false, result);
        }
        BackupAnswer answer = new BackupAnswer(command, true, null);
        answer.setSize(Long.valueOf(result));
        return answer;
    }
}
