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

package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.VifAttachment;

public class NiciraNvpUtilities {

    private static NiciraNvpUtilities instance;

    static {
        instance = new NiciraNvpUtilities();
    }

    private NiciraNvpUtilities() {
    }

    public static NiciraNvpUtilities getInstance() {
        return instance;
    }

    public LogicalSwitch createLogicalSwitch() {
        final LogicalSwitch logicalSwitch = new LogicalSwitch();
        return logicalSwitch;
    }

    public LogicalSwitchPort createLogicalSwitchPort(final CreateLogicalSwitchPortCommand command) {
        final String attachmentUuid = command.getAttachmentUuid();

        // Tags set to scope cs_account and account name
        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", command.getOwnerName()));

        final LogicalSwitchPort logicalSwitchPort = new LogicalSwitchPort(attachmentUuid, tags, true);
        return logicalSwitchPort;
    }

    public VifAttachment createVifAttachment(final String attachmentUuid) {
        return new VifAttachment(attachmentUuid);
    }
}