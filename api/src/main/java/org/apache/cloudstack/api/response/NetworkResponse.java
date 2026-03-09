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

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAssociatedNetwork;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.Network;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = {Network.class})
public class NetworkResponse extends BaseResponseWithAssociatedNetwork implements ControlledEntityResponse, SetResourceIconResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Network")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the Network")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "The displaytext of the Network")
    private String displaytext;

    @SerializedName("broadcastdomaintype")
    @Param(description = "Broadcast domain type of the Network")
    private String broadcastDomainType;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "The traffic type of the Network")
    private String trafficType;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "The Network's gateway")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "The Network's netmask")
    private String netmask;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "CloudStack managed address space, all CloudStack managed Instances get IP address from CIDR")
    private String cidr;

    @SerializedName(ApiConstants.NETWORK_CIDR)
    @Param(description = "The Network CIDR of the guest Network configured with IP reservation. It is the summation of CIDR and RESERVED_IP_RANGE")
    private String networkCidr;

    @SerializedName(ApiConstants.RESERVED_IP_RANGE)
    @Param(description = "The Network's IP range not to be used by CloudStack guest Instances and can be used for non CloudStack purposes")
    private String reservedIpRange;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone ID of the Network")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The name of the zone the Network belongs to")
    private String zoneName;

    @SerializedName("networkofferingid")
    @Param(description = "Network offering ID the Network is created from")
    private String networkOfferingId;

    @SerializedName("networkofferingname")
    @Param(description = "Name of the Network offering the Network is created from")
    private String networkOfferingName;

    @SerializedName("networkofferingdisplaytext")
    @Param(description = "Display text of the Network offering the Network is created from")
    private String networkOfferingDisplayText;

    @SerializedName("networkofferingconservemode")
    @Param(description = "True if Network offering is IP conserve mode enabled")
    private Boolean networkOfferingConserveMode;

    @SerializedName("networkofferingavailability")
    @Param(description = "Availability of the Network offering the Network is created from")
    private String networkOfferingAvailability;

    @SerializedName(ApiConstants.IS_SYSTEM)
    @Param(description = "True if Network is system, false otherwise")
    private Boolean isSystem;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of the Network")
    private String state;

    @SerializedName("related")
    @Param(description = "Related to what other Network configuration")
    private String related;

    @SerializedName("broadcasturi")
    @Param(description = "Broadcast URI of the Network. This parameter is visible to ROOT admins only")
    private String broadcastUri;

    @SerializedName(ApiConstants.DNS1)
    @Param(description = "The first IPv4 DNS for the Network")
    private String dns1;

    @SerializedName(ApiConstants.DNS2)
    @Param(description = "The second IPv4 DNS for the Network")
    private String dns2;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The type of the Network")
    private String type;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "The VLAN of the Network. This parameter is visible to ROOT admins only")
    private String vlan;

    @SerializedName(ApiConstants.AS_NUMBER_ID)
    @Param(description = "UUID of AS NUMBER", since = "4.20.0")
    private String asNumberId;

    @SerializedName(ApiConstants.AS_NUMBER)
    @Param(description = "AS NUMBER", since = "4.20.0")
    private Long asNumber;

    @SerializedName(ApiConstants.ACL_TYPE)
    @Param(description = "ACL type - access type to the Network")
    private String aclType;

    @SerializedName(ApiConstants.SUBDOMAIN_ACCESS)
    @Param(description = "True if users from subdomains can access the domain level Network")
    private Boolean subdomainAccess;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The owner of the Network")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the IP address")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the Network owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain name of the Network owner")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the Domain the network belongs to", since = "4.19.0.0")
    private String domainPath;

    @SerializedName("isdefault")
    @Param(description = "True if Network is default, false otherwise")
    private Boolean isDefault;

    @SerializedName("service")
    @Param(description = "The list of services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;

    @SerializedName(ApiConstants.NETWORK_DOMAIN)
    @Param(description = "The Network domain")
    private String networkDomain;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "The physical Network id")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.RESTART_REQUIRED)
    @Param(description = "True if Network requires restart")
    private Boolean restartRequired;

    @SerializedName(ApiConstants.SPECIFY_VLAN)
    @Param(description = "True if network supports specifying vlan, false otherwise")
    private Boolean specifyVlan;

    @SerializedName(ApiConstants.SPECIFY_IP_RANGES)
    @Param(description = "True if Network supports specifying IP ranges, false otherwise")
    private Boolean specifyIpRanges;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC the Network belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "Name of the VPC to which this Network belongs", since = "4.15")
    private String vpcName;

    @SerializedName(ApiConstants.TUNGSTEN_VIRTUAL_ROUTER_UUID)
    @Param(description = "Tungsten-Fabric virtual router the Network belongs to")
    private String tungstenVirtualRouterUuid;

    @SerializedName(ApiConstants.CAN_USE_FOR_DEPLOY)
    @Param(description = "List Networks available for Instance deployment")
    private Boolean canUseForDeploy;

    @SerializedName(ApiConstants.IS_PERSISTENT)
    @Param(description = "List Networks that are persistent")
    private Boolean isPersistent;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "The list of resource tags associated with Network", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "The details of the Network")
    private Map<String, String> details;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "The gateway of IPv6 Network")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "The CIDR of IPv6 Network")
    private String ip6Cidr;

    @SerializedName(ApiConstants.DISPLAY_NETWORK)
    @Param(description = "An optional field, whether to the display the Network to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayNetwork;

    @SerializedName(ApiConstants.ACL_ID)
    @Param(description = "ACL ID associated with the VPC Network")
    private String aclId;

    @SerializedName(ApiConstants.ACL_NAME)
    @Param(description = "ACL name associated with the VPC Network")
    private String aclName;

    @SerializedName(ApiConstants.STRECHED_L2_SUBNET)
    @Param(description = "True if Network can span multiple zones", since = "4.4")
    private Boolean strechedL2Subnet;

    @SerializedName(ApiConstants.NETWORK_SPANNED_ZONES)
    @Param(description = "If a Network is enabled for 'stretched L2 subnet' then represents zones on which Network currently spans", since = "4.4")
    private Set<String> networkSpannedZones;

    @SerializedName(ApiConstants.EXTERNAL_ID)
    @Param(description = "The external ID of the Network", since = "4.11")
    private String externalId;

    @SerializedName(ApiConstants.REDUNDANT_ROUTER)
    @Param(description = "If the Network has redundant routers enabled", since = "4.11.1")
    private Boolean redundantRouter;

    @SerializedName(ApiConstants.SUPPORTS_VM_AUTOSCALING)
    @Param(description = "If Network offering supports Instance autoscaling feature", since = "4.18.0")
    private Boolean supportsVmAutoScaling;

    @SerializedName(ApiConstants.RESOURCE_ICON)
    @Param(description = "Base64 string representation of the resource icon", since = "4.16.0.0")
    ResourceIconResponse icon;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date this Network was created", since = "4.16.0")
    private Date created;

    @SerializedName(ApiConstants.RECEIVED_BYTES)
    @Param(description = "The total number of Network traffic bytes received")
    private Long bytesReceived;

    @SerializedName(ApiConstants.SENT_BYTES)
    @Param(description = "The total number of Network traffic bytes sent")
    private Long bytesSent;

    @SerializedName((ApiConstants.EGRESS_DEFAULT_POLICY))
    @Param(description = "True if guest Network default egress policy is allow; false if default egress policy is deny")
    private Boolean egressDefaultPolicy;

    @SerializedName(ApiConstants.INTERNET_PROTOCOL)
    @Param(description = "The internet protocol of Network offering")
    private String internetProtocol;

    @SerializedName(ApiConstants.IPV6_ROUTING)
    @Param(description = "The IPv6 routing type of network offering", since = "4.17.0")
    private String ipv6Routing;

    @SerializedName(ApiConstants.IPV6_ROUTES)
    @Param(description = "The routes for the Network to ease adding route in upstream router", since = "4.17.0")
    private Set<Ipv6RouteResponse> ipv6Routes;

    @SerializedName(ApiConstants.PUBLIC_MTU)
    @Param(description = "MTU configured on the Network VR's public facing interfaces")
    private Integer publicMtu;

    @SerializedName(ApiConstants.PRIVATE_MTU)
    @Param(description = "MTU configured on the network VR's private interfaces")
    private Integer privateMtu;

    @SerializedName(ApiConstants.IP6_DNS1)
    @Param(description = "The first IPv6 DNS for the network", since = "4.18.0")
    private String ipv6Dns1;

    @SerializedName(ApiConstants.IP6_DNS2)
    @Param(description = "The second IPv6 DNS for the network", since = "4.18.0")
    private String ipv6Dns2;

    @SerializedName(ApiConstants.IPV4_ROUTING)
    @Param(description = "The IPv4 routing type of network", since = "4.20.0")
    private String ipv4Routing;

    @SerializedName(ApiConstants.IPV4_ROUTES)
    @Param(description = "The routes for the network to ease adding route in upstream router", since = "4.20.0")
    private Set<Ipv4RouteResponse> ipv4Routes;

    @SerializedName(ApiConstants.BGP_PEERS)
    @Param(description = "The BGP peers for the network", since = "4.20.0")
    private Set<BgpPeerResponse> bgpPeers;

    public NetworkResponse() {}

    public Boolean getDisplayNetwork() {
        return displayNetwork;
    }

    public void setDisplayNetwork(Boolean displayNetwork) {
        this.displayNetwork = displayNetwork;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
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

    public void setAsNumber(long asNumber) {
        this.asNumber = asNumber;
    }

    public void setAsNumberId(String asNumberId) {
        this.asNumberId = asNumberId;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    @Override
    public void setDomainName(String domain) {
        this.domain = domain;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
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

    public void setSpecifyVlan(Boolean specifyVlan) {
        this.specifyVlan = specifyVlan;
    }

    public void setSpecifyIpRanges(Boolean specifyIpRanges) {
        this.specifyIpRanges = specifyIpRanges;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcId() {
        return vpcId;
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

    public String getTungstenVirtualRouterUuid() {
        return tungstenVirtualRouterUuid;
    }

    public Boolean getSupportsVmAutoScaling() {
        return supportsVmAutoScaling;
    }

    public void setSupportsVmAutoScaling(Boolean supportsVmAutoScaling) {
        this.supportsVmAutoScaling = supportsVmAutoScaling;
    }

    public void setTungstenVirtualRouterUuid(String tungstenVirtualRouterUuid) {
        this.tungstenVirtualRouterUuid = tungstenVirtualRouterUuid;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    @Override
    public void setResourceIconResponse(ResourceIconResponse icon) {
        this.icon = icon;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public void setBytesSent(final Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public boolean getEgressDefaultPolicy() {
        return egressDefaultPolicy;
    }

    public void setEgressDefaultPolicy(Boolean egressDefaultPolicy) {
        this.egressDefaultPolicy = egressDefaultPolicy;
    }

    public void setInternetProtocol(String internetProtocol) {
        this.internetProtocol = internetProtocol;
    }

    public void setIpv4Routing(String ipv4Routing) {
        this.ipv4Routing = ipv4Routing;
    }

    public void setIpv4Routes(Set<Ipv4RouteResponse> ipv4Routes) {
        this.ipv4Routes = ipv4Routes;
    }

    public void addIpv4Route(Ipv4RouteResponse ipv4Route) {
        this.ipv4Routes.add(ipv4Route);
    }

    public void setIpv6Routing(String ipv6Routing) {
        this.ipv6Routing = ipv6Routing;
    }

    public void setIpv6Routes(Set<Ipv6RouteResponse> ipv6Routes) {
        this.ipv6Routes = ipv6Routes;
    }

    public void addIpv6Route(Ipv6RouteResponse ipv6Route) {
        this.ipv6Routes.add(ipv6Route);
    }

    public void setBgpPeers(Set<BgpPeerResponse> bgpPeers) {
        this.bgpPeers = bgpPeers;
    }

    public void addBgpPeer(BgpPeerResponse bgpPeer) {
        if (this.bgpPeers == null) {
            this.setBgpPeers(new LinkedHashSet<>());
        }
        this.bgpPeers.add(bgpPeer);
    }

    public Integer getPublicMtu() {
        return publicMtu;
    }

    public void setPublicMtu(Integer publicMtu) {
        this.publicMtu = publicMtu;
    }

    public Integer getPrivateMtu() {
        return privateMtu;
    }

    public void setPrivateMtu(Integer privateMtu) {
        this.privateMtu = privateMtu;
    }

    public void setIpv6Dns1(String ipv6Dns1) {
        this.ipv6Dns1 = ipv6Dns1;
    }

    public void setIpv6Dns2(String ipv6Dns2) {
        this.ipv6Dns2 = ipv6Dns2;
    }
}
