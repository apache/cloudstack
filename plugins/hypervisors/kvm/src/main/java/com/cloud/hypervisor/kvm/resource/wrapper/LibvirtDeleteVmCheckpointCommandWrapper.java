//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Map;

import org.apache.cloudstack.backup.DeleteVmCheckpointCommand;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = DeleteVmCheckpointCommand.class)
public class LibvirtDeleteVmCheckpointCommandWrapper extends CommandWrapper<DeleteVmCheckpointCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(DeleteVmCheckpointCommand cmd, LibvirtComputingResource resource) {
        if (cmd.isStoppedVM()) {
            return deleteBitmapsOnDisks(cmd);
        }
        return deleteDomainCheckpoint(cmd);
    }

    private Answer deleteDomainCheckpoint(DeleteVmCheckpointCommand cmd) {
        String vmName = cmd.getVmName();
        String checkpointId = cmd.getCheckpointId();
        String virshCmd = String.format("virsh checkpoint-delete %s %s", vmName, checkpointId);
        Script script = new Script("/bin/bash");
        script.add("-c");
        script.add(virshCmd);
        String result = script.execute();
        if (result != null) {
            return new Answer(cmd, false, "Failed to delete checkpoint: " + result);
        }
        return new Answer(cmd, true, "Checkpoint deleted");
    }

    /**
     * Stopped VM: persistent bitmaps on disk images ({@code qemu-img bitmap --remove}), matching {@link LibvirtStartBackupCommandWrapper} bitmap --add.
     */
    private Answer deleteBitmapsOnDisks(DeleteVmCheckpointCommand cmd) {
        String checkpointId = cmd.getCheckpointId();
        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        if (diskPathUuidMap == null || diskPathUuidMap.isEmpty()) {
            return new Answer(cmd, false, "No disks provided for bitmap removal");
        }
        for (Map.Entry<String, String> entry : diskPathUuidMap.entrySet()) {
            String diskPath = entry.getKey();
            Script script = new Script("qemu-img");
            script.add("bitmap");
            script.add("--remove");
            script.add(diskPath);
            script.add(checkpointId);
            String result = script.execute();
            if (result != null) {
                return new Answer(cmd, false,
                        "Failed to remove bitmap " + checkpointId + " from disk " + diskPath + ": " + result);
            }
        }
        return new Answer(cmd, true, "Checkpoint bitmap removed from disks");
    }
}
