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

import com.cloud.network.NetworkService;
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
import org.apache.commons.lang3.StringUtils;

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

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the display text of the network")
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

    @Parameter(name = ApiConstants.ISOLATED_PVLAN_TYPE, type = CommandType.STRING,
            description = "the isolated private VLAN type for this network")
    private String isolatedPvlanType;

    @Parameter(name = ApiConstants.NETWORK_DOMAIN, type = CommandType.STRING, description = "network domain")
    private String networkDomain;

    @Parameter(name = ApiConstants.ACL_TYPE, type = CommandType.STRING, description = "Access control type; supported values"
        + " are account and domain. In 3.0 all shared networks should have aclType=Domain, and all isolated networks"
        + " - Account. Account means that only the account owner can use the network, domain - all accounts in the domain can use the network")
    private String aclType;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account that will own the network. Account should be under the selected domain")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the network")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning a network. " +
            "If no account is provided then network will be assigned to the caller account and domain")
    private Long domainId;

    @Parameter(name = ApiConstants.SUBDOMAIN_ACCESS,
               type = CommandType.BOOLEAN,
               description = "Defines whether to allow"
                   + " subdomains to use networks dedicated to their parent domain(s). Should be used with aclType=Domain, defaulted to allow.subdomain.network.access global config if not specified")
    private Boolean subdomainAccess;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "the VPC network belongs to")
    private Long vpcId;

    @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_ROUTER_UUID, type = CommandType.STRING, description = "Tungsten-Fabric virtual router the network belongs to")
    private String tungstenVirtualRouterUuid;

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

    @Parameter(name = ApiConstants.ASSOCIATED_NETWORK_ID,
            type = CommandType.UUID,
            entityType = NetworkResponse.class,
            since = "4.17.0",
            description = "The network this network is associated to. only available if create a Shared network")
    private Long associatedNetworkId;

    @Parameter(name = ApiConstants.PUBLIC_MTU, type = CommandType.INTEGER,
            description = "MTU to be configured on the network VR's public facing interfaces", since = "4.18.0")
    private Integer publicMtu;

    @Parameter(name = ApiConstants.PRIVATE_MTU, type = CommandType.INTEGER,
            description = "MTU to be configured on the network VR's private interface(s)", since = "4.18.0")
    private Integer privateMtu;

    @Parameter(name = ApiConstants.DNS1, type = CommandType.STRING, description = "the first IPv4 DNS for the network", since = "4.18.0")
    private String ip4Dns1;

    @Parameter(name = ApiConstants.DNS2, type = CommandType.STRING, description = "the second IPv4 DNS for the network", since = "4.18.0")
    private String ip4Dns2;

    @Parameter(name = ApiConstants.IP6_DNS1, type = CommandType.STRING, description = "the first IPv6 DNS for the network", since = "4.18.0")
    private String ip6Dns1;

    @Parameter(name = ApiConstants.IP6_DNS2, type = CommandType.STRING, description = "the second IPv6 DNS for the network", since = "4.18.0")
    private String ip6Dns2;

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
        return StringUtils.isEmpty(displayText) ? name : displayText;
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

    public String getIsolatedPvlanType() {
        return isolatedPvlanType;
    }

    public Long getAssociatedNetworkId() {
        return associatedNetworkId;
    }

    public String getTungstenVirtualRouterUuid() {
        return tungstenVirtualRouterUuid;
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

        Network associatedNetwork = null;
        if (associatedNetworkId != null) {
            associatedNetwork = _entityMgr.findById(Network.class, associatedNetworkId);
            if (associatedNetwork == null) {
                throw new InvalidParameterValueException("Unable to find network by ID " + associatedNetworkId);
            }
            if (offering.getGuestType() != GuestType.Shared) {
                throw new InvalidParameterValueException("Associated network ID can be specified for networks of guest IP type " + GuestType.Shared + " only.");
            }
            if (zoneId != null && associatedNetwork.getDataCenterId() != zoneId) {
                throw new InvalidParameterValueException("The network can only be created in the same zone as the associated network");
            } else if (zoneId == null) {
                zoneId = associatedNetwork.getDataCenterId();
            }
            if (physicalNetworkId != null && !physicalNetworkId.equals(associatedNetwork.getPhysicalNetworkId())) {
                throw new InvalidParameterValueException("The network can only be created on the same physical network as the associated network");
            } else if (physicalNetworkId == null) {
                physicalNetworkId = associatedNetwork.getPhysicalNetworkId();
            }
        }
        if (physicalNetworkId != null) {
            if ((offering.getGuestType() == GuestType.Shared) || (offering.getGuestType() == GuestType.L2)) {
                return physicalNetworkId;
            } else {
                throw new InvalidParameterValueException("Physical network ID can be specified for networks of guest IP type " + GuestType.Shared + " or " + GuestType.L2 + " only.");
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

    public Integer getPublicMtu() {
        return publicMtu != null ? publicMtu : NetworkService.DEFAULT_MTU;
    }

    public Integer getPrivateMtu() {
        return privateMtu != null ? privateMtu : NetworkService.DEFAULT_MTU;
    }
    public String getIp4Dns1() {
        return ip4Dns1;
    }

    public String getIp4Dns2() {
        return ip4Dns2;
    }

    public String getIp6Dns1() {
        return ip6Dns1;
    }

    public String getIp6Dns2() {
        return ip6Dns2;
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
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
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
