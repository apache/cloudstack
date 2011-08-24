/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class UsageRecordResponse extends BaseResponse {
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the user account name")
    private String accountName;
    
    @SerializedName(ApiConstants.ACCOUNT_ID) @Param(description="the user account Id")
    private Long accountId;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID number")
    private Long domainId;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID number")
    private Long zoneId;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of account, including account name, service offering, and template")
    private String description;

    @SerializedName("usage") @Param(description="usage in hours")
    private String usage;

    @SerializedName("usagetype") @Param(description="usage type")
    private Integer usageType;

    @SerializedName("rawusage") @Param(description="raw usage in hours")
    private String rawUsage;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="virtual machine ID number")
    private Long virtualMachineId;

    @SerializedName(ApiConstants.NAME) @Param(description="virtual machine name")
    private String vmName;

    @SerializedName("offeringid") @Param(description="service offering ID number")
    private Long serviceOfferingId;

    @SerializedName(ApiConstants.TEMPLATE_ID) @Param(description="template ID number")
    private Long templateId;

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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }


    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

    public String getRawUsage() {
        return rawUsage;
    }

    public void setRawUsage(String rawUsage) {
        this.rawUsage = rawUsage;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getUsageId() {
        return usageId;
    }

    public void setUsageId(Long usageId) {
        this.usageId = usageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(String assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(String releasedDate) {
        this.releasedDate = releasedDate;
    }

    public void setSourceNat(Boolean isSourceNat) {
        this.isSourceNat = isSourceNat;
    }

    public Boolean isSourceNat() {
        return isSourceNat;
    }
}
