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
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class IPAddressResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="public IP address id")
    private Long id;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="public IP address")
    private String ipAddress;

    @SerializedName("allocated") @Param(description="date the public IP address was acquired")
    private Date allocated;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the zone the public IP address belongs to")
    private Long zoneId;

    @SerializedName("zonename") @Param(description="the name of the zone the public IP address belongs to")
    private String zoneName;

    @SerializedName("issourcenat") @Param(description="true if the IP address is a source nat address, false otherwise")
    private Boolean sourceNat;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account the public IP address is associated with")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private Long projectId;
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID the public IP address is associated with")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain the public IP address is associated with")
    private String domainName;

    @SerializedName(ApiConstants.FOR_VIRTUAL_NETWORK) @Param(description="the virtual network for the IP address")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.VLAN_ID) @Param(description="the ID of the VLAN associated with the IP address")
    private Long vlanId;

    @SerializedName("vlanname") @Param(description="the VLAN associated with the IP address")
    private String vlanName;

    @SerializedName("isstaticnat") @Param(description="true if this ip is for static nat, false otherwise")
    private Boolean staticNat;
    
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="virutal machine id the ip address is assigned to (not null only for static nat Ip)")
    private Long virtualMachineId;
    
    @SerializedName("virtualmachinename") @Param(description="virutal machine name the ip address is assigned to (not null only for static nat Ip)")
    private String virtualMachineName;
    
    @SerializedName("virtualmachinedisplayname") @Param(description="virutal machine display name the ip address is assigned to (not null only for static nat Ip)")
    private String virtualMachineDisplayName;
    
    @SerializedName("associatednetworkid") @Param(description="the ID of the Network associated with the IP address")
    private Long associatedNetworkId;
    
    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the ID of the Network where ip belongs to")
    private Long networkId;
    
    @SerializedName(ApiConstants.STATE) @Param(description="State of the ip address. Can be: Allocatin, Allocated and Releasing")
    private String state;
    
    @SerializedName(ApiConstants.JOB_ID) @Param(description="shows the current pending asynchronous job ID. This tag is not returned if no current pending jobs are acting on the volume")
    private Long jobId;

    @SerializedName(ApiConstants.JOB_STATUS) @Param(description="shows the current pending asynchronous job status")
    private Integer jobStatus;

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setAllocated(Date allocated) {
        this.allocated = allocated;
    }
    
    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setSourceNat(Boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }
    
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }

    public void setVlanName(String vlanName) {
        this.vlanName = vlanName;
    }

	public void setStaticNat(Boolean staticNat) {
		this.staticNat = staticNat;
	}

    public void setAssociatedNetworkId(Long networkId) {
        this.associatedNetworkId = networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public void setVirtualMachineDisplayName(String virtualMachineDisplayName) {
        this.virtualMachineDisplayName = virtualMachineDisplayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public Long getObjectId() {
        return getId();
    }
    
    @Override
    public Long getJobId() {
        return jobId;
    }

    @Override
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
    
    @Override
    public Integer getJobStatus() {
        return jobStatus;
    }

    @Override
    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
