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

import com.cloud.network.rules.FirewallRule;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createIpv6FirewallRule", description = "Creates an Ipv6 firewall rule in the given network (the network has to belong to VPC)", responseObject = FirewallRuleResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateIpv6FirewallRuleCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIpv6FirewallRuleCmd.class.getName());

    private static final String s_name = "createipv6firewallruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description = "the protocol for the Ipv6 firewall rule. Valid values are TCP/UDP/ICMP/ALL or valid protocol number")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of Ipv6 firewall rule")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of Ipv6 firewall rule")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the CIDR list to allow traffic from/to. Multiple entries must be separated by a single comma character (,).")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the ICMP message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this ICMP message")
    private Integer icmpCode;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "The network of the VM the Ipv6 firewall rule will be created for")
    private Long networkId;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "the traffic type for the Ipv6 firewall rule," + "can be ingress or egress, defaulted to ingress if not specified")
    private String trafficType;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {
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

    public String getProtocol() {
        String p = protocol.trim();
        // Deal with ICMP(protocol number 1) specially because it need to be paired with icmp type and code
        if (StringUtils.isNumeric(p)) {
            int protoNumber = Integer.parseInt(p);
            if (protoNumber == 1) {
                p = "icmp";
            }
        }
        return p;
    }

    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            return cidrlist;
        } else {
            List<String> oneCidrList = new ArrayList<String>();
            oneCidrList.add(NetUtils.ALL_IP4_CIDRS);
            return oneCidrList;
        }
    }

    public FirewallRule.TrafficType getTrafficType() {
        if (trafficType == null) {
            return FirewallRule.TrafficType.Ingress;
        }
        for (FirewallRule.TrafficType type : FirewallRule.TrafficType.values()) {
            if (type.toString().equalsIgnoreCase(trafficType)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid traffic type " + trafficType);
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Integer getSourcePortStart() {
        return publicStartPort;
    }

    public Integer getSourcePortEnd() {
        if (publicEndPort == null) {
            if (publicStartPort != null) {
                return publicStartPort;
            }
        } else {
            return publicEndPort;
        }

        return null;
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IPV6_FIREWALL_RULE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating ipv6 firewall rule";
    }

    public Integer getIcmpCode() {
        if (icmpCode != null) {
            return icmpCode;
        } else if (getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
            return -1;
        }
        return null;
    }

    public Integer getIcmpType() {
        if (icmpType != null) {
            return icmpType;
        } else if (getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
            return -1;

        }
        return null;
    }

    @Override
    public void create() {
        FirewallRule result = ipv6Service.createIpv6FirewallRule(this);
        setEntityId(result.getId());
        setEntityUuid(result.getUuid());
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        boolean success = false;
        FirewallRule rule = ipv6Service.getIpv6FirewallRule(getEntityId());
        try {
            CallContext.current().setEventDetails("Rule ID: " + getEntityId());
            success = ipv6Service.applyIpv6FirewallRule(rule.getId());

            // State is different after the rule is applied, so get new object here
            rule = ipv6Service.getIpv6FirewallRule(getEntityId());
            FirewallRuleResponse ruleResponse = new FirewallRuleResponse();
            if (rule != null) {
                ruleResponse = _responseGenerator.createIpv6FirewallRuleResponse(rule);
                setResponseObject(ruleResponse);
            }
            ruleResponse.setResponseName(getCommandName());
        } finally {
            if (!success || rule == null) {
                ipv6Service.revokeIpv6FirewallRule(getEntityId());
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create ipv6 firewall rule");
            }
        }
    }
}
