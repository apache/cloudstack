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

import static com.cloud.network.resource.NiciraNvpResource.NAME_MAX_LEN;
import static com.cloud.network.resource.NiciraNvpResource.NUM_RETRIES;

import java.util.ArrayList;
import java.util.List;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConfigureSharedNetworkVlanIdAnswer;
import com.cloud.agent.api.ConfigureSharedNetworkVlanIdCommand;
import com.cloud.network.nicira.L2GatewayAttachment;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.rest.HttpStatusCodeHelper;

@ResourceWrapper(handles =  ConfigureSharedNetworkVlanIdCommand.class)
public class NiciraNvpConfigureSharedNetworkVlanIdCommandWrapper extends CommandWrapper<ConfigureSharedNetworkVlanIdCommand, Answer, NiciraNvpResource>{


    @Override
    public Answer execute(ConfigureSharedNetworkVlanIdCommand command, NiciraNvpResource niciraNvpResource) {
        final String logicalSwitchUuid = command.getLogicalSwitchUuid();
        final String l2GatewayServiceUuid = command.getL2GatewayServiceUuid();
        long vlanId = command.getVlanId();
        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", command.getOwnerName()));
        final long networkId = command.getNetworkId();

        logger.debug("Connecting Logical Switch " + logicalSwitchUuid + " to L2 Gateway Service " + l2GatewayServiceUuid + ", vlan id " + vlanId + " network " + networkId);
        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        logger.debug("Creating Logical Switch Port in Logical Switch " + logicalSwitchUuid);
        LogicalSwitchPort lSwitchPort = null;
        try {
            lSwitchPort = new LogicalSwitchPort();
            lSwitchPort.setAdminStatusEnabled(true);
            lSwitchPort.setDisplayName(niciraNvpResource.truncate(networkId + "-l2Gateway-port", NAME_MAX_LEN));
            lSwitchPort.setTags(tags);
            lSwitchPort = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, lSwitchPort);
        }
        catch (NiciraNvpApiException e){
            logger.warn("Could not create Logical Switch Port on Logical Switch " + logicalSwitchUuid + " due to: " + e.getMessage() + ", retrying");
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully created in Logical Switch " + logicalSwitchUuid);

        logger.debug("Attaching Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") on VLAN " + command.getVlanId() + " using L2GatewayAttachment");
        try {
            final L2GatewayAttachment attachment = new L2GatewayAttachment(l2GatewayServiceUuid);
            if (command.getVlanId() != 0) {
                attachment.setVlanId(command.getVlanId());
            }
            niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, lSwitchPort.getUuid(), attachment);
        }
        catch (NiciraNvpApiException e){
            logger.warn("Could not attach Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") to Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") due to: " + e.getMessage() + ", errorCode: " + e.getErrorCode());
            cleanup(logicalSwitchUuid, lSwitchPort, niciraNvpApi);
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully attached on VLAN " + command.getVlanId() + " using L2GatewayAttachment");

        logger.debug("Successfully connected Logical Switch " + logicalSwitchUuid + " to L2 Gateway Service " + l2GatewayServiceUuid + ", vlan id " + vlanId + ", network " + networkId + ", through Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ")");
        return new ConfigureSharedNetworkVlanIdAnswer(command, true, "OK");
    }

    private void cleanup(String logicalSwitchUuid, LogicalSwitchPort lSwitchPort, NiciraNvpApi niciraNvpApi) {
        logger.warn("Deleting previously created Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") from Logical Switch " + logicalSwitchUuid);
        try {
            niciraNvpApi.deleteLogicalSwitchPort(logicalSwitchUuid, lSwitchPort.getUuid());
        } catch (NiciraNvpApiException exceptionDeleteLSwitchPort) {
            logger.error("Error while deleting Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") from Logical Switch " + logicalSwitchUuid + " due to: " + exceptionDeleteLSwitchPort.getMessage());
        }
        logger.warn("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully deteled");
    }

    private Answer handleException(NiciraNvpApiException e, ConfigureSharedNetworkVlanIdCommand command, NiciraNvpResource niciraNvpResource) {
        if (HttpStatusCodeHelper.isConflict(e.getErrorCode())){
            logger.warn("There's been a conflict in NSX side, aborting implementation");
            return new ConfigureSharedNetworkVlanIdAnswer(command, false, "FAILED: There's been a conflict in NSX side");
        }
        else {
            logger.warn("Error code: " + e.getErrorCode() + ", retrying");
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, ConfigureSharedNetworkVlanIdAnswer.class, e);
        }
    }

}
