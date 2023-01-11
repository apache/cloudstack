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
package org.apache.cloudstack.api.command.user.nat;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.IpForwardingRuleResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.user.Account;

@APICommand(name = "createIpForwardingRule", description = "Creates an IP forwarding rule", responseObject = FirewallRuleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateIpForwardingRuleCmd extends BaseAsyncCreateCmd implements StaticNatRule {
    public static final Logger s_logger = Logger.getLogger(CreateIpForwardingRuleCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IP_ADDRESS_ID,
               type = CommandType.UUID,
               entityType = IPAddressResponse.class,
               required = true,
               description = "the public IP address ID of the forwarding rule, already associated via associateIp")
    private Long ipAddressId;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, required = true, description = "the start port for the rule")
    private Integer startPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the end port for the rule")
    private Integer endPort;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description = "the protocol for the rule. Valid values are TCP or UDP.")
    private String protocol;

    @Parameter(name = ApiConstants.OPEN_FIREWALL,
               type = CommandType.BOOLEAN,
               description = "if true, firewall rule for source/end public port is automatically created; if false - firewall rule has to be created explicitly. Has value true by default")
    private Boolean openFirewall;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the CIDR list to forward traffic from. Multiple entries must be separated by a single comma character (,). This parameter is deprecated. Do not use.")
    private List<String> cidrlist;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
        return ipAddressId;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public Boolean getOpenFirewall() {
        if (openFirewall != null) {
            return openFirewall;
        } else {
            return true;
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException {

        boolean result = true;
        FirewallRule rule = null;
        try {
            CallContext.current().setEventDetails("Rule ID: " + getEntityId());

            if (getOpenFirewall()) {
                result = result && _firewallService.applyIngressFirewallRules(ipAddressId, CallContext.current().getCallingAccount());
            }

            result = result && _rulesService.applyStaticNatRules(ipAddressId, CallContext.current().getCallingAccount());
            rule = _entityMgr.findById(FirewallRule.class, getEntityId());
            StaticNatRule staticNatRule = _rulesService.buildStaticNatRule(rule, false);
            IpForwardingRuleResponse fwResponse = _responseGenerator.createIpForwardingRuleResponse(staticNatRule);
            fwResponse.setResponseName(getCommandName());
            setResponseObject(fwResponse);
        } finally {
            if (!result || rule == null) {

                if (getOpenFirewall()) {
                    _firewallService.revokeRelatedFirewallRule(getEntityId(), true);
                }

                _rulesService.revokeStaticNatRule(getEntityId(), true);

                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Error in creating IP forwarding rule on the domr");
            }
        }
    }

    @Override
    public void create() {

        //cidr list parameter is deprecated
        if (cidrlist != null) {
            throw new InvalidParameterValueException(
                "Parameter cidrList is deprecated; if you need to open firewall rule for the specific CIDR, please refer to createFirewallRule command");
        }

        try {
            StaticNatRule rule = _rulesService.createStaticNatRule(this, getOpenFirewall());
            setEntityId(rule.getId());
            setEntityUuid(rule.getUuid());
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Unable to create static NAT rule due to ", e);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
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
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD;
    }

    @Override
    public String getEventDescription() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ("Applying an ipforwarding 1:1 NAT rule for IP: " + ip.getAddress() + " with virtual machine:" + getVirtualMachineId());
    }

    private long getVirtualMachineId() {
        Long vmId = _networkService.getIp(ipAddressId).getAssociatedWithVmId();

        if (vmId == null) {
            throw new InvalidParameterValueException("IP address is not associated with any network, unable to create static NAT rule");
        }
        return vmId;
    }

    @Override
    public String getDestIpAddress() {
        return null;
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("Don't call me");
    }

    @Override
    public Long getSourceIpAddressId() {
        return ipAddressId;
    }

    @Override
    public Integer getSourcePortStart() {
        return startPort;
    }

    @Override
    public Integer getSourcePortEnd() {
        if (endPort == null) {
            return startPort;
        } else {
            return endPort;
        }
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public FirewallRule.Purpose getPurpose() {
        return FirewallRule.Purpose.StaticNat;
    }

    @Override
    public FirewallRule.State getState() {
        throw new UnsupportedOperationException("Don't call me");
    }

    @Override
    public long getNetworkId() {
        return -1;
    }

    @Override
    public long getDomainId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getDomainId();
    }

    @Override
    public long getAccountId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getAccountId();
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
            throw new InvalidParameterValueException("Unable to find IP address by ID " + ipAddressId);
        }
        return ip;
    }

    @Override
    public Integer getIcmpCode() {
        return null;
    }

    @Override
    public Integer getIcmpType() {
        return null;
    }

    @Override
    public List<String> getSourceCidrList() {
        return null;
    }

    @Override
    public Long getRelated() {
        return null;
    }

    @Override
    public FirewallRuleType getType() {
        return FirewallRuleType.User;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.FirewallRule;
    }

    @Override
    public TrafficType getTrafficType() {
        return null;
    }

    @Override
    public boolean isDisplay() {
        return true;
    }

    @Override
    public List<String> getDestinationCidrList(){
        return null;
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
