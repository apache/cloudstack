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
import com.cloud.agent.api.FindL2GatewayServiceAnswer;
import com.cloud.agent.api.FindL2GatewayServiceCommand;
import com.cloud.network.nicira.GatewayServiceConfig;
import com.cloud.network.nicira.L2GatewayServiceConfig;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  FindL2GatewayServiceCommand.class)
public class NiciraNvpFindL2GatewayServiceCommandWrapper extends CommandWrapper<FindL2GatewayServiceCommand, Answer, NiciraNvpResource> {


    @Override
    public Answer execute(FindL2GatewayServiceCommand command, NiciraNvpResource niciraNvpResource) {
        final GatewayServiceConfig config = command.getGatewayServiceConfig();
        final String uuid = config.getUuid();
        final String type = config.getType();
        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        logger.info("Looking for L2 Gateway Service " + uuid + " of type " + type);

        try {
            List<L2GatewayServiceConfig> lstGW = niciraNvpApi.findL2GatewayServiceByUuidAndType(uuid, type);
            if (lstGW.size() == 0) {
                return new FindL2GatewayServiceAnswer(command, false, "L2 Gateway Service not found", null);
            } else {
                return new FindL2GatewayServiceAnswer(command, true, "L2 Gateway Service " + lstGW.get(0).getDisplayName()+ " found", lstGW.get(0).getUuid());
            }
        } catch (NiciraNvpApiException e) {
            logger.error("Error finding Gateway Service due to: " + e.getMessage());
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, FindL2GatewayServiceAnswer.class, e);
        }
    }

}
