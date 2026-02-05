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

public class StartBackupCommand extends Command {
    private String vmName;
    private String toCheckpointId;
    private String fromCheckpointId;
    private Long fromCheckpointCreateTime;
    private int nbdPort;
    private Map<String, String> diskPathUuidMap;
    private String hostIpAddress;
    private boolean stoppedVM;

    public StartBackupCommand() {
    }

    public StartBackupCommand(String vmName, String toCheckpointId, String fromCheckpointId, Long fromCheckpointCreateTime,
                             int nbdPort, Map<String, String> diskPathUuidMap, String hostIpAddress, boolean stoppedVM) {
        this.vmName = vmName;
        this.toCheckpointId = toCheckpointId;
        this.fromCheckpointId = fromCheckpointId;
        this.fromCheckpointCreateTime = fromCheckpointCreateTime;
        this.nbdPort = nbdPort;
        this.diskPathUuidMap = diskPathUuidMap;
        this.hostIpAddress = hostIpAddress;
        this.stoppedVM = stoppedVM;
    }

    public String getVmName() {
        return vmName;
    }

    public String getToCheckpointId() {
        return toCheckpointId;
    }

    public String getFromCheckpointId() {
        return fromCheckpointId;
    }

    public Long getFromCheckpointCreateTime() {
        return fromCheckpointCreateTime;
    }

    public int getNbdPort() {
        return nbdPort;
    }

    public Map<String, String> getDiskPathUuidMap() {
        return diskPathUuidMap;
    }

    public boolean isIncremental() {
        return fromCheckpointId != null && !fromCheckpointId.isEmpty();
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public boolean isStoppedVM() {
        return stoppedVM;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
