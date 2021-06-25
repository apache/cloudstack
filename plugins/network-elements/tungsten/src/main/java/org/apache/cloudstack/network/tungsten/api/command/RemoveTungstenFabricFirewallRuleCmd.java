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
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = RemoveTungstenFabricFirewallRuleCmd.APINAME, description = "remove Tungsten-Fabric firewall rule",
    responseObject = TungstenFabricFirewallPolicyResponse.class, requestHasSensitiveInfo = false,
    responseHasSensitiveInfo = false)
public class RemoveTungstenFabricFirewallRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveTungstenFabricFirewallRuleCmd.class.getName());
    public static final String APINAME = "removeTungstenFabricFirewallRule";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.FIREWALL_POLICY_UUID, type = CommandType.STRING, required = true, description = "the Uuid of Tungsten-Fabric firewall policy")
    private String firewallPolicyUuid;

    @Parameter(name = ApiConstants.FIREWALL_RULE_UUID, type = CommandType.STRING, required = true, description = "the Uuid of Tungsten-Fabric firewall rule")
    private String firewallRuleUuid;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenFabricFirewallPolicyResponse tungstenFabricFirewallPolicyResponse =
            tungstenService.removeTungstenFirewallRule(
            zoneId, firewallPolicyUuid, firewallRuleUuid);
        if (tungstenFabricFirewallPolicyResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove Tungsten-Fabric firewall rule");
        } else {
            tungstenFabricFirewallPolicyResponse.setResponseName(getCommandName());
            setResponseObject(tungstenFabricFirewallPolicyResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_REMOVE_FIREWALL_RULE;
    }

    @Override
    public String getEventDescription() {
        return "remove Tungsten-Fabric firewall rule";
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
