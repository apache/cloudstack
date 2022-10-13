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
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = ApplyTungstenFabricPolicyCmd.APINAME, description = "apply Tungsten-Fabric policy",
    responseObject = TungstenFabricPolicyResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo =
    false)
public class ApplyTungstenFabricPolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ApplyTungstenFabricPolicyCmd.class.getName());
    public static final String APINAME = "applyTungstenFabricPolicy";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_UUID, type = CommandType.STRING, required = true, description = "the uuid of network")
    private String networkUuid;

    @Parameter(name = ApiConstants.POLICY_UUID, type = CommandType.STRING, required = true, description = "the uuid of Tungsten-Fabric policy")
    private String policyUuid;

    @Parameter(name = ApiConstants.MAJOR_SEQUENCE, type = CommandType.INTEGER, required = true, description = "the major sequence of Tungsten-Fabric policy")
    private int majorSequence;

    @Parameter(name = ApiConstants.MINOR_SEQUENCE, type = CommandType.INTEGER, required = true, description = "the minor sequence of Tungsten-Fabric policy")
    private int minorSequence;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricPolicyResponse tungstenNetworkPolicyResponse = tungstenService.applyTungstenPolicy(zoneId,
            networkUuid, policyUuid, majorSequence, minorSequence);
        if (tungstenNetworkPolicyResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to applied Tungsten-Fabric policy");
        } else {
            tungstenNetworkPolicyResponse.setResponseName(getCommandName());
            setResponseObject(tungstenNetworkPolicyResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_APPLY_POLICY;
    }

    @Override
    public String getEventDescription() {
        return "apply Tungsten-Fabric network policy";
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
