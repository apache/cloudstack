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
import com.cloud.utils.TungstenUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = AddTungstenFabricPolicyRuleCmd.APINAME, description = "add Tungsten-Fabric policy rule",
    responseObject = TungstenFabricRuleResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo =
    false)
public class AddTungstenFabricPolicyRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddTungstenFabricPolicyRuleCmd.class.getName());
    public static final String APINAME = "addTungstenFabricPolicyRule";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true , description = "the ID of zone")
    private long zoneId;

    @Parameter(name = ApiConstants.POLICY_UUID, type = CommandType.STRING, required = true, description = "the uuid of Tungsten-Fabric policy")
    private String policyUuid;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule action")
    private String action;

    @Parameter(name = ApiConstants.DIRECTION, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule direction")
    private String direction;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule protocol")
    private String protocol;

    @Parameter(name = ApiConstants.SRC_NETWORK, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule source network")
    private String srcNetwork;

    @Parameter(name = ApiConstants.SRC_IP_PREFIX, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule source ip prefix")
    private String srcIpPrefix;

    @Parameter(name = ApiConstants.SRC_IP_PREFIX_LEN, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule source ip prefix length")
    private int srcIpPrefixLen;

    @Parameter(name = ApiConstants.SRC_START_PORT, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule source start port")
    private int srcStartPort;

    @Parameter(name = ApiConstants.SRC_END_PORT, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule source end port")
    private int srcEndPort;

    @Parameter(name = ApiConstants.DEST_NETWORK, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule destination network")
    private String destNetwork;

    @Parameter(name = ApiConstants.DEST_IP_PREFIX, type = CommandType.STRING, required = true, description = "Tungsten-Fabric policy rule destination ip prefix")
    private String destIpPrefix;

    @Parameter(name = ApiConstants.DEST_IP_PREFIX_LEN, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule destination ip prefix length")
    private int destIpPrefixLen;

    @Parameter(name = ApiConstants.DEST_START_PORT, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule destination start port")
    private int destStartPort;

    @Parameter(name = ApiConstants.DEST_END_PORT, type = CommandType.INTEGER, required = true, description = "Tungsten-Fabric policy rule destination end port")
    private int destEndPort;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_ADD_POLICY_RULE;
    }

    @Override
    public String getEventDescription() {
        return "add Tungsten-Fabric policy rule";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        String tungstenDirection =
            direction.equals(ApiConstants.ONE_WAY) ? TungstenUtils.ONE_WAY_DIRECTION : TungstenUtils.TWO_WAY_DIRECTION;
        TungstenFabricRuleResponse tungstenFabricRuleResponse = tungstenService.addTungstenPolicyRule(zoneId,
            policyUuid, action, tungstenDirection, protocol, srcNetwork, srcIpPrefix, srcIpPrefixLen, srcStartPort,
            srcEndPort, destNetwork, destIpPrefix, destIpPrefixLen, destStartPort, destEndPort);
        if (tungstenFabricRuleResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Tungsten-Fabric policy rule");
        } else {
            tungstenFabricRuleResponse.setResponseName(getCommandName());
            setResponseObject(tungstenFabricRuleResponse);
        }
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
