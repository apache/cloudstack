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

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class VlanIpRangeResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the VLAN IP range")
    private IdentityProxy id = new IdentityProxy("vlan");

    @SerializedName("forvirtualnetwork") @Param(description="the virtual network for the VLAN IP range")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID of the VLAN IP range")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.VLAN) @Param(description="the ID or VID of the VLAN.")
    private String vlan;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account of the VLAN IP range")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID of the VLAN IP range")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the VLAN IP range")
    private String domainName;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID for the VLAN IP range")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    @SerializedName("podname") @Param(description="the Pod name for the VLAN IP range")
    private String podName;

    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway of the VLAN IP range")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the VLAN IP range")
    private String netmask;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="the description of the VLAN IP range")
    private String description;

    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the VLAN IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the VLAN IP range")
    private String endIp;
    
    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the network id of vlan range")
    private IdentityProxy networkId = new IdentityProxy("networks");
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vlan range")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vlan range")
    private String projectName;
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network this belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }
    
    public void setVlan(String vlan) {
        this.vlan = vlan;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setPodId(Long podId) {
        this.podId.setValue(podId);
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkId(Long networkId) {
        this.networkId.setValue(networkId);
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
    }

    public long getphysicalNetworkId() {
        return physicalNetworkId.getValue();
    }    
}
