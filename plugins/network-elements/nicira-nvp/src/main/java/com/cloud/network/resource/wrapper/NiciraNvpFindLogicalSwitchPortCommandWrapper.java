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
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = FindLogicalSwitchPortCommand.class)
public final class NiciraNvpFindLogicalSwitchPortCommandWrapper extends CommandWrapper<FindLogicalSwitchPortCommand, Answer, NiciraNvpResource> {

    @Override
    public Answer execute(final FindLogicalSwitchPortCommand command, final NiciraNvpResource niciraNvpResource) {
        final String logicalSwitchUuid = command.getLogicalSwitchUuid();
        final String logicalSwitchPortUuid = command.getLogicalSwitchPortUuid();

        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        try {
            final List<LogicalSwitchPort> ports = niciraNvpApi.findLogicalSwitchPortsByUuid(logicalSwitchUuid, logicalSwitchPortUuid);
            if (ports.size() == 0) {
                return new FindLogicalSwitchPortAnswer(command, false, "Logical switchport " + logicalSwitchPortUuid + " not found", null);
            } else {
                return new FindLogicalSwitchPortAnswer(command, true, "Logical switchport " + logicalSwitchPortUuid + " found", logicalSwitchPortUuid);
            }
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, FindLogicalSwitchPortAnswer.class, e);
        }
    }
}