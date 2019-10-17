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
package org.apache.cloudstack.api.command.user.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createNetwork", description = "Creates a network", responseObject = NetworkResponse.class, responseView = ResponseView.Restricted, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkCmd extends BaseCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkCmd.class.getName());

    private static final String s_name = "createnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the network")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, required = true, description = "the display text of the network")
    private String displayText;

    @Parameter(name = ApiConstants.NETWORK_OFFERING_ID,
               type = CommandType.UUID,
               entityType = NetworkOfferingResponse.class,
               required = true,
               description = "the network offering ID")
    private Long networkOfferingId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the zone ID for the network")
    private Long zoneId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               description = "the physical network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, description = "the gateway of the network. Required "
        + "for shared networks and isolated networks when it belongs to VPC")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, description = "the netmask of the network. Required "
        + "for shared networks and isolated networks when it belongs to VPC")
    private String netmask;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, description = "the beginning IP address in the network IP range")
    private String startIp;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, description = "the ending IP address in the network IP"
        + " range. If not specified, will be defaulted to startIP")
    private String endIp;

    @Parameter(name = ApiConstants.ISOLATED_PVLAN, type = CommandType.STRING, description = "the isolated private VLAN for this network")
    private String isolatedPvlan;

    @Parameter(name = ApiConstants.NETWORK_DOMAIN, type = CommandType.STRING, description = "network domain")
    private String networkDomain;

    @Parameter(name = ApiConstants.ACL_TYPE, type = CommandType.STRING, description = "Access control type; supported values"
        + " are account and domain. In 3.0 all shared networks should have aclType=Domain, and all isolated networks"
        + " - Account. Account means that only the account owner can use the network, domain - all accounts in the domain can use the network")
    private String aclType;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account that will own the network")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the SSH key")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning a network")
    private Long domainId;

    @Parameter(name = ApiConstants.SUBDOMAIN_ACCESS,
               type = CommandType.BOOLEAN,
               description = "Defines whether to allow"
                   + " subdomains to use networks dedicated to their parent domain(s). Should be used with aclType=Domain, defaulted to allow.subdomain.network.access global config if not specified")
    private Boolean subdomainAccess;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "the VPC network belongs to")
    private Long vpcId;

    @Parameter(name = ApiConstants.START_IPV6, type = CommandType.STRING, description = "the beginning IPv6 address in the IPv6 network range")
    private String startIpv6;

    @Parameter(name = ApiConstants.END_IPV6, type = CommandType.STRING, description = "the ending IPv6 address in the IPv6 network range")
    private String endIpv6;

    @Parameter(name = ApiConstants.IP6_GATEWAY, type = CommandType.STRING, description = "the gateway of the IPv6 network. Required for Shared networks")
    private String ip6Gateway;

    @Parameter(name = ApiConstants.IP6_CIDR, type = CommandType.STRING, description = "the CIDR of IPv6 network, must be at least /64")
    private String ip6Cidr;

    @Parameter(name = ApiConstants.EXTERNAL_ID, type = CommandType.STRING, description = "ID of the network in an external system.")
    private String externalId;

    @Parameter(name = ApiConstants.DISPLAY_NETWORK,
               type = CommandType.BOOLEAN,
 description = "an optional field, whether to the display the network to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayNetwork;

    @Parameter(name = ApiConstants.ACL_ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, description = "Network ACL ID associated for the network")
    private Long aclId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public String getGateway() {
        return gateway;
    }

    public String getIsolatedPvlan() {
        return isolatedPvlan;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getNetworkName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getAclType() {
        return aclType;
    }

    public Boolean getSubdomainAccess() {
        return subdomainAccess;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Boolean getDisplayNetwork() {
        return displayNetwork;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public boolean isDisplay() {
        if(displayNetwork == null)
            return true;
        else
            return displayNetwork;
    }

    public Long getZoneId() {
        Long physicalNetworkId = getPhysicalNetworkId();

        if (physicalNetworkId == null && zoneId == null) {
            throw new InvalidParameterValueException("Zone ID is required");
        }

        return zoneId;
    }

    public Long getPhysicalNetworkId() {
        NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, networkOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find network offering by ID " + networkOfferingId);
        }

        if (physicalNetworkId != null) {
            if (offering.getGuestType() == GuestType.Shared) {
                return physicalNetworkId;
            } else {
                throw new InvalidParameterValueException("Physical network OD can be specified for networks of guest IP type " + GuestType.Shared + " only.");
            }
        } else {
            if (zoneId == null) {
                throw new InvalidParameterValueException("ZoneId is required as physicalNetworkId is null");
            }
            return _networkService.findPhysicalNetworkId(zoneId, offering.getTags(), offering.getTrafficType());
        }
    }

    public String getStartIpv6() {
        if (startIpv6 == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(startIpv6);
    }

    public String getEndIpv6() {
        if (endIpv6 == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(endIpv6);
    }

    public String getIp6Gateway() {
        if (ip6Gateway == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(ip6Gateway);
    }

    public String getIp6Cidr() {
        if (ip6Cidr == null) {
            return null;
        }
        return NetUtils.standardizeIp6Cidr(ip6Cidr);
    }

    public Long getAclId() {
        return aclId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    // an exception thrown by createNetwork() will be caught by the dispatcher.
        public
        void execute() throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        Network result = _networkService.createGuestNetwork(this);
        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(getResponseView(), result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network");
        }
    }
}
