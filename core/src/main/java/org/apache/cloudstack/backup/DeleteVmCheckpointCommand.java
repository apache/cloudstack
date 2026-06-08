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

public class DeleteVmCheckpointCommand extends Command {
    private String vmName;
    private String checkpointId;
    private Map<String, String> diskPathUuidMap;
    private boolean stoppedVM;

    public DeleteVmCheckpointCommand() {
    }

    public DeleteVmCheckpointCommand(String vmName, String checkpointId, Map<String, String> diskPathUuidMap, boolean stoppedVM) {
        this.vmName = vmName;
        this.checkpointId = checkpointId;
        this.diskPathUuidMap = diskPathUuidMap;
        this.stoppedVM = stoppedVM;
    }

    public String getVmName() {
        return vmName;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public Map<String, String> getDiskPathUuidMap() {
        return diskPathUuidMap;
    }

    public boolean isStoppedVM() {
        return stoppedVM;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
