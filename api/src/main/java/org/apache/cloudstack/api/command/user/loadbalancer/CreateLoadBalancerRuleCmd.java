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
package org.apache.cloudstack.api.command.user.loadbalancer;

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createLoadBalancerRule", description = "Creates a load balancer rule", responseObject = LoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateLoadBalancerRuleCmd extends BaseAsyncCreateCmd /*implements LoadBalancer */{
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ALGORITHM, type = CommandType.STRING, required = true, description = "load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the load balancer rule", length = 4096)
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name = ApiConstants.PRIVATE_PORT,
               type = CommandType.INTEGER,
               required = true,
               description = "the private port of the private IP address/virtual machine where the network traffic will be load balanced to")
    private Integer privatePort;

    @Parameter(name = ApiConstants.PUBLIC_IP_ID,
               type = CommandType.UUID,
               entityType = IPAddressResponse.class,
               description = "public IP address ID from where the network traffic will be load balanced from")
    private Long publicIpId;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = false,
               description = "zone where the load balancer is going to be created. This parameter is required when LB service provider is ElasticLoadBalancerVm")
    private Long zoneId;

    @Parameter(name = ApiConstants.PUBLIC_PORT,
               type = CommandType.INTEGER,
               required = true,
               description = "the public port from where the network traffic will be load balanced from")
    private Integer publicPort;

    @Parameter(name = ApiConstants.OPEN_FIREWALL, type = CommandType.BOOLEAN, description = "if true, firewall rule for"
        + " source/end public port is automatically created; if false - firewall rule has to be created explicitly. If not specified 1) defaulted to false when LB"
        + " rule is being created for VPC guest network 2) in all other cases defaulted to true")
    private Boolean openFirewall;

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "the account associated with the load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "the domain ID associated with the load balancer")
    private Long domainId;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, since = "4.18.0.0", description = "the CIDR list to allow traffic, "
            + "all other CIDRs will be blocked. Multiple entries must be separated by a single comma character (,). By default, all CIDRs are allowed.")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "The guest network this "
        + "rule will be created for. Required when public Ip address is not associated with any Guest network yet (VPC case)")
    private Long networkId;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "The protocol for the LB such as tcp, udp or tcp-proxy.")
    private String lbProtocol;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public Long getSourceIpAddressId() {
        if (publicIpId != null) {
            IpAddress ipAddr = _networkService.getIp(publicIpId);
            if (ipAddr == null || !ipAddr.readyToUse()) {
                throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address ID " + publicIpId);
            }
        } else if (getEntityId() != null) {
            LoadBalancer rule = _entityMgr.findById(LoadBalancer.class, getEntityId());
            return rule.getSourceIpAddressId();
        }

        return publicIpId;
    }

    private Long getVpcId() {
        if (publicIpId != null) {
            IpAddress ipAddr = _networkService.getIp(publicIpId);
            if (ipAddr == null || !ipAddr.readyToUse()) {
                throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address ID " + publicIpId);
            } else {
                return ipAddr.getVpcId();
            }
        }
        return null;
    }

    public long getNetworkId() {
        if (networkId != null) {
            return networkId;
        }
        Long zoneId = getZoneId();

        if (zoneId == null) {
            Long ipId = getSourceIpAddressId();
            if (ipId == null) {
                throw new InvalidParameterValueException("Either networkId or zoneId or publicIpId has to be specified");
            }
        }

        if (zoneId != null) {
            DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
            if (zone.getNetworkType() == NetworkType.Advanced) {
                List<? extends Network> networks = _networkService.getIsolatedNetworksOwnedByAccountInZone(getZoneId(), _accountService.getAccount(getEntityOwnerId()));
                if (networks.size() == 0) {
                    String domain = _domainService.getDomain(getDomainId()).getName();
                    throw new InvalidParameterValueException("Account name=" + getAccountName() + " domain=" + domain + " doesn't have virtual networks in zone=" +
                        zone.getName());
                }

                if (networks.size() < 1) {
                    throw new InvalidParameterValueException("Account doesn't have any isolated networks in the zone");
                } else if (networks.size() > 1) {
                    throw new InvalidParameterValueException("Account has more than one isolated network in the zone");
                }

                return networks.get(0).getId();
            } else {
                Network defaultGuestNetwork = _networkService.getExclusiveGuestNetwork(zoneId);
                if (defaultGuestNetwork == null) {
                    throw new InvalidParameterValueException("Unable to find a default guest network for account " + getAccountName() + " in domain ID=" + getDomainId());
                } else {
                    return defaultGuestNetwork.getId();
                }
            }
        } else {
            IpAddress ipAddr = _networkService.getIp(publicIpId);
            if (ipAddr.getAssociatedWithNetworkId() != null) {
                return ipAddr.getAssociatedWithNetworkId();
            } else {
                throw new InvalidParameterValueException("IP address ID=" + publicIpId + " is not associated with any network");
            }
        }
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public String getName() {
        return loadBalancerRuleName;
    }

    public Boolean getOpenFirewall() {
        boolean isVpc = getVpcId() == null ? false : true;
        if (openFirewall != null) {
            if (isVpc && openFirewall) {
                throw new InvalidParameterValueException("Can't have openFirewall=true when IP address belongs to VPC");
            }
            return openFirewall;
        } else {
            if (isVpc) {
                return false;
            }
            return true;
        }
    }

    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            throw new InvalidParameterValueException(
                "Parameter cidrList is deprecated; if you need to open firewall rule for the specific CIDR, please refer to createFirewallRule command");
        }
        return null;
    }

    public String getLbProtocol() {
        return StringUtils.trim(StringUtils.lowerCase(lbProtocol));
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {

        CallContext callerContext = CallContext.current();
        boolean success = true;
        LoadBalancer rule = null;
        try {
            CallContext.current().setEventDetails("Rule Id: " + getEntityId());

            if (getOpenFirewall()) {
                success = success && _firewallService.applyIngressFirewallRules(getSourceIpAddressId(), callerContext.getCallingAccount());
            }

            // State might be different after the rule is applied, so get new object here
            rule = _entityMgr.findById(LoadBalancer.class, getEntityId());
            LoadBalancerResponse lbResponse = new LoadBalancerResponse();
            if (rule != null) {
                lbResponse = _responseGenerator.createLoadBalancerResponse(rule);
                setResponseObject(lbResponse);
            }
            lbResponse.setResponseName(getCommandName());
        } catch (Exception ex) {
            s_logger.warn("Failed to create LB rule due to exception ", ex);
        } finally {
            if (!success || rule == null) {

                if (getOpenFirewall()) {
                    _firewallService.revokeRelatedFirewallRule(getEntityId(), true);
                }
                // no need to apply the rule on the backend as it exists in the db only
                _lbService.deleteLoadBalancerRule(getEntityId(), false);

                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create load balancer rule");
            }
        }
    }

    @Override
    public void create() {
        try {
            LoadBalancer result =
                _lbService.createPublicLoadBalancerRule(getXid(), getName(), getDescription(), getSourcePortStart(), getSourcePortEnd(), getDefaultPortStart(),
                    getDefaultPortEnd(), getSourceIpAddressId(), getProtocol(), getAlgorithm(), getNetworkId(), getEntityOwnerId(), getOpenFirewall(), getLbProtocol(), isDisplay(),
                        getCidrList());
            this.setEntityId(result.getId());
            this.setEntityUuid(result.getUuid());
        } catch (NetworkRuleConflictException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        } catch (InsufficientAddressCapacityException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    public Integer getSourcePortStart() {
        return publicPort.intValue();
    }

    public Integer getSourcePortEnd() {
        return publicPort.intValue();
    }

    public String getProtocol() {
        return NetUtils.TCP_PROTO;
    }

    public long getAccountId() {
        if (publicIpId != null)
            return _networkService.getIp(getSourceIpAddressId()).getAccountId();

        Account account = null;
        if ((domainId != null) && (accountName != null)) {
            account = _responseGenerator.findAccountByNameDomain(accountName, domainId);
            if (account != null) {
                return account.getId();
            } else {
                throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain ID=" + domainId);
            }
        } else {
            throw new InvalidParameterValueException("Can't define IP owner. Either specify account/domainId or publicIpId");
        }
    }

    public long getDomainId() {
        if (publicIpId != null)
            return _networkService.getIp(getSourceIpAddressId()).getDomainId();
        if (domainId != null) {
            return domainId;
        }
        return CallContext.current().getCallingAccount().getDomainId();
    }

    public int getDefaultPortStart() {
        return privatePort.intValue();
    }

    public int getDefaultPortEnd() {
        return privatePort.intValue();
    }

    @Override
    public long getEntityOwnerId() {
        return getAccountId();
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId = publicIpId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating load balancer: " + getName() + " account: " + getAccountName();

    }

    public String getXid() {
        /*FIXME*/
        return null;
    }

    public void setSourceIpAddressId(Long ipId) {
        this.publicIpId = ipId;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.FirewallRule;
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }

    public List<String> getCidrList(){
        return cidrlist;
    }

}
