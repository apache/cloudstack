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

package com.cloud.network.resource.wrapper;

import static com.cloud.network.resource.NiciraNvpResource.NUM_RETRIES;

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.VifAttachment;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.resource.NiciraNvpUtilities;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  UpdateLogicalSwitchPortCommand.class)
public final class NiciraNvpUpdateLogicalSwitchPortCommandWrapper extends CommandWrapper<UpdateLogicalSwitchPortCommand, Answer, NiciraNvpResource> {

    @Override
    public Answer execute(final UpdateLogicalSwitchPortCommand command, final NiciraNvpResource niciraNvpResource) {
        final NiciraNvpUtilities niciraNvpUtilities = niciraNvpResource.getNiciraNvpUtilities();

        final String logicalSwitchUuid = command.getLogicalSwitchUuid();
        final String logicalSwitchPortUuid = command.getLogicalSwitchPortUuid();
        final String attachmentUuid = command.getAttachmentUuid();

        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        try {
            // Tags set to scope cs_account and account name
            final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account", command.getOwnerName()));

            final VifAttachment vifAttachment = niciraNvpUtilities.createVifAttachment(attachmentUuid);

            niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, logicalSwitchPortUuid, vifAttachment);
            return new UpdateLogicalSwitchPortAnswer(command, true, "Attachment for  " + logicalSwitchPortUuid + " updated", logicalSwitchPortUuid);
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, UpdateLogicalSwitchPortAnswer.class, e);
        }
    }
}
