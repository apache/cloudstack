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
package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackZone {
    @SerializedName(ApiConstants.ID)
    private String id;
    @SerializedName(ApiConstants.ALLOCATION_STATE)
    private String allocationState;
    @SerializedName(ApiConstants.DESCRIPTION)
    private String description;
    @SerializedName(ApiConstants.DHCP_PROVIDER)
    private String dhcpProvider;
    @SerializedName(ApiConstants.DISPLAY_TEXT)
    private String displayText;
    @SerializedName(ApiConstants.DNS1)
    private String dns1;
    @SerializedName(ApiConstants.DNS2)
    private String dns2;
    @SerializedName(ApiConstants.DOMAIN)
    private String domain;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private Long domainId;
    @SerializedName(ApiConstants.GUEST_CIDR_ADDRESS)
    private String guestCidrAddress;
    @SerializedName(ApiConstants.INTERNAL_DNS1)
    private String internalDns1;
    @SerializedName(ApiConstants.INTERNAL_DNS2)
    private String internalDns2;
    @SerializedName(ApiConstants.NAME)
    private String name;
    @SerializedName(ApiConstants.NETWORK_TYPE)
    private String networkType;
    @SerializedName(ApiConstants.SECURITY_GROUPS_ENABLED)
    private Boolean securityGroupsEnabled;
    @SerializedName(ApiConstants.VLAN)
    private String vlan; 
    @SerializedName(ApiConstants.ZONE_TOKEN)
    private String zoneToken;
    
    public CloudStackZone() {
    }

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the allocationState
	 */
	public String getAllocationState() {
		return allocationState;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the dhcpProvider
	 */
	public String getDhcpProvider() {
		return dhcpProvider;
	}

	/**
	 * @return the displayText
	 */
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * @return the dns1
	 */
	public String getDns1() {
		return dns1;
	}

	/**
	 * @return the dns2
	 */
	public String getDns2() {
		return dns2;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the domainId
	 */
	public Long getDomainId() {
		return domainId;
	}

	/**
	 * @return the guestCidrAddress
	 */
	public String getGuestCidrAddress() {
		return guestCidrAddress;
	}

	/**
	 * @return the internalDns1
	 */
	public String getInternalDns1() {
		return internalDns1;
	}

	/**
	 * @return the internalDns2
	 */
	public String getInternalDns2() {
		return internalDns2;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the networkType
	 */
	public String getNetworkType() {
		return networkType;
	}

	/**
	 * @return the securityGroupsEnabled
	 */
	public Boolean getSecurityGroupsEnabled() {
		return securityGroupsEnabled;
	}

	/**
	 * @return the vlan
	 */
	public String getVlan() {
		return vlan;
	}

	/**
	 * @return the zoneToken
	 */
	public String getZoneToken() {
		return zoneToken;
	}
}
