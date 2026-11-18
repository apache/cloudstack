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

import java.util.List;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FindLogicalRouterPortAnswer;
import com.cloud.agent.api.FindLogicalRouterPortCommand;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  FindLogicalRouterPortCommand.class)
public class NiciraNvpFindLogicalRouterPortCommandWrapper extends CommandWrapper<FindLogicalRouterPortCommand, Answer, NiciraNvpResource> {


    @Override
    public Answer execute(FindLogicalRouterPortCommand command, NiciraNvpResource niciraNvpResource) {
        final String logicalRouterUuid = command.getLogicalRouterUuid();
        final String attachmentLswitchUuid = command.getAttachmentLswitchUuid();
        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        logger.debug("Finding Logical Router Port in Logical Router " + logicalRouterUuid + " and attachmentLSwitchUuid " + attachmentLswitchUuid);

        try{
            List<LogicalRouterPort> lRouterPorts = niciraNvpApi.findLogicalRouterPortByAttachmentLSwitchUuid(logicalRouterUuid, attachmentLswitchUuid);
            if (lRouterPorts.size() == 0) {
                return new FindLogicalRouterPortAnswer(command, false, "Logical Router Port not found", null);
            } else {
                return new FindLogicalRouterPortAnswer(command, true, "Logical Router Port found", lRouterPorts.get(0).getUuid());
            }
        }
        catch (NiciraNvpApiException e){
            logger.error("Error finding Logical Router Port due to: " + e.getMessage());
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, FindLogicalRouterPortAnswer.class, e);
        }
    }

}
