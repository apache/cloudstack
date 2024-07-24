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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = RemoveTungstenFabricTagCmd.APINAME, description = "remove Tungsten-Fabric tag", responseObject =
    TungstenFabricTagResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RemoveTungstenFabricTagCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveTungstenFabricTagCmd.class.getName());
    public static final String APINAME = "removeTungstenFabricTag";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_UUID, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the uuid of networks")
    private List<String> networkUuids;

    @Parameter(name = ApiConstants.VM_UUID, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the uuid of vms")
    private List<String> vmUuids;

    @Parameter(name = ApiConstants.NIC_UUID, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the uuid of nics")
    private List<String> nicUuids;

    @Parameter(name = ApiConstants.POLICY_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric policy")
    private String policyUuid;

    @Parameter(name = ApiConstants.APPLICATION_POLICY_SET_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric application policy set")
    private String applicationPolicySetUuid;

    @Parameter(name = ApiConstants.TAG_UUID, type = CommandType.STRING, required = true, description = "the uuid of Tungsten-Fabric tag")
    private String tagUuid;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricTagResponse tungstenFabricTagResponse = tungstenService.removeTungstenTag(zoneId, networkUuids,
            vmUuids, nicUuids, policyUuid, applicationPolicySetUuid, tagUuid);
        if (tungstenFabricTagResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove Tungsten-Fabric tag");
        } else {
            tungstenFabricTagResponse.setResponseName(getCommandName());
            setResponseObject(tungstenFabricTagResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_REMOVE_TAG;
    }

    @Override
    public String getEventDescription() {
        return "remove Tungsten-Fabric tag";
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
