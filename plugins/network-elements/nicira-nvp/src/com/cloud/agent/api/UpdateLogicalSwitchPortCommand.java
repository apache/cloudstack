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

package com.cloud.agent.api;

public class UpdateLogicalSwitchPortCommand extends Command {
    private final String logicalSwitchUuid;
    private final String logicalSwitchPortUuid;
    private final String attachmentUuid;
    private final String ownerName;
    private final String nicName;

    public UpdateLogicalSwitchPortCommand(final String logicalSwitchPortUuid, final String logicalSwitchUuid, final String attachmentUuid, final String ownerName,
            final String nicName) {
        this.logicalSwitchUuid = logicalSwitchUuid;
        this.logicalSwitchPortUuid = logicalSwitchPortUuid;
        this.attachmentUuid = attachmentUuid;
        this.ownerName = ownerName;
        this.nicName = nicName;
    }

    public String getLogicalSwitchUuid() {
        return logicalSwitchUuid;
    }

    public String getLogicalSwitchPortUuid() {
        return logicalSwitchPortUuid;
    }

    public String getAttachmentUuid() {
        return attachmentUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getNicName() {
        return nicName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
