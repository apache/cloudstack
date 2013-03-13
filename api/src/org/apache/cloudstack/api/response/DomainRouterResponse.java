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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=VirtualMachine.class)
@SuppressWarnings("unused")
public class DomainRouterResponse extends BaseResponse implements ControlledViewEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the id of the router")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID for the router")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the Zone name for the router")
    private String zoneName;

    @SerializedName(ApiConstants.DNS1) @Param(description="the first DNS for the router")
    private String dns1;

    @SerializedName(ApiConstants.DNS2) @Param(description="the second DNS for the router")
    private String dns2;

    @SerializedName(ApiConstants.IP6_DNS1) @Param(description="the first IPv6 DNS for the router")
    private String ip6Dns1;

    @SerializedName(ApiConstants.IP6_DNS2) @Param(description="the second IPv6 DNS for the router")
    private String ip6Dns2;

    @SerializedName("networkdomain") @Param(description="the network domain for the router")
    private String networkDomain;

    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway for the router")
    private String gateway;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the router")
    private String name;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID for the router")
    private String podId;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the host ID for the router")
    private String hostId;

    @SerializedName("hostname") @Param(description="the hostname for the router")
    private String hostName;

    @SerializedName(ApiConstants.LINK_LOCAL_IP) @Param(description="the link local IP address for the router")
    private String linkLocalIp;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_ADDRESS) @Param(description="the link local MAC address for the router")
    private String linkLocalMacAddress;

    @SerializedName(ApiConstants.LINK_LOCAL_MAC_NETMASK) @Param(description="the link local netmask for the router")
    private String linkLocalNetmask;

    @SerializedName(ApiConstants.LINK_LOCAL_NETWORK_ID) @Param(description="the ID of the corresponding link local network")
    private String linkLocalNetworkId;

    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public IP address for the router")
    private String publicIp;

    @SerializedName("publicmacaddress") @Param(description="the public MAC address for the router")
    private String publicMacAddress;

    @SerializedName("publicnetmask") @Param(description="the public netmask for the router")
    private String publicNetmask;

    @SerializedName("publicnetworkid") @Param(description="the ID of the corresponding public network")
    private String publicNetworkId;

    @SerializedName("guestipaddress") @Param(description="the guest IP address for the router")
    private String guestIpAddress;

    @SerializedName("guestmacaddress") @Param(description="the guest MAC address for the router")
    private String guestMacAddress;

    @SerializedName("guestnetmask") @Param(description="the guest netmask for the router")
    private String guestNetmask;

    @SerializedName("guestnetworkid") @Param(description="the ID of the corresponding guest network")
    private String guestNetworkId;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="the template ID for the router")
    private String templateId;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date and time the router was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the router")
    private State state;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the router")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID associated with the router")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain associated with the router")
    private String domainName;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID) @Param(description="the ID of the service offering of the virtual machine")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname") @Param(description="the name of the service offering of the virtual machine")
    private String serviceOfferingName;

    @SerializedName("isredundantrouter") @Param(description="if this router is an redundant virtual router")
    private boolean isRedundantRouter;

    @SerializedName("redundantstate") @Param(description="the state of redundant virtual router")
    private String redundantState;

    @SerializedName("templateversion") @Param(description="the version of template")
    private String templateVersion;

    @SerializedName("scriptsversion") @Param(description="the version of scripts")
    private String scriptsVersion;

    @SerializedName(ApiConstants.VPC_ID) @Param(description="VPC the network belongs to")
    private String vpcId;

    @SerializedName("nic")  @Param(description="the list of nics associated with the router",
            responseObject = NicResponse.class, since="4.0")
    private Set<NicResponse> nics;

    public DomainRouterResponse(){
        nics = new LinkedHashSet<NicResponse>();
    }



    @Override
    public String getObjectId() {
        return this.getId();
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public void setGuestIpAddress(String guestIpAddress) {
        this.guestIpAddress = guestIpAddress;
    }

    public void setGuestMacAddress(String guestMacAddress) {
        this.guestMacAddress = guestMacAddress;
    }

    public void setGuestNetmask(String guestNetmask) {
        this.guestNetmask = guestNetmask;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setPublicNetworkId(String publicNetworkId) {
        this.publicNetworkId = publicNetworkId;
    }

    public void setGuestNetworkId(String guestNetworkId) {
        this.guestNetworkId = guestNetworkId;
    }

    public void setLinkLocalIp(String linkLocalIp) {
        this.linkLocalIp = linkLocalIp;
    }

    public void setLinkLocalMacAddress(String linkLocalMacAddress) {
        this.linkLocalMacAddress = linkLocalMacAddress;
    }

    public void setLinkLocalNetmask(String linkLocalNetmask) {
        this.linkLocalNetmask = linkLocalNetmask;
    }

    public void setLinkLocalNetworkId(String linkLocalNetworkId) {
        this.linkLocalNetworkId = linkLocalNetworkId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setRedundantState(String redundantState) {
        this.redundantState = redundantState;
    }

    public void setIsRedundantRouter(boolean isRedundantRouter) {
        this.isRedundantRouter = isRedundantRouter;
    }

    public String getTemplateVersion() {
        return this.templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getScriptsVersion() {
        return this.scriptsVersion;
    }

    public void setScriptsVersion(String scriptsVersion) {
        this.scriptsVersion = scriptsVersion;
    }
    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setNics(Set<NicResponse> nics) {
        this.nics = nics;
    }

    public void addNic(NicResponse nic) {
        this.nics.add(nic);
    }

	public String getIp6Dns1() {
		return ip6Dns1;
	}

	public void setIp6Dns1(String ip6Dns1) {
		this.ip6Dns1 = ip6Dns1;
	}

	public String getIp6Dns2() {
		return ip6Dns2;
	}

	public void setIp6Dns2(String ip6Dns2) {
		this.ip6Dns2 = ip6Dns2;
	}
}
