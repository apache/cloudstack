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
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the resource")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the resource")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID")
    private IdentityProxy domainId = new IdentityProxy("domain");
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain the resource is associated with")
    private String domainName;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of the usage record")
    private String description;

    @SerializedName("usage") @Param(description="usage in hours")
    private String usage;

    @SerializedName("usagetype") @Param(description="usage type ID")
    private Integer usageType;

    @SerializedName("rawusage") @Param(description="raw usage in hours")
    private String rawUsage;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="virtual machine ID")
    private IdentityProxy virtualMachineId = new IdentityProxy("vm_instance");

    @SerializedName(ApiConstants.NAME) @Param(description="virtual machine name")
    private String vmName;

    @SerializedName("offeringid") @Param(description="offering ID")
    private String  offeringId;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="template ID")
    private IdentityProxy templateId = new IdentityProxy("vm_template");

    @SerializedName("usageid") @Param(description="id of the resource")
    private String usageId;
    
    @SerializedName(ApiConstants.TYPE) @Param(description="resource type")
    private String type;

    @SerializedName(ApiConstants.SIZE) @Param(description="resource size")
    private Long size;

    @SerializedName(ApiConstants.START_DATE) @Param(description="start date of the usage record")
    private String startDate;

    @SerializedName(ApiConstants.END_DATE) @Param(description="end date of the usage record")
    private String endDate;

    @SerializedName("issourcenat") @Param(description="True if the IPAddress is source NAT")
    private Boolean isSourceNat;

    @SerializedName("iselastic") @Param(description="True if the IPAddress is elastic")
    private Boolean isElastic;
    
    @SerializedName("networkid") @Param(description="id of the network")
    private String networkId;
    
    @SerializedName("isdefault") @Param(description="True if the resource is default")
    private Boolean isDefault;
    
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

    public void setOfferingId(String offeringId) {
        this.offeringId =  offeringId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId.setValue(templateId);
    }

    public void setUsageId(String usageId) {
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

    public void setNetworkId(String networkId) {
        this.networkId =  networkId;
    }
    
    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
