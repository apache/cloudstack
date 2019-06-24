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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = DataCenter.class)
public class ZoneResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "Zone id")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Zone name")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Zone description")
    private String description;

    @SerializedName(ApiConstants.DNS1)
    @Param(description = "the first DNS for the Zone")
    private String dns1;

    @SerializedName(ApiConstants.DNS2)
    @Param(description = "the second DNS for the Zone")
    private String dns2;

    @SerializedName(ApiConstants.IP6_DNS1)
    @Param(description = "the first IPv6 DNS for the Zone")
    private String ip6Dns1;

    @SerializedName(ApiConstants.IP6_DNS2)
    @Param(description = "the second IPv6 DNS for the Zone")
    private String ip6Dns2;

    @SerializedName(ApiConstants.INTERNAL_DNS1)
    @Param(description = "the first internal DNS for the Zone")
    private String internalDns1;

    @SerializedName(ApiConstants.INTERNAL_DNS2)
    @Param(description = "the second internal DNS for the Zone")
    private String internalDns2;

    @SerializedName(ApiConstants.GUEST_CIDR_ADDRESS)
    @Param(description = "the guest CIDR address for the Zone")
    private String guestCidrAddress;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "the display text of the zone")
    private String displayText;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "Network domain name for the networks in the zone")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the UUID of the containing domain, null for public zones")
    private String domainId;

    @SerializedName("domainname")
    @Param(description = "the name of the containing domain, null for public zones")
    private String domainName;

    @SerializedName(ApiConstants.NETWORK_TYPE)
    @Param(description = "the network type of the zone; can be Basic or Advanced")
    private String networkType;

    @SerializedName("securitygroupsenabled")
    @Param(description = "true if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;

    @SerializedName("allocationstate")
    @Param(description = "the allocation state of the cluster")
    private String allocationState;

    @SerializedName(ApiConstants.ZONE_TOKEN)
    @Param(description = "Zone Token")
    private String zoneToken;

    @SerializedName(ApiConstants.DHCP_PROVIDER)
    @Param(description = "the dhcp Provider for the Zone")
    private String dhcpProvider;

    @SerializedName("capacity")
    @Param(description = "the capacity of the Zone", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacitites;

    @SerializedName(ApiConstants.LOCAL_STORAGE_ENABLED)
    @Param(description = "true if local storage offering enabled, false otherwise")
    private boolean localStorageEnabled;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with zone.", responseObject = ResourceTagResponse.class, since = "4.3")
    private Set<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.RESOURCE_DETAILS)
    @Param(description = "Meta data associated with the zone (key/value pairs)", since = "4.3.0")
    private Map<String, String> resourceDetails;

    public ZoneResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setInternalDns1(String internalDns1) {
        this.internalDns1 = internalDns1;
    }

    public void setInternalDns2(String internalDns2) {
        this.internalDns2 = internalDns2;
    }

    public void setGuestCidrAddress(String guestCidrAddress) {
        this.guestCidrAddress = guestCidrAddress;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setType(String networkType) {
        this.networkType = networkType;
    }

    public void setSecurityGroupsEnabled(boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    public void setZoneToken(String zoneToken) {
        this.zoneToken = zoneToken;
    }

    public void setDhcpProvider(String dhcpProvider) {
        this.dhcpProvider = dhcpProvider;
    }

    public void setCapacitites(List<CapacityResponse> capacitites) {
        this.capacitites = capacitites;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setLocalStorageEnabled(boolean localStorageEnabled) {
        this.localStorageEnabled = localStorageEnabled;
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

    public void addTag(ResourceTagResponse tag) {
        this.tags.add(tag);
    }

    public void setResourceDetails(Map<String, String> details) {
        if (details == null) {
            return;
        }
        this.resourceDetails = new HashMap<>(details);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public String getGuestCidrAddress() {
        return guestCidrAddress;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getDomain() {
        return domain;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getNetworkType() {
        return networkType;
    }

    public boolean isSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public String getZoneToken() {
        return zoneToken;
    }

    public String getDhcpProvider() {
        return dhcpProvider;
    }

    public List<CapacityResponse> getCapacitites() {
        return capacitites;
    }

    public boolean isLocalStorageEnabled() {
        return localStorageEnabled;
    }

    public Set<ResourceTagResponse> getTags() {
        return tags;
    }

    public Map<String, String> getResourceDetails() {
        return resourceDetails;
    }
}
