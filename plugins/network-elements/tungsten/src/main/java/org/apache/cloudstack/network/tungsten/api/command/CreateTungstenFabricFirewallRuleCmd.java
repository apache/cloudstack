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
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;

import javax.inject.Inject;

@APICommand(name = CreateTungstenFabricFirewallRuleCmd.APINAME, description = "create Tungsten-Fabric firewall",
    responseObject = TungstenFabricFirewallRuleResponse.class, requestHasSensitiveInfo = false,
    responseHasSensitiveInfo = false)
public class CreateTungstenFabricFirewallRuleCmd extends BaseAsyncCmd {
    public static final String APINAME = "createTungstenFabricFirewallRule";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.FIREWALL_POLICY_UUID, type = CommandType.STRING, required = true, description = "the uuid of Tungsten-Fabric firewall policy")
    private String firewallPolicyUuid;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten-Fabric firewall rule name")
    private String name;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, required = true, description = "Tungsten-Fabric firewall rule action")
    private String action;

    @Parameter(name = ApiConstants.SERVICE_GROUP_UUID, type = CommandType.STRING, required = true, description = "Tungsten-Fabric firewall rule service group uuid")
    private String serviceGroupUuid;

    @Parameter(name = ApiConstants.SRC_TAG_UUID, type = CommandType.STRING, description = "Tungsten-Fabric firewall rule source tag uuid")
    private String srcTagUuid;

    @Parameter(name = ApiConstants.SRC_ADDRESS_GROUP_UUID, type = CommandType.STRING, description = "Tungsten-Fabric firewall rule source address group uuid")
    private String srcAddressGroupUuid;

    @Parameter(name = ApiConstants.SRC_NETWORK_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric source network")
    private String srcNetworkUuid;

    @Parameter(name = ApiConstants.DIRECTION, type = CommandType.STRING, required = true, description = "Tungsten-Fabric firewall rule direction")
    private String direction;

    @Parameter(name = ApiConstants.DEST_TAG_UUID, type = CommandType.STRING, description = "Tungsten-Fabric firewall rule destination tag uuid")
    private String destTagUuid;

    @Parameter(name = ApiConstants.DEST_ADDRESS_GROUP_UUID, type = CommandType.STRING, description = "Tungsten-Fabric firewall rule destination address group uuid")
    private String destAddressGroupUuid;

    @Parameter(name = ApiConstants.DEST_NETWORK_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric destination network")
    private String destNetworkUuid;

    @Parameter(name = ApiConstants.TAG_TYPE_UUID, type = CommandType.STRING, description = "Tungsten-Fabric firewall rule tag type uuid")
    private String tagTypeUuid;

    @Parameter(name = ApiConstants.SEQUENCE, type = CommandType.INTEGER, required = true, description = "the sequence of Tungsten-Fabric firewall rule")
    private int sequence;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        String tungstenDirection =
            direction.equals(ApiConstants.ONE_WAY) ? TungstenUtils.ONE_WAY_DIRECTION : TungstenUtils.TWO_WAY_DIRECTION;
        TungstenFabricFirewallRuleResponse tungstenFabricFirewallRuleResponse =
            tungstenService.createTungstenFirewallRule(
            zoneId, firewallPolicyUuid, name, action, serviceGroupUuid, srcTagUuid, srcAddressGroupUuid, srcNetworkUuid, tungstenDirection, destTagUuid,
            destAddressGroupUuid, destNetworkUuid, tagTypeUuid, sequence);
        if (tungstenFabricFirewallRuleResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Tungsten-Fabric firewall rule");
        } else {
            tungstenFabricFirewallRuleResponse.setResponseName(getCommandName());
            setResponseObject(tungstenFabricFirewallRuleResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_CREATE_FIREWALL_RULE;
    }

    @Override
    public String getEventDescription() {
        return "create Tungsten-Fabric firewall rule";
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
