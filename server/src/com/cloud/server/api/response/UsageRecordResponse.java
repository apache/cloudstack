/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.api.response.BaseResponse;
import com.cloud.api.response.ControlledEntityResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class UsageRecordResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the user account name")
    private String accountName;
    
    @SerializedName(ApiConstants.ACCOUNT_ID) @Param(description="the user account Id")
    private IdentityProxy accountId = new IdentityProxy("account");
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID number")
    private IdentityProxy domainId = new IdentityProxy("domain");
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain the public IP address is associated with")
    private String domainName;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID number")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of account, including account name, service offering, and template")
    private String description;

    @SerializedName("usage") @Param(description="usage in hours")
    private String usage;

    @SerializedName("usagetype") @Param(description="usage type")
    private Integer usageType;

    @SerializedName("rawusage") @Param(description="raw usage in hours")
    private String rawUsage;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="virtual machine ID number")
    private IdentityProxy virtualMachineId = new IdentityProxy("vm_instance");

    @SerializedName(ApiConstants.NAME) @Param(description="virtual machine name")
    private String vmName;

    @SerializedName("offeringid") @Param(description="service offering ID number")
    private IdentityProxy serviceOfferingId = new IdentityProxy("disk_offering");

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="template ID number")
    private IdentityProxy templateId = new IdentityProxy("vm_template");

    @SerializedName("usageid") @Param(description="id of the usage entity")
    private Long usageId;
    
    @SerializedName(ApiConstants.TYPE) @Param(description="type")
    private String type;

    @SerializedName(ApiConstants.SIZE)
    private Long size;

    @SerializedName(ApiConstants.START_DATE) @Param(description="start date of account")
    private String startDate;

    @SerializedName(ApiConstants.END_DATE) @Param(description="end date of account")
    private String endDate;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the IP address")
    private String ipAddress;

    @SerializedName("assigneddate") @Param(description="the assign date of the account")
    private String assignedDate;

    @SerializedName("releaseddate") @Param(description="the release date of the account")
    private String releasedDate;
    
    @SerializedName("issourcenat") @Param(description="source Nat flag for IPAddress")
    private Boolean isSourceNat;

    @SerializedName("iselastic") @Param(description="Elastic flag for IPAddress")
    private Boolean isElastic;
    
    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountId(Long accountId) {
        this.accountId.setValue(accountId);
    }
    
    @Override
    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

    public void setRawUsage(String rawUsage) {
        this.rawUsage = rawUsage;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId.setValue(virtualMachineId);
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId.setValue(serviceOfferingId);
    }

    public void setTemplateId(Long templateId) {
        this.templateId.setValue(templateId);
    }

    public void setUsageId(Long usageId) {
        this.usageId = usageId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setAssignedDate(String assignedDate) {
        this.assignedDate = assignedDate;
    }

    public void setReleasedDate(String releasedDate) {
        this.releasedDate = releasedDate;
    }

    public void setSourceNat(Boolean isSourceNat) {
        this.isSourceNat = isSourceNat;
    }

    public void setElastic(Boolean isElastic) {
        this.isElastic = isElastic;
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
