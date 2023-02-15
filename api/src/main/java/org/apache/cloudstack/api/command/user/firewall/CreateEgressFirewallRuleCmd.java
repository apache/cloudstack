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
// under the License.package org.apache.cloudstack.api.command.user.firewall;

package org.apache.cloudstack.api.command.user.firewall;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createEgressFirewallRule", description = "Creates a egress firewall rule for a given network ", responseObject = FirewallResponse.class, entityType = {FirewallRule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateEgressFirewallRuleCmd extends BaseAsyncCreateCmd implements FirewallRule {
    public static final Logger s_logger = Logger.getLogger(CreateEgressFirewallRuleCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               required = true,
               description = "the network id of the port forwarding rule")
    private Long networkId;

    @Parameter(name = ApiConstants.PROTOCOL,
               type = CommandType.STRING,
               required = true,
               description = "the protocol for the firewall rule. Valid values are TCP/UDP/ICMP.")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of firewall rule")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of firewall rule")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic from. Multiple entries must be separated by a single comma character (,).")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.DEST_CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic to. Multiple entries must be separated by a single comma character (,).")
    private List<String> destCidrList;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the icmp message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this icmp message")
    private Integer icmpCode;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "type of firewallrule: system/user")
    private String type;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getIpAddressId() {
        return null;
    }

    @Override
    public String getProtocol() {
        return protocol.trim();
    }

    @Override
    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            return cidrlist;
        } else {
            List<String> oneCidrList = new ArrayList<String>();
            oneCidrList.add(_networkService.getNetwork(networkId).getCidr());
            return oneCidrList;
        }
    }

    @Override
    public List<String> getDestinationCidrList(){
        return destCidrList;
    }

    public Long getVpcId() {
        Network network = _networkService.getNetwork(getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("Invalid networkId is given");
        }

        Long vpcId = network.getVpcId();
        return vpcId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    public void setSourceCidrList(List<String> cidrs) {
        cidrlist = cidrs;
    }

    public void setDestCidrList(List<String> cidrs){
        destCidrList = cidrs;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext callerContext = CallContext.current();
        boolean success = false;
        FirewallRule rule = _entityMgr.findById(FirewallRule.class, getEntityId());
        try {
            CallContext.current().setEventDetails("Rule Id: " + getEntityId());
            success = _firewallService.applyEgressFirewallRules(rule, callerContext.getCallingAccount());
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
                _firewallService.revokeEgressFirewallRule(getEntityId(), true);
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
    public Long getSourceIpAddressId() {
        return null;
    }

    @Override
    public Integer getSourcePortStart() {
        if (publicStartPort != null) {
            return publicStartPort;
        }
        return null;
    }

    @Override
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
        return networkId;
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
        Network network = _networkService.getNetwork(networkId);
            return  network.getDomainId();
        }

    @Override
    public void create() {
        validateCidrs();

        if (NetUtils.ALL_PROTO.equalsIgnoreCase(getProtocol())) {
            if (getSourcePortStart() != null && getSourcePortEnd() != null) {
                throw new InvalidParameterValueException(String.format("Unable to create firewall rule for the network id=[%d] due to: ports must not be informed to protocol ALL, as it does not require them.", networkId));
            }
        }

        if (getVpcId() != null) {
            throw new  InvalidParameterValueException(String.format("Unable to create firewall rule for the network id=[%d] as firewall egress rule can be created only for non VPC networks.", networkId));
        }

        try {
            FirewallRule result = _firewallService.createEgressFirewallRule(this);
            if (result != null) {
                setEntityId(result.getId());
                setEntityUuid(result.getUuid());
            }
        } catch (NetworkRuleConflictException ex) {
            String message = "Network rule conflict: ";
            if (!s_logger.isTraceEnabled()) {
                s_logger.info(message + ex.getMessage());
            } else {
                s_logger.trace(message, ex);
            }
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }

    protected void validateCidrs() {
        if (getSourceCidrList() != null) {
            String guestCidr = _networkService.getNetwork(getNetworkId()).getCidr();

            for (String cidr : getSourceCidrList()) {
                cidr = StringUtils.trim(cidr);
                isValidIpCidr(cidr, "Source");
                if (cidr.equals(NetUtils.ALL_IP4_CIDRS)) {
                    continue;
                }
                if (!NetUtils.isNetworkAWithinNetworkB(cidr, guestCidr)) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR,String.format("[%s] is not within the guest CIDR [%s]. ", cidr, guestCidr));
                }
            }
        }

        //Destination CIDR formatting check. Since it's optional param, no need to set a default as in the case of source.
        if(destCidrList != null){
            for(String cidr : destCidrList){
                cidr = StringUtils.trim(cidr);
                isValidIpCidr(cidr, "Destination");
            }
        }
    }

    private void isValidIpCidr(String cidr, String sourceOrDestination) {
        if (!NetUtils.isValidIp4Cidr(cidr) && !NetUtils.isValidIp6Cidr(cidr)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,String.format("%s CIDRs formatting error [%s].", sourceOrDestination, cidr));
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FIREWALL_EGRESS_OPEN;
    }

    @Override
    public String getEventDescription() {
         Network network = _networkService.getNetwork(networkId);
        return ("Creating firewall rule for network: " + network + " for protocol:" + getProtocol());
    }

    @Override
    public long getAccountId() {
        Network network = _networkService.getNetwork(networkId);
        return network.getAccountId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
                return  getNetworkId();
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
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.FirewallRule;
    }

    @Override
    public TrafficType getTrafficType() {
           return TrafficType.Egress;
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    @Override
    public Class<?> getEntityType() {
        return FirewallRule.class;
    }

    @Override
    public String getName() {
        return null;
    }

}
