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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.VifAttachment;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.resource.NiciraNvpUtilities;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CreateLogicalSwitchPortCommand.class)
public final class NiciraNvpCreateLogicalSwitchPortCommandWrapper extends CommandWrapper<CreateLogicalSwitchPortCommand, Answer, NiciraNvpResource> {

    private static final Logger s_logger = Logger.getLogger(NiciraNvpCreateLogicalSwitchPortCommandWrapper.class);

    @Override
    public Answer execute(final CreateLogicalSwitchPortCommand command, final NiciraNvpResource niciraNvpResource) {
        final NiciraNvpUtilities niciraNvpUtilities = niciraNvpResource.getNiciraNvpUtilities();

        final String logicalSwitchUuid = command.getLogicalSwitchUuid();
        final String attachmentUuid = command.getAttachmentUuid();

        try {
            final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

            final LogicalSwitchPort logicalSwitchPort = niciraNvpUtilities.createLogicalSwitchPort(command);
            final LogicalSwitchPort newPort = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, logicalSwitchPort);
            try {
                niciraNvpApi.updateLogicalSwitchPortAttachment(command.getLogicalSwitchUuid(), newPort.getUuid(), new VifAttachment(attachmentUuid));
            } catch (final NiciraNvpApiException ex) {
                s_logger.warn("modifyLogicalSwitchPort failed after switchport was created, removing switchport");
                niciraNvpApi.deleteLogicalSwitchPort(command.getLogicalSwitchUuid(), newPort.getUuid());
                throw ex; // Rethrow the original exception
            }
            return new CreateLogicalSwitchPortAnswer(command, true, "Logical switch port " + newPort.getUuid() + " created", newPort.getUuid());
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, CreateLogicalSwitchPortAnswer.class, e);
        }
    }
}
