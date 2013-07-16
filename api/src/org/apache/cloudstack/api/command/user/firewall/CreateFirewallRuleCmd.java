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
package org.apache.cloudstack.api.command.user.firewall;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createFirewallRule", description = "Creates a firewall rule for a given ip address", responseObject = FirewallResponse.class)
public class CreateFirewallRuleCmd extends BaseAsyncCreateCmd implements FirewallRule {
    public static final Logger s_logger = Logger.getLogger(CreateFirewallRuleCmd.class.getName());

    private static final String s_name = "createfirewallruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IP_ADDRESS_ID, type = CommandType.UUID, entityType = IPAddressResponse.class,
            required=true, description = "the IP address id of the port forwarding rule")
    private Long ipAddressId;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description = "the protocol for the firewall rule. Valid values are TCP/UDP/ICMP.")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of firewall rule")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of firewall rule")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic from")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the icmp message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this icmp message")
    private Integer icmpCode;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "type of firewallrule: system/user")
    private String type;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////


    public Long getIpAddressId() {
        return ipAddressId;
    }

    @Override
    public String getProtocol() {
        return protocol.trim();
    }

    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            return cidrlist;
        } else {
            List<String> oneCidrList = new ArrayList<String>();
            oneCidrList.add(NetUtils.ALL_CIDRS);
            return oneCidrList;
        }

    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public void setSourceCidrList(List<String> cidrs){
        cidrlist = cidrs;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext callerContext = CallContext.current();
        boolean success = false;
        FirewallRule rule = _entityMgr.findById(FirewallRule.class, getEntityId());
        try {
            CallContext.current().setEventDetails("Rule Id: " + getEntityId());
            success = _firewallService.applyIngressFirewallRules(rule.getSourceIpAddressId(), callerContext.getCallingAccount());

            // State is different after the rule is applied, so get new object here
            rule = _entityMgr.findById(FirewallRule.class, getEntityId());
            FirewallResponse fwResponse = new FirewallResponse();
            if (rule != null) {
                fwResponse = _responseGenerator.createFirewallResponse(rule);
                setResponseObject(fwResponse);
            }
            fwResponse.setResponseName(getCommandName());
        } finally {
            if (!success || rule == null) {
                _firewallService.revokeFirewallRule(getEntityId(), true);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create firewall rule");
            }
        }
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("database id can only provided by VO objects");
    }

    @Override
    public String getXid() {
        // FIXME: We should allow for end user to specify Xid.
        return null;
    }


    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getSourceIpAddressId() {
        return ipAddressId;
    }

    @Override
    public Integer getSourcePortStart() {
        if (publicStartPort != null) {
            return publicStartPort.intValue();
        }
        return null;
    }

    @Override
    public Integer getSourcePortEnd() {
        if (publicEndPort == null) {
            if (publicStartPort != null) {
                return publicStartPort.intValue();
            }
        } else {
            return publicEndPort.intValue();
        }

        return null;
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.Firewall;
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("Should never call me to find the state");
    }

    @Override
    public long getNetworkId() {
        IpAddress ip = _entityMgr.findById(IpAddress.class, getIpAddressId());
        Long ntwkId = null;

        if (ip.getAssociatedWithNetworkId() != null) {
            ntwkId = ip.getAssociatedWithNetworkId();
        }

        if (ntwkId == null) {
            throw new InvalidParameterValueException("Unable to create firewall rule for the ipAddress id=" + ipAddressId +
                    " as ip is not associated with any network and no networkId is passed in");
        }
        return ntwkId;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public long getDomainId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getDomainId();
    }

    @Override
    public void create() {
        if (getSourceCidrList() != null) {
            for (String cidr: getSourceCidrList()){
                if (!NetUtils.isValidCIDR(cidr)){
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source cidrs formatting error " + cidr);
                }
            }
        }

        try {
            FirewallRule result = _firewallService.createIngressFirewallRule(this);
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network Rule Conflict: ", ex);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FIREWALL_OPEN;
    }

    @Override
    public String getEventDescription() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ("Createing firewall rule for Ip: " + ip.getAddress() + " for protocol:" + this.getProtocol());
    }

    @Override
    public long getAccountId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getAccountId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getIp().getAssociatedWithNetworkId();
    }

    private IpAddress getIp() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id " + ipAddressId);
        }
        return ip;
    }

    @Override
    public Integer getIcmpCode() {
        if (icmpCode != null) {
            return icmpCode;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
            return -1;
        }
        return null;
    }

    @Override
    public Integer getIcmpType() {
        if (icmpType != null) {
            return icmpType;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
                return -1;

        }
        return null;
    }

    @Override
    public Long getRelated() {
        return null;
    }

    @Override
    public FirewallRuleType getType() {
        if (type != null && type.equalsIgnoreCase("system")) {
            return FirewallRuleType.System;
        } else {
            return FirewallRuleType.User;
        }
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.FirewallRule;
    }

    @Override
    public TrafficType getTrafficType() {
        return FirewallRule.TrafficType.Ingress;
    }

}
