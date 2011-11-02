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

public class ZoneResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="Zone id")
    private IdentityProxy id = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.NAME) @Param(description="Zone name")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="Zone description")
    private String description;

    @SerializedName(ApiConstants.DNS1) @Param(description="the first DNS for the Zone")
    private String dns1;

    @SerializedName(ApiConstants.DNS2) @Param(description="the second DNS for the Zone")
    private String dns2;

    @SerializedName(ApiConstants.INTERNAL_DNS1) @Param(description="the first internal DNS for the Zone")
    private String internalDns1;

    @SerializedName(ApiConstants.INTERNAL_DNS2) @Param(description="the second internal DNS for the Zone")
    private String internalDns2;
    
    @SerializedName(ApiConstants.VLAN) @Param(description="the vlan range of the zone")
    private String vlan;

    @SerializedName(ApiConstants.GUEST_CIDR_ADDRESS) @Param(description="the guest CIDR address for the Zone")
    private String guestCidrAddress;
    
    //TODO - generate description
    @SerializedName("status")
    private String status;

    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="the display text of the zone")
    private String displayText;
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="Network domain name for the networks in the zone")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the containing domain, null for public zones")
    private Long domainId;
    
    @SerializedName(ApiConstants.NETWORK_TYPE) @Param(description="the network type of the zone; can be Basic or Advanced")
    private String networkType;
    
    @SerializedName("securitygroupsenabled") @Param(description="true if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;
    
    @SerializedName("allocationstate") @Param(description="the allocation state of the cluster")
    private String allocationState; 
    
    @SerializedName(ApiConstants.ZONE_TOKEN) @Param(description="Zone Token")
    private String zoneToken;    
    
    @SerializedName(ApiConstants.DHCP_PROVIDER) @Param(description="the dhcp Provider for the Zone")
    private String dhcpProvider;     
    
    @SerializedName("capacity")  @Param(description="the capacity of the Zone", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacitites;

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDns1() {
        return dns1;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public void setInternalDns1(String internalDns1) {
        this.internalDns1 = internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public void setInternalDns2(String internalDns2) {
        this.internalDns2 = internalDns2;
    }

    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getGuestCidrAddress() {
        return guestCidrAddress;
    }

    public void setGuestCidrAddress(String guestCidrAddress) {
        this.guestCidrAddress = guestCidrAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public Long getDomainId() {
		return domainId;
	}

	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}
	
    public String getNetworkType() {
        return networkType;
    }

    public void setType(String networkType) {
        this.networkType = networkType;
    }

    public boolean getSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public void setSecurityGroupsEnabled(boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }
    
    public String getAllocationState() {
    	return allocationState;
    }
    
    public void setAllocationState(String allocationState) {
    	this.allocationState = allocationState;
    }
    
    public String getZoneToken() {
	    return zoneToken;
	}
	
	public void setZoneToken(String zoneToken) {
		this.zoneToken = zoneToken;
	}
	
    public String getDhcpProvider() {
        return dhcpProvider;
    } 
 	
 	public void setDhcpProvider(String dhcpProvider) {
 		this.dhcpProvider = dhcpProvider;
 	}

	public List<CapacityResponse> getCapacitites() {
		return capacitites;
	}

	public void setCapacitites(List<CapacityResponse> capacitites) {
		this.capacitites = capacitites;
	}	
}
