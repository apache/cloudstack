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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class UsageRecordResponse extends BaseResponseWithTagInformation implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the user account name")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the user account Id")
    private String accountId;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the resource")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the resource")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain the resource is associated with")
    private String domainName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "description of the usage record")
    private String description;

    @SerializedName("usage")
    @Param(description = "usage in hours")
    private String usage;

    @SerializedName("usagetype")
    @Param(description = "usage type ID")
    private Integer usageType;

    @SerializedName("rawusage")
    @Param(description = "raw usage in hours")
    private String rawUsage;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "virtual machine ID")
    private String virtualMachineId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "resource or virtual machine name")
    private String resourceName;

    @SerializedName("offeringid")
    @Param(description = "offering ID")
    private String offeringId;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "template ID")
    private String templateId;

    @SerializedName(ApiConstants.OS_TYPE_ID)
    @Param(description = "virtual machine os type ID")
    private String osTypeId;

    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "virtual machine os display name")
    private String osDisplayName;

    @SerializedName(ApiConstants.OS_CATEGORY_ID)
    @Param(description = "virtual machine guest os category ID")
    private String osCategoryId;

    @SerializedName(ApiConstants.OS_CATEGORY_NAME)
    @Param(description = "virtual machine os category name")
    private String osCategoryName;

    @SerializedName("usageid")
    @Param(description = "id of the resource")
    private String usageId;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "resource type")
    private String type;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "resource size")
    private Long size;

    @SerializedName("virtualsize")
    @Param(description = "virtual size of resource")
    private Long virtualSize;

    @SerializedName(ApiConstants.CPU_NUMBER)
    @Param(description = "number of cpu of resource")
    private Long cpuNumber;

    @SerializedName(ApiConstants.CPU_SPEED)
    @Param(description = "speed of each cpu of resource")
    private Long cpuSpeed;

    @SerializedName(ApiConstants.MEMORY)
    @Param(description = "memory allocated for the resource")
    private Long memory;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "start date of the usage record")
    private String startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "end date of the usage record")
    private String endDate;

    @SerializedName("issourcenat")
    @Param(description = "True if the IPAddress is source NAT")
    private Boolean isSourceNat;

    @SerializedName(ApiConstants.IS_SYSTEM)
    @Param(description = "True if the IPAddress is system IP - allocated during vm deploy or lb rule create")
    private Boolean isSystem;

    @SerializedName("networkid")
    @Param(description = "id of the network")
    private String networkId;

    @SerializedName("isdefault")
    @Param(description = "True if the resource is default")
    private Boolean isDefault;

    @SerializedName("vpcid")
    @Param(description = "id of the vpc")
    private String vpcId;

    public UsageRecordResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
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

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setResourceName(String name) {
        this.resourceName = name;
    }

    public void setOfferingId(String offeringId) {
        this.offeringId = offeringId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setOsTypeId(String osTypeId) {
        this.osTypeId = osTypeId;
    }

    public void setOsDisplayName(String osDisplayName) {
        this.osDisplayName = osDisplayName;
    }

    public void setOsCategoryId(String osCategoryId) {
        this.osCategoryId = osCategoryId;
    }

    public void setOsCategoryName(String osCategoryName) {
        this.osCategoryName = osCategoryName;
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

    public void setSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
        this.networkId = networkId;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public void setCpuNumber(Long cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public void setCpuSpeed(Long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public String getDomainName(){
        return domainName;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }
}
