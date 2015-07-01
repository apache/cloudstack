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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  DeleteLogicalSwitchCommand.class)
public final class NiciraNvpDeleteLogicalSwitchCommandWrapper extends CommandWrapper<DeleteLogicalSwitchCommand, Answer, NiciraNvpResource> {

    @Override
    public Answer execute(final DeleteLogicalSwitchCommand command, final NiciraNvpResource niciraNvpResource) {
        try {
            final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();
            niciraNvpApi.deleteLogicalSwitch(command.getLogicalSwitchUuid());
            return new DeleteLogicalSwitchAnswer(command, true, "Logicalswitch " + command.getLogicalSwitchUuid() + " deleted");
        } catch (final NiciraNvpApiException e) {
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, DeleteLogicalSwitchAnswer.class, e);
        }
    }
}