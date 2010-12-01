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

public class UserVmResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the virtual machine")
    private Long id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the virtual machine")
    private String name;

    @SerializedName("displayname") @Param(description="user generated name. The name of the virtual machine is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the ip address of the virtual machine")
    private String ipAddress;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the virtual machine")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain in which the virtual machine exists")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the domain in which the virtual machine exists")
    private String domainName;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date when this virtual machine was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the virtual machine")
    private String state;

    @SerializedName(ApiConstants.HA_ENABLE) @Param(description="true if high-availability is enabled, false otherwise")
    private Boolean haEnable;

    @SerializedName(ApiConstants.GROUP_ID) @Param(description="the group ID of the virtual machine")
    private Long groupId;

    @SerializedName(ApiConstants.GROUP) @Param(description="the group name of the virtual machine")
    private String group;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the availablility zone for the virtual machine")
    private Long zoneId;

    @SerializedName("zonename") @Param(description="the name of the availability zone for the virtual machine")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the ID of the host for the virtual machine")
    private Long hostId;

    @SerializedName("hostname") @Param(description="the name of the host for the virtual machine")
    private String hostName;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file.")
    private Long templateId;

    @SerializedName("templatename") @Param(description="the name of the template for the virtual machine")
    private String templateName;

    @SerializedName("templatedisplaytext") @Param(description="	an alternate display text of the template for the virtual machine")
    private String templateDisplayText;

    @SerializedName(ApiConstants.PASSWORD_ENABLED) @Param(description="true if the password rest feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName("isoid") @Param(description="the ID of the ISO attached to the virtual machine")
    private Long isoId;

    @SerializedName("isoname") @Param(description="the name of the ISO attached to the virtual machine")
    private String isoName;

    @SerializedName("isodisplaytext") @Param(description="an alternate display text of the ISO attached to the virtual machine")
    private String isoDisplayText;

    @SerializedName("serviceofferingid") @Param(description="the ID of the service offering of the virtual machine")
    private Long serviceOfferingId;

    @SerializedName("serviceofferingname") @Param(description="the name of the service offering of the virtual machine")
    private String serviceOfferingName;
    
    @SerializedName("forvirtualnetwork") @Param(description="the virtual network for the service offering")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.CPU_NUMBER) @Param(description="the number of cpu this virtual machine is running with")
    private Integer cpuNumber;

    @SerializedName(ApiConstants.CPU_SPEED) @Param(description="the speed of each cpu")
    private Integer cpuSpeed;

    @SerializedName(ApiConstants.MEMORY) @Param(description="the memory allocated for the virtual machine")
    private Integer memory;

    @SerializedName("cpuused") @Param(description="the amount of the vm's CPU currently used")
    private String cpuUsed;
    
    @SerializedName("networkkbsread") @Param(description="the incoming network traffic on the vm")
    private Long networkKbsRead;

    @SerializedName("networkkbswrite") @Param(description="the outgoing network traffic on the host")
    private Long networkKbsWrite;

    @SerializedName("guestosid") @Param(description="Os type ID of the virtual machine")
    private Long guestOsId;

    @SerializedName("rootdeviceid") @Param(description="device ID of the root volume")
    private Long rootDeviceId;

    @SerializedName("rootdevicetype") @Param(description="device type of the root volume")
    private String rootDeviceType;

    @SerializedName("networkgrouplist") @Param(description="list of network groups associated with the virtual machine")
    private String networkGroupList;

    @SerializedName(ApiConstants.PASSWORD) @Param(description="the password (if exists) of the virtual machine")
    private String password;

    @SerializedName(ApiConstants.JOB_ID) @Param(description="shows the current pending asynchronous job ID. This tag is not returned if no current pending jobs are acting on the virtual machine")
    private Long jobId;

    @SerializedName("jobstatus") @Param(description="shows the current pending asynchronous job status")
    private Integer jobStatus;
    
    public Long getObjectId() {
    	return getId();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public void setHaEnable(Boolean haEnable) {
        this.haEnable = haEnable;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public void setTemplateDisplayText(String templateDisplayText) {
        this.templateDisplayText = templateDisplayText;
    }

    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(Boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public Long getIsoId() {
        return isoId;
    }

    public void setIsoId(Long isoId) {
        this.isoId = isoId;
    }

    public String getIsoName() {
        return isoName;
    }

    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }

    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public void setIsoDisplayText(String isoDisplayText) {
        this.isoDisplayText = isoDisplayText;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public Integer getCpuNumber() {
        return cpuNumber;
    }

    public void setCpuNumber(Integer cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public String getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public Long getNetworkKbsRead() {
        return networkKbsRead;
    }

    public void setNetworkKbsRead(Long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
    }

    public Long getNetworkKbsWrite() {
        return networkKbsWrite;
    }

    public void setNetworkKbsWrite(Long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
    }

    public Long getGuestOsId() {
        return guestOsId;
    }

    public void setGuestOsId(Long guestOsId) {
        this.guestOsId = guestOsId;
    }

    public Long getRootDeviceId() {
        return rootDeviceId;
    }

    public void setRootDeviceId(Long rootDeviceId) {
        this.rootDeviceId = rootDeviceId;
    }

    public String getRootDeviceType() {
        return rootDeviceType;
    }

    public void setRootDeviceType(String rootDeviceType) {
        this.rootDeviceType = rootDeviceType;
    }

    public String getNetworkGroupList() {
        return networkGroupList;
    }

    public void setNetworkGroupList(String networkGroupList) {
        this.networkGroupList = networkGroupList;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }
}
