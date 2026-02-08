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

public class CreateImageTransferCommand extends Command {
    private String transferId;
    private String hostIpAddress;
    private String exportName;
    private int nbdPort;
    private String direction;
    private String checkpointId;
    private String file;
    private ImageTransfer.Backend backend;

    public CreateImageTransferCommand() {
    }

    private CreateImageTransferCommand(String transferId, String hostIpAddress, String direction) {
        this.transferId = transferId;
        this.hostIpAddress = hostIpAddress;
        this.direction = direction;
    }

    public CreateImageTransferCommand(String transferId, String hostIpAddress, String direction, String exportName, int nbdPort, String checkpointId) {
        this(transferId, hostIpAddress, direction);
        this.backend = ImageTransfer.Backend.nbd;
        this.exportName = exportName;
        this.nbdPort = nbdPort;
        this.checkpointId = checkpointId;
    }

    public CreateImageTransferCommand(String transferId, String hostIpAddress, String direction, String file) {
        this(transferId, hostIpAddress, direction);
        if (direction == ImageTransfer.Direction.download.toString()) {
            throw new IllegalArgumentException("File backend is only supported for upload");
        }
        this.backend = ImageTransfer.Backend.file;
        this.file = file;
    }

    public String getExportName() {
        return exportName;
    }

    public int getNbdPort() {
        return nbdPort;
    }

    public String getFile() {
        return file;
    }

    public ImageTransfer.Backend getBackend() {
        return backend;
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

    public String getDirection() {
        return direction;
    }

    public String getCheckpointId() {
        return checkpointId;
    }
}
