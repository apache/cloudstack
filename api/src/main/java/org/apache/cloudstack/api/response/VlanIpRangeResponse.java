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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.Vlan;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Vlan.class)
@SuppressWarnings("unused")
public class VlanIpRangeResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the VLAN IP range")
    private String id;

    @SerializedName("forvirtualnetwork")
    @Param(description = "The virtual Network for the VLAN IP range")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The Zone ID of the VLAN IP range")
    private String zoneId;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "The ID or VID of the VLAN.")
    private String vlan;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account of the VLAN IP range")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the VLAN IP range")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain name of the VLAN IP range")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the domain to which the VLAN IP range belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "The Pod ID for the VLAN IP range")
    private String podId;

    @SerializedName("podname")
    @Param(description = "The Pod name for the VLAN IP range")
    private String podName;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the VLAN IP range")
    private String description;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "The gateway of the VLAN IP range")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "The netmask of the VLAN IP range")
    private String netmask;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "The CIDR of the VLAN IP range")
    private String cidr;

    @SerializedName(ApiConstants.START_IP)
    @Param(description = "The start IP of the VLAN IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP)
    @Param(description = "The end IP of the VLAN IP range")
    private String endIp;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "The Network ID of VLAN range")
    private String networkId;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the VLAN range")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the VLAN range")
    private String projectName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "The physical Network this belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.START_IPV6)
    @Param(description = "The start IPv6 of the VLAN IP range")
    private String startIpv6;

    @SerializedName(ApiConstants.END_IPV6)
    @Param(description = "The end IPv6 of the VLAN IP range")
    private String endIpv6;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "The gateway of IPv6 Network")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "The CIDR of IPv6 Network")
    private String ip6Cidr;

    @SerializedName(ApiConstants.FOR_SYSTEM_VMS)
    @Param(description = "Indicates whether VLAN IP range is dedicated to System VMs or not")
    private Boolean forSystemVms;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "indicates to which provider the IP range is dedicated to", since = "4.21.0")
    private String provider;

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getForSystemVms() {
        return forSystemVms;
    }

    public void setForSystemVms(Boolean forSystemVms) {
        this.forSystemVms = forSystemVms;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
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

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
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

    public String getphysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getStartIpv6() {
        return startIpv6;
    }

    public void setStartIpv6(String startIpv6) {
        this.startIpv6 = startIpv6;
    }

    public void setEndIpv6(String endIpv6) {
        this.endIpv6 = endIpv6;
    }

    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
