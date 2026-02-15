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

import com.cloud.agent.api.Command;

public class StartNBDServerCommand extends Command {
    private String transferId;
    private String hostIpAddress;
    private String exportName;
    private String volumePath;
    private String socket;
    private String direction;

    public StartNBDServerCommand() {
    }

    protected StartNBDServerCommand(String transferId, String hostIpAddress, String exportName, String volumePath, String direction) {
        this.transferId = transferId;
        this.hostIpAddress = hostIpAddress;
        this.exportName = exportName;
        this.volumePath = volumePath;
        this.direction = direction;
    }

    public StartNBDServerCommand(String transferId, String hostIpAddress, String exportName, String volumePath, String socket, String direction) {
        this(transferId, hostIpAddress, exportName, volumePath, direction);
        this.socket = socket;
    }

    public String getExportName() {
        return exportName;
    }

    public String getSocket() {
        return socket;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public String getTransferId() {
        return transferId;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public String getDirection() {
        return direction;
    }
}
