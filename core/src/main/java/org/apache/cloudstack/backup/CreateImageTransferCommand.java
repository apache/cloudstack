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
    private String exportName;
    private String socket;
    private String direction;
    private String checkpointId;
    private String file;
    private ImageTransfer.Backend backend;
    private int idleTimeoutSeconds;

    public CreateImageTransferCommand() {
    }

    private CreateImageTransferCommand(String transferId, String direction, String socket, int idleTimeoutSeconds) {
        this.transferId = transferId;
        this.direction = direction;
        this.socket = socket;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public CreateImageTransferCommand(String transferId, String direction, String exportName, String socket, String checkpointId, int idleTimeoutSeconds) {
        this(transferId, direction, socket, idleTimeoutSeconds);
        this.backend = ImageTransfer.Backend.nbd;
        this.exportName = exportName;
        this.checkpointId = checkpointId;
    }

    public CreateImageTransferCommand(String transferId, String direction, String socket, String file, int idleTimeoutSeconds) {
        this(transferId, direction, socket, idleTimeoutSeconds);
        if (direction == ImageTransfer.Direction.download.toString()) {
            throw new IllegalArgumentException("File backend is only supported for upload");
        }
        this.backend = ImageTransfer.Backend.file;
        this.file = file;
    }

    public String getExportName() {
        return exportName;
    }

    public String getSocket() {
        return socket;
    }

    public String getFile() {
        return file;
    }

    public ImageTransfer.Backend getBackend() {
        return backend;
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

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }
}
