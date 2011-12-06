/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class NetworkResponse extends BaseResponse implements ControlledEntityResponse{
    
    @SerializedName(ApiConstants.ID) @Param(description="the id of the network")
    private IdentityProxy id = new IdentityProxy("networks");
    
    @SerializedName(ApiConstants.NAME) @Param(description="the name of the network")
    private String name;
    
    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="the displaytext of the network")
    private String displaytext;
    
    @SerializedName("broadcastdomaintype") @Param(description="Broadcast domain type of the network")
    private String broadcastDomainType;
    
    @SerializedName(ApiConstants.TRAFFIC_TYPE) @Param(description="the traffic type of the network")
    private String trafficType;
    
    @SerializedName(ApiConstants.GATEWAY) @Param(description="the network's gateway")
    private String gateway;
    
    @SerializedName(ApiConstants.NETMASK) @Param(description="the network's netmask")
    private String netmask;
    
    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the network")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the network")
    private String endIp;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="zone id of the network")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
    
    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the name of the zone the network belongs to")
    private String zoneName;
    
    @SerializedName("networkofferingid") @Param(description="network offering id the network is created from")
    private IdentityProxy networkOfferingId = new IdentityProxy("network_offerings");
    
    @SerializedName("networkofferingname") @Param(description="name of the network offering the network is created from")
    private String networkOfferingName;
    
    @SerializedName("networkofferingdisplaytext") @Param(description="display text of the network offering the network is created from")
    private String networkOfferingDisplayText;
    
    @SerializedName("networkofferingavailability") @Param(description="availability of the network offering the network is created from")
    private String networkOfferingAvailability;
    
    @SerializedName(ApiConstants.IS_SYSTEM) @Param(description="true if network is system, false otherwise")
    private Boolean isSystem;
    
    @SerializedName(ApiConstants.STATE) @Param(description="state of the network")
    private String state;

    @SerializedName("related") @Param(description="related to what other network configuration")
    private IdentityProxy related = new IdentityProxy("networks");
    
    @SerializedName("broadcasturi") @Param(description="broadcast uri of the network")
    private String broadcastUri;
    
    @SerializedName(ApiConstants.DNS1) @Param(description="the first DNS for the network")
    private String dns1;
    
    @SerializedName(ApiConstants.DNS2) @Param(description="the second DNS for the network")
    private String dns2;
    
    @SerializedName(ApiConstants.TYPE) @Param(description="the type of the network")
    private String type;
    
    @SerializedName(ApiConstants.VLAN) @Param(description="the vlan of the network")
    private String vlan;
    
    @SerializedName(ApiConstants.ACL_TYPE) @Param(description="acl type - access type to the network")
    private String aclType;
    
    @SerializedName(ApiConstants.SUBDOMAIN_ACCESS) @Param(description="true if users from subdomains can access the domain level network")
    private Boolean subdomainAccess;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the owner of the network")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the network owner")
    private IdentityProxy domainId = new IdentityProxy("domain");
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the network owner")
    private String domain;
    
    @SerializedName("isdefault") @Param(description="true if network is default, false otherwise")
    private Boolean isDefault;
    
    @SerializedName("service") @Param(description="the list of services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;
    
    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network id")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");
    
    public void setId(Long id) {
        this.id.setValue(id);
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
    
    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public void setNetworkOfferingId(Long networkOfferingId) {
        this.networkOfferingId.setValue(networkOfferingId);
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setRelated(Long related) {
        this.related.setValue(related);
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

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setNetworkOfferingName(String networkOfferingName) {
        this.networkOfferingName = networkOfferingName;
    }

    public void setNetworkOfferingDisplayText(String networkOfferingDisplayText) {
        this.networkOfferingDisplayText = networkOfferingDisplayText;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

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
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
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
}
