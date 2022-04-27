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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;

@APICommand(name = UpdateIpv6FirewallRuleCmd.APINAME, description = "Updates Ipv6 firewall rule with specified ID", responseObject = FirewallRuleResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateIpv6FirewallRuleCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIpv6FirewallRuleCmd.class.getName());

    public static final String APINAME = "updateIpv6FirewallRule";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = true, description = "the ID of the ipv6 firewall rule")
    private Long id;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "the protocol for the Ipv6 firewall rule. Valid values are TCP/UDP/ICMP/ALL or valid protocol number")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of Ipv6 firewall rule")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of Ipv6 firewall rule")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to allow traffic from/to. Multiple entries must be separated by a single comma character (,).")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the ICMP message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this ICMP message")
    private Integer icmpCode;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "the traffic type for the Ipv6 firewall rule, can be Ingress or Egress, defaulted to Ingress if not specified")
    private String trafficType;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the Ipv6 firewall rule to the end user or not", since = "4.4", authorized = {
            RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getProtocol() {
        if (protocol != null) {
            return protocol.trim();
        } else {
            return null;
        }
    }

    public List<String> getSourceCidrList() {
        return cidrlist;
    }

    public FirewallRule.TrafficType getTrafficType() {
        if (trafficType != null) {
            for (FirewallRule.TrafficType type : FirewallRule.TrafficType.values()) {
                if (type.toString().equalsIgnoreCase(trafficType)) {
                    return type;
                }
            }
        }
        return null;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    public Integer getSourcePortStart() {
        return publicStartPort;
    }

    public Integer getSourcePortEnd() {
        return publicEndPort;
    }

    @Override
    public long getEntityOwnerId() {
        FirewallRule rule = _firewallService.getFirewallRule(id);
        if (rule != null) {
            return rule.getAccountId();
        }
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IPV6_FIREWALL_RULE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating ipv6 firewall rule";
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext.current().setEventDetails("Rule Id: " + getId());
        FirewallRule rules = ipv6Service.updateIpv6FirewallRule(this);
        FirewallResponse ruleResponse = _responseGenerator.createIpv6FirewallRuleResponse(rules);
        setResponseObject(ruleResponse);
        ruleResponse.setResponseName(getCommandName());
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), FirewallRule.class);
        }
    }

    @Override
    public Long getApiResourceId() {
        FirewallRule rule = _firewallService.getFirewallRule(id);
        if (rule != null) {
            return rule.getNetworkId();
        }
        return null;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Network;
    }
}
