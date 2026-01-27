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
package org.apache.cloudstack.api.command.user.network.routing;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.firewall.IListFirewallRulesCmd;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;

import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.Pair;

@APICommand(name = "listRoutingFirewallRules",
        description = "Lists all Routing firewall rules",
        since = "4.20.0",
        responseObject = FirewallRuleResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListRoutingFirewallRulesCmd extends BaseListTaggedResourcesCmd implements IListFirewallRulesCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class,
               description = "Lists Routing firewall rule with the specified ID")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list Routing firewall rules by network ID")
    private Long networkId;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "list Routing firewall rules by traffic type - ingress or egress")
    private String trafficType;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public FirewallRule.TrafficType getTrafficType() {
        if (trafficType != null) {
            return FirewallRule.TrafficType.valueOf(trafficType);
        }
        return null;
    }

    @Override
    public Long getIpAddressId() {
        return null;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends FirewallRule>, Integer> result = routedIpv4Manager.listRoutingFirewallRules(this);
        ListResponse<FirewallResponse> response = new ListResponse<>();
        List<FirewallResponse> ruleResponses = new ArrayList<>();

        for (FirewallRule rule : result.first()) {
            FirewallResponse ruleData = _responseGenerator.createFirewallResponse(rule);
            ruleResponses.add(ruleData);
        }
        response.setResponses(ruleResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
