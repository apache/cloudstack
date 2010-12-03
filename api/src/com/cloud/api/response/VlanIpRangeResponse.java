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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VlanIpRangeResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the VLAN IP range")
    private Long id;

    @SerializedName("forvirtualnetwork") @Param(description="the virtual network for the VLAN IP range")
    private Boolean forVirtualNetwork;

    @SerializedName("zoneid") @Param(description="the Zone ID of the VLAN IP range")
    private Long zoneId;

    @SerializedName("vlan") @Param(description="the ID or VID of the VLAN.")
    private String vlan;

    @SerializedName("account") @Param(description="the account of the VLAN IP range")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain ID of the VLAN IP range")
    private Long domainId;

    @SerializedName("domain") @Param(description="the domain name of the VLAN IP range")
    private String domainName;

    @SerializedName("podid") @Param(description="the Pod ID for the VLAN IP range")
    private Long podId;

    @SerializedName("podname") @Param(description="the Pod name for the VLAN IP range")
    private String podName;

    @SerializedName("gateway") @Param(description="the gateway of the VLAN IP range")
    private String gateway;

    @SerializedName("netmask") @Param(description="the netmask of the VLAN IP range")
    private String netmask;

    @SerializedName("description") @Param(description="the description of the VLAN IP range")
    private String description;

    @SerializedName("startip") @Param(description="the start ip of the VLAN IP range")
    private String startIp;

    @SerializedName("endip") @Param(description="the end ip of the VLAN IP range")
    private String endIp;
    
    @SerializedName("networkid") @Param(description="the network id of vlan range")
    private Long networkId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }
}
