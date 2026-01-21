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
    private Long vmId;
    private String toCheckpointId;
    private String fromCheckpointId;
    private int nbdPort;
    private Map<Long, String> diskVolumePaths;  // volumeId -> path mapping
    private String hostIpAddress;

    public StartBackupCommand() {
    }

    public StartBackupCommand(String vmName, Long vmId, String toCheckpointId, String fromCheckpointId,
                             int nbdPort, Map<Long, String> diskVolumePaths, String hostIpAddress) {
        this.vmName = vmName;
        this.vmId = vmId;
        this.toCheckpointId = toCheckpointId;
        this.fromCheckpointId = fromCheckpointId;
        this.nbdPort = nbdPort;
        this.diskVolumePaths = diskVolumePaths;
        this.hostIpAddress = hostIpAddress;
    }

    public String getVmName() {
        return vmName;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getToCheckpointId() {
        return toCheckpointId;
    }

    public String getFromCheckpointId() {
        return fromCheckpointId;
    }

    public int getNbdPort() {
        return nbdPort;
    }

    public Map<Long, String> getDiskVolumePaths() {
        return diskVolumePaths;
    }

    public boolean isIncremental() {
        return fromCheckpointId != null && !fromCheckpointId.isEmpty();
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
