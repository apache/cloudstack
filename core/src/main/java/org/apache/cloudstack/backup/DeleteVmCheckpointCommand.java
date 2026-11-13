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

package org.apache.cloudstack.backup;

import java.util.Map;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;

public class DeleteVmCheckpointCommand extends Command {
    private String vmName;
    private String checkpointId;
    private Map<String, String> diskPathUuidMap;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map<String, byte[]> diskPathPassphraseMap;
    private boolean stoppedVM;
    private Long checkpointCreateTime;

    public DeleteVmCheckpointCommand() {
    }

    public DeleteVmCheckpointCommand(String vmName, String checkpointId, Map<String, String> diskPathUuidMap, boolean stoppedVM) {
        this(vmName, checkpointId, diskPathUuidMap, null, stoppedVM, null);
    }

    public DeleteVmCheckpointCommand(String vmName, String checkpointId, Map<String, String> diskPathUuidMap,
                                     Map<String, byte[]> diskPathPassphraseMap, boolean stoppedVM, Long checkpointCreateTime) {
        this.vmName = vmName;
        this.checkpointId = checkpointId;
        this.diskPathUuidMap = diskPathUuidMap;
        this.diskPathPassphraseMap = diskPathPassphraseMap;
        this.stoppedVM = stoppedVM;
        this.checkpointCreateTime = checkpointCreateTime;
    }

    public String getVmName() {
        return vmName;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    // Creation time of the checkpoint on the host in epoch seconds, needed to redefine it in libvirt.
    public Long getCheckpointCreateTime() {
        return checkpointCreateTime;
    }

    public Map<String, String> getDiskPathUuidMap() {
        return diskPathUuidMap;
    }

    public Map<String, byte[]> getDiskPathPassphraseMap() {
        return diskPathPassphraseMap;
    }

    public boolean isStoppedVM() {
        return stoppedVM;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
