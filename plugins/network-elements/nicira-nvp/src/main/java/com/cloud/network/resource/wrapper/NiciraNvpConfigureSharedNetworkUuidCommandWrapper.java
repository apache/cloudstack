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
import com.cloud.agent.api.ConfigureSharedNetworkUuidAnswer;
import com.cloud.agent.api.ConfigureSharedNetworkUuidCommand;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.PatchAttachment;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.utils.CommandRetryUtility;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.rest.HttpStatusCodeHelper;

@ResourceWrapper(handles =  ConfigureSharedNetworkUuidCommand.class)
public final class NiciraNvpConfigureSharedNetworkUuidCommandWrapper extends CommandWrapper<ConfigureSharedNetworkUuidCommand, Answer, NiciraNvpResource>{


    @Override
    public Answer execute(ConfigureSharedNetworkUuidCommand command, NiciraNvpResource niciraNvpResource) {
        final String logicalRouterUuid = command.getLogicalRouterUuid();
        final String logicalSwitchUuid = command.getLogicalSwitchUuid();
        final String portIpAddress = command.getPortIpAddress();
        final List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account", command.getOwnerName()));
        final long networkId = command.getNetworkId();

        final NiciraNvpApi niciraNvpApi = niciraNvpResource.getNiciraNvpApi();

        logger.debug("Attaching Logical Switch " + logicalSwitchUuid + " on Logical Router " + logicalRouterUuid + " for Shared Network " + networkId);

        //Step 1: Get lSwitch displayName
        logger.info("Looking for Logical Switch " + logicalSwitchUuid + " display name");
        String logicalSwitchDisplayName;
        try{
            List<LogicalSwitch> lSwitchList = niciraNvpApi.findLogicalSwitch(logicalSwitchUuid);
            if (lSwitchList != null){
                if (lSwitchList.size() == 1){
                    logicalSwitchDisplayName = lSwitchList.get(0).getDisplayName();
                }
                else {
                    logger.error("More than one Logical Switch found with uuid " + logicalSwitchUuid);
                    throw new CloudRuntimeException("More than one Logical Switch found with uuid=" + logicalSwitchUuid);
                }
            }
            else {
                logger.error("Logical Switch " + logicalSwitchUuid + " not found");
                throw new CloudRuntimeException("Logical Switch " + logicalSwitchUuid + " not found");
            }
        }
        catch (NiciraNvpApiException e){
            logger.warn("Logical Switch " + logicalSwitchUuid + " not found, retrying");
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, ConfigureSharedNetworkUuidAnswer.class, e);
        }
        catch (CloudRuntimeException e){
            logger.info("Shared network UUID vlan id failed due to : " + e.getMessage());
            return new ConfigureSharedNetworkUuidAnswer(command, false, e.getMessage());
        }
        logger.info("Found display name " + logicalSwitchDisplayName + " for Logical Switch " + logicalSwitchUuid);


        //Step 2: Create lRouterPort
        logger.debug("Creating Logical Router Port in Logical Router " + logicalRouterUuid);
        LogicalRouterPort lRouterPort = null;
        try {
            lRouterPort = new LogicalRouterPort();
            lRouterPort.setAdminStatusEnabled(true);
            lRouterPort.setDisplayName(niciraNvpResource.truncate(logicalSwitchDisplayName + "-uplink", NAME_MAX_LEN));
            lRouterPort.setTags(tags);
            final List<String> ipAddresses = new ArrayList<String>();
            ipAddresses.add(portIpAddress);
            lRouterPort.setIpAddresses(ipAddresses);
            lRouterPort = niciraNvpApi.createLogicalRouterPort(logicalRouterUuid, lRouterPort);
        }
        catch (NiciraNvpApiException e){
            logger.warn("Could not create Logical Router Port on Logical Router " + logicalRouterUuid + " due to: " + e.getMessage() + ", retrying");
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") successfully created in Logical Router " + logicalRouterUuid);


