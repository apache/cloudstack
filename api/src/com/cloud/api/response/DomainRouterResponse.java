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

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.vm.State;
import com.google.gson.annotations.SerializedName;

public class DomainRouterResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the router")
    private Long id;
 
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID for the router")
    private Long zoneId;

    @SerializedName("zonename") @Param(description="the Zone name for the router")
    private String zoneName; 

    @SerializedName(ApiConstants.DNS1) @Param(description="the first DNS for the router")
    private String dns1;

    @SerializedName(ApiConstants.DNS2) @Param(description="the second DNS for the router")
    private String dns2;

    @SerializedName("networkdomain") @Param(description="the network domain for the router")
    private String networkDomain;

    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway for the router")
    private String gateway;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the router")
    private String name;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID for the router")
    private Long podId;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the host ID for the router")
    private Long hostId;

    @SerializedName("hostname") @Param(description="the hostname for the router")
    private String hostName;

    @SerializedName(ApiConstants.PRIVATE_IP) @Param(description="the private IP address for the router")
    private String privateIp;

    @SerializedName("privatemacaddress") @Param(description="the private MAC address for the router")
    private String privateMacAddress;

    @SerializedName("privatenetmask") @Param(description="the private netmask for the router")
    private String privateNetmask;

    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public IP address for the router")
    private String publicIp;

    @SerializedName("publicmacaddress") @Param(description="the public MAC address for the router")
    private String publicMacAddress;

    @SerializedName("publicnetmask") @Param(description="the public netmask for the router")
    private String publicNetmask;

    @SerializedName("guestipaddress") @Param(description="the guest IP address for the router")
    private String guestIpAddress;

    @SerializedName("guestmacaddress") @Param(description="the guest MAC address for the router")
    private String guestMacAddress;

    @SerializedName("guestnetmask") @Param(description="the guest netmask for the router")
    private String guestNetmask;

    @SerializedName("templateid") @Param(description="the template ID for the router")
    private Long templateId;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date and time the router was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the router")
    private State state;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the router")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID associated with the router")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain associated with the router")
    private String domainName;
    
    public Long getObjectId() {
    	return getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
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

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public String getPrivateNetmask() {
        return privateNetmask;
    }

    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public String getPublicNetmask() {
        return publicNetmask;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public String getGuestIpAddress() {
        return guestIpAddress;
    }

    public void setGuestIpAddress(String guestIpAddress) {
        this.guestIpAddress = guestIpAddress;
    }

    public String getGuestMacAddress() {
        return guestMacAddress;
    }

    public void setGuestMacAddress(String guestMacAddress) {
        this.guestMacAddress = guestMacAddress;
    }

    public String getGuestNetmask() {
        return guestNetmask;
    }

    public void setGuestNetmask(String guestNetmask) {
        this.guestNetmask = guestNetmask;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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
}
