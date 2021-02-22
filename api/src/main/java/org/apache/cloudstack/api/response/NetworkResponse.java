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
package org.apache.cloudstack.api.response;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.Network;
import com.cloud.projects.ProjectAccount;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = {Network.class, ProjectAccount.class})
public class NetworkResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the network")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the network")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "the displaytext of the network")
    private String displaytext;

    @SerializedName("broadcastdomaintype")
    @Param(description = "Broadcast domain type of the network")
    private String broadcastDomainType;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "the traffic type of the network")
    private String trafficType;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the network's gateway")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "the network's netmask")
    private String netmask;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "Cloudstack managed address space, all CloudStack managed VMs get IP address from CIDR")
    private String cidr;

    @SerializedName(ApiConstants.NETWORK_CIDR)
    @Param(description = "the network CIDR of the guest network configured with IP reservation. It is the summation of CIDR and RESERVED_IP_RANGE")
    private String networkCidr;

    @SerializedName(ApiConstants.RESERVED_IP_RANGE)
    @Param(description = "the network's IP range not to be used by CloudStack guest VMs and can be used for non CloudStack purposes")
    private String reservedIpRange;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of the network")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the zone the network belongs to")
    private String zoneName;

    @SerializedName("networkofferingid")
    @Param(description = "network offering id the network is created from")
    private String networkOfferingId;

    @SerializedName("networkofferingname")
    @Param(description = "name of the network offering the network is created from")
    private String networkOfferingName;

    @SerializedName("networkofferingdisplaytext")
    @Param(description = "display text of the network offering the network is created from")
    private String networkOfferingDisplayText;

    @SerializedName("networkofferingconservemode")
    @Param(description = "true if network offering is ip conserve mode enabled")
    private Boolean networkOfferingConserveMode;

    @SerializedName("networkofferingavailability")
    @Param(description = "availability of the network offering the network is created from")
    private String networkOfferingAvailability;

    @SerializedName(ApiConstants.IS_SYSTEM)
    @Param(description = "true if network is system, false otherwise")
    private Boolean isSystem;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the network")
    private String state;

    @SerializedName("related")
    @Param(description = "related to what other network configuration")
    private String related;

    @SerializedName("broadcasturi")
    @Param(description = "broadcast uri of the network. This parameter is visible to ROOT admins only")
    private String broadcastUri;

    @SerializedName(ApiConstants.DNS1)
    @Param(description = "the first DNS for the network")
    private String dns1;

    @SerializedName(ApiConstants.DNS2)
    @Param(description = "the second DNS for the network")
    private String dns2;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the network")
    private String type;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "The vlan of the network. This parameter is visible to ROOT admins only")
    private String vlan;

    @SerializedName(ApiConstants.ACL_TYPE)
    @Param(description = "acl type - access type to the network")
    private String aclType;

    @SerializedName(ApiConstants.SUBDOMAIN_ACCESS)
    @Param(description = "true if users from subdomains can access the domain level network")
    private Boolean subdomainAccess;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the owner of the network")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the ipaddress")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the network owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the network owner")
    private String domain;

    @SerializedName("isdefault")
    @Param(description = "true if network is default, false otherwise")
    private Boolean isDefault;

    @SerializedName("service")
    @Param(description = "the list of services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;

    @SerializedName(ApiConstants.NETWORK_DOMAIN)
    @Param(description = "the network domain")
    private String networkDomain;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network id")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.RESTART_REQUIRED)
    @Param(description = "true network requires restart")
    private Boolean restartRequired;

    @SerializedName(ApiConstants.SPECIFY_IP_RANGES)
    @Param(description = "true if network supports specifying ip ranges, false otherwise")
    private Boolean specifyIpRanges;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC the network belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "Name of the VPC to which this network belongs", since = "4.15")
    private String vpcName;

    @SerializedName(ApiConstants.CAN_USE_FOR_DEPLOY)
    @Param(description = "list networks available for vm deployment")
    private Boolean canUseForDeploy;

    @SerializedName(ApiConstants.IS_PERSISTENT)
    @Param(description = "list networks that are persistent")
    private Boolean isPersistent;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with network", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the network")
    private Map<String, String> details;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "the gateway of IPv6 network")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "the cidr of IPv6 network")
    private String ip6Cidr;

    @SerializedName(ApiConstants.DISPLAY_NETWORK)
    @Param(description = "an optional field, whether to the display the network to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayNetwork;

    @SerializedName(ApiConstants.ACL_ID)
    @Param(description = "ACL Id associated with the VPC network")
    private String aclId;

    @SerializedName(ApiConstants.ACL_NAME)
    @Param(description = "ACL name associated with the VPC network")
    private String aclName;

    @SerializedName(ApiConstants.STRECHED_L2_SUBNET)
    @Param(description = "true if network can span multiple zones", since = "4.4")
    private Boolean strechedL2Subnet;

    @SerializedName(ApiConstants.NETWORK_SPANNED_ZONES)
    @Param(description = "If a network is enabled for 'streched l2 subnet' then represents zones on which network currently spans", since = "4.4")
    private Set<String> networkSpannedZones;

    @SerializedName(ApiConstants.EXTERNAL_ID)
    @Param(description = "The external id of the network", since = "4.11")
    private String externalId;

    @SerializedName(ApiConstants.REDUNDANT_ROUTER)
    @Param(description = "If the network has redundant routers enabled", since = "4.11.1")
    private Boolean redundantRouter;

    public Boolean getDisplayNetwork() {
        return displayNetwork;
    }

    public void setDisplayNetwork(Boolean displayNetwork) {
        this.displayNetwork = displayNetwork;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBroadcastDomainType(String broadcastDomainType) {
        this.broadcastDomainType = broadcastDomainType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setNetworkOfferingId(String networkOfferingId) {
        this.networkOfferingId = networkOfferingId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setNetworkOfferingName(String networkOfferingName) {
        this.networkOfferingName = networkOfferingName;
    }

    public void setNetworkOfferingDisplayText(String networkOfferingDisplayText) {
        this.networkOfferingDisplayText = networkOfferingDisplayText;
    }

    public void setNetworkOfferingConserveMode(Boolean networkOfferingConserveMode) {
        this.networkOfferingConserveMode = networkOfferingConserveMode;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    @Override
    public void setDomainName(String domain) {
        this.domain = domain;
    }

    public void setNetworkOfferingAvailability(String networkOfferingAvailability) {
        this.networkOfferingAvailability = networkOfferingAvailability;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setAclType(String aclType) {
        this.aclType = aclType;
    }

    public void setSubdomainAccess(Boolean subdomainAccess) {
        this.subdomainAccess = subdomainAccess;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public void setReservedIpRange(String reservedIpRange) {
        this.reservedIpRange = reservedIpRange;
    }

    public void setRestartRequired(Boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    public void setSpecifyIpRanges(Boolean specifyIpRanges) {
        this.specifyIpRanges = specifyIpRanges;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setCanUseForDeploy(Boolean canUseForDeploy) {
        this.canUseForDeploy = canUseForDeploy;
    }

    public void setIsPersistent(Boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }

    public String getAclId() {
        return aclId;
    }

    public void setAclId(String aclId) {
        this.aclId = aclId;
    }

    public String getAclName() {
        return aclName;
    }

    public void setAclName(String aclName) {
        this.aclName = aclName;
    }

    public void setStrechedL2Subnet(Boolean strechedL2Subnet) {
        this.strechedL2Subnet = strechedL2Subnet;
    }

    public void setNetworkSpannedZones(Set<String> networkSpannedZones) {
        this.networkSpannedZones = networkSpannedZones;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Boolean getRedundantRouter() {
        return redundantRouter;
    }

    public void setRedundantRouter(Boolean redundantRouter) {
        this.redundantRouter = redundantRouter;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }
}