        //Step 3: Create lSwitchPort
        logger.debug("Creating Logical Switch Port in Logical Switch " + logicalSwitchUuid + " (" + logicalSwitchDisplayName + ")");
        LogicalSwitchPort lSwitchPort = null;
        try {
            lSwitchPort = new LogicalSwitchPort(niciraNvpResource.truncate("lrouter-uplink", NAME_MAX_LEN), tags, true);
            lSwitchPort = niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, lSwitchPort);
        }
        catch (NiciraNvpApiException e){
            logger.warn("Could not create Logical Switch Port on Logical Switch " + logicalSwitchUuid + " (" + logicalSwitchDisplayName + ")  due to: " + e.getMessage());
            cleanupLRouterPort(logicalRouterUuid, lRouterPort, niciraNvpApi);
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully created in Logical Switch " + logicalSwitchUuid + " (" + logicalSwitchDisplayName + ")");


        //Step 4: Attach lRouterPort to lSwitchPort with a PatchAttachment
        logger.debug("Attaching Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") to Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") with a PatchAttachment");
        try {
            niciraNvpApi.updateLogicalRouterPortAttachment(logicalRouterUuid, lRouterPort.getUuid(), new PatchAttachment(lSwitchPort.getUuid()));
        }
        catch (NiciraNvpApiException e) {
            logger.warn("Could not attach Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") to Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") due to: " + e.getMessage() + ", retrying");
            cleanupLRouterPort(logicalRouterUuid, lRouterPort, niciraNvpApi);
            cleanupLSwitchPort(logicalSwitchUuid, lSwitchPort, niciraNvpApi);
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") successfully attached to Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") with a PatchAttachment");


        //Step 5: Attach lSwitchPort to lRouterPort with a PatchAttachment
        logger.debug("Attaching Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") to Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") with a PatchAttachment");
        try {
            niciraNvpApi.updateLogicalSwitchPortAttachment(logicalSwitchUuid, lSwitchPort.getUuid(), new PatchAttachment(lRouterPort.getUuid()));
        }
        catch (NiciraNvpApiException e){
            logger.warn("Could not attach Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") to Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") due to: " + e.getMessage() + ", retrying");
            cleanupLRouterPort(logicalRouterUuid, lRouterPort, niciraNvpApi);
            cleanupLSwitchPort(logicalSwitchUuid, lSwitchPort, niciraNvpApi);
            return handleException(e, command, niciraNvpResource);
        }
        logger.debug("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully attached to Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") with a PatchAttachment");

        logger.info("Successfully attached Logical Switch " + logicalSwitchUuid + " on Logical Router " + logicalRouterUuid + " for Shared Network " + networkId);
        return new ConfigureSharedNetworkUuidAnswer(command, true, "OK");
    }

    private void cleanupLSwitchPort(String logicalSwitchUuid, LogicalSwitchPort lSwitchPort, NiciraNvpApi niciraNvpApi) {
        logger.warn("Deleting previously created Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") from Logical Switch " + logicalSwitchUuid);
        try {
            niciraNvpApi.deleteLogicalSwitchPort(logicalSwitchUuid, lSwitchPort.getUuid());
        } catch (NiciraNvpApiException exceptionDeleteLSwitchPort) {
            logger.error("Error while deleting Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") from Logical Switch " + logicalSwitchUuid + " due to: " + exceptionDeleteLSwitchPort.getMessage());
        }
        logger.warn("Logical Switch Port " + lSwitchPort.getUuid() + " (" + lSwitchPort.getDisplayName() + ") successfully deleted");
    }

    private void cleanupLRouterPort(String logicalRouterUuid, LogicalRouterPort lRouterPort, NiciraNvpApi niciraNvpApi) {
        logger.warn("Deleting previously created Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") from Logical Router " + logicalRouterUuid + " and retrying");
        try {
            niciraNvpApi.deleteLogicalRouterPort(logicalRouterUuid, lRouterPort.getUuid());
        } catch (NiciraNvpApiException exceptionDelete) {
            logger.error("Error while deleting Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") from Logical Router " + logicalRouterUuid + " due to: " + exceptionDelete.getMessage());
        }
        logger.warn("Logical Router Port " + lRouterPort.getUuid() + " (" + lRouterPort.getDisplayName() + ") successfully deleted");
    }

    private Answer handleException(NiciraNvpApiException e, ConfigureSharedNetworkUuidCommand command, NiciraNvpResource niciraNvpResource) {
        if (HttpStatusCodeHelper.isConflict(e.getErrorCode())){
            logger.warn("There's been a conflict in NSX side, aborting implementation");
            return new ConfigureSharedNetworkUuidAnswer(command, false, "FAILED: There's been a conflict in NSX side");
        }
        else {
            logger.warn("Error code: " + e.getErrorCode() + ", retrying");
            final CommandRetryUtility retryUtility = niciraNvpResource.getRetryUtility();
            retryUtility.addRetry(command, NUM_RETRIES);
            return retryUtility.retry(command, ConfigureSharedNetworkUuidAnswer.class, e);
        }
    }

}
