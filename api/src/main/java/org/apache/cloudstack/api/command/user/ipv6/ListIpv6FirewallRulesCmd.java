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
package org.apache.cloudstack.api.command.user.ipv6;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.firewall.IListFirewallRulesCmd;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.Pair;

@APICommand(name = "listIpv6FirewallRules", description = "Lists all IPv6 firewall rules", responseObject = FirewallRuleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListIpv6FirewallRulesCmd extends BaseListTaggedResourcesCmd implements IListFirewallRulesCmd {
    public static final Logger s_logger = Logger.getLogger(ListIpv6FirewallRulesCmd.class.getName());

    private static final String s_name = "listipv6firewallrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class,
               description = "Lists ipv6 firewall rule with the specified ID")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list ipv6 firewall rules by network ID")
    private Long networkId;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "list ipv6 firewall rules by traffic type - ingress or egress")
    private String trafficType;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "list ipv6 firewall rules by protocol")
    private String protocol;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, description = "list ipv6 firewall rules by action: allow or deny")
    private String action;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
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

    public String getProtocol() {
        return protocol;
    }

    public String getAction() {
        return action;
    }

    @Override
    public Boolean getDisplay() {
        return BooleanUtils.toBooleanDefaultIfNull(display, super.getDisplay());
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends FirewallRule>, Integer> result = ipv6Service.listIpv6FirewallRules(this);
        ListResponse<FirewallRuleResponse> response = new ListResponse<FirewallRuleResponse>();
        List<FirewallRuleResponse> ruleResponses = new ArrayList<FirewallRuleResponse>();

        for (FirewallRule rule : result.first()) {
            FirewallRuleResponse ruleData = _responseGenerator.createIpv6FirewallRuleResponse(rule);
            ruleResponses.add(ruleData);
        }
        response.setResponses(ruleResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
