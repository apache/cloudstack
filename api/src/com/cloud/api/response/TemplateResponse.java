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
import java.util.Map;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.storage.Storage.ImageFormat;
import com.google.gson.annotations.SerializedName;

public class TemplateResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the template ID")
    private long id;

    @SerializedName("name") @Param(description="the template name")
    private String name;

    @SerializedName("displaytext") @Param(description="the template display text")
    private String displayText;

    @SerializedName("ispublic") // propName="public"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description="true if this template is a public template, false otherwise")
    private boolean isPublic;

    @SerializedName("created") @Param(description="the date this template was created")
    private Date created;

    @SerializedName("removed") @Param(description="the date this template was removed")
    private Date removed;

    @SerializedName("isready") // propName="ready"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description="true if the template is ready to be deployed from, false otherwise.")
    private boolean isReady;

    @SerializedName("passwordenabled") @Param(description="true if the reset password feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName("format") @Param(description="the format of the template.")
    private ImageFormat format;

    @SerializedName("bootable") @Param(description="true if the ISO is bootable, false otherwise")
    private Boolean bootable;

    @SerializedName("isfeatured") @Param(description="true if this template is a featured template, false otherwise")
    private boolean featured;

    @SerializedName("crossZones") @Param(description="true if the template is managed across all Zones, false otherwise")
    private boolean crossZones;

    @SerializedName("ostypeid") @Param(description="the ID of the OS type for this template.")
    private Long osTypeId;

    @SerializedName("ostypename") @Param(description="the name of the OS type for this template.")
    private String osTypeName;

    @SerializedName("accountid") @Param(description="the account id to which the template belongs")
    private Long accountId;

    @SerializedName("account") @Param(description="the account name to which the template belongs")
    private String account;

    @SerializedName("zoneid") @Param(description="the ID of the zone for this template")
    private Long zoneId;

    @SerializedName("zonename") @Param(description="the name of the zone for this template")
    private String zoneName;

    @SerializedName("status") @Param(description="the status of the template")
    private String status;

    @SerializedName("size") @Param(description="the size of the template")
    private Long size;

    @SerializedName("templatetype") @Param(description="the type of the template")
    private String templateType;

    @SerializedName("hypervisor") @Param(description="the hypervisor on which the template runs")
    private String hypervisor;

    @SerializedName("jobid") @Param(description="shows the current pending asynchronous job ID. This tag is not returned if no current pending jobs are acting on the template")
    private Long jobId;

    @SerializedName("jobstatus") @Param(description="shows the current pending asynchronous job status")
    private Integer jobStatus;

    @SerializedName("domain") @Param(description="the name of the domain to which the template belongs")
    private String domainName;  

    @SerializedName("domainid") @Param(description="the ID of the domain to which the template belongs")
    private Long domainId;

    @SerializedName("isextractable") @Param(description="true if the template is extractable, false otherwise")
    private Boolean extractable;

    @SerializedName("checksum") @Param(description="checksum of the template")
    private String checksum;

    @SerializedName("sourcetemplateid") @Param(description="the template ID of the parent template if present")
    private Long sourcetemplateId;    

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the ID of the secondary storage host for the template")
    private Long hostId;

    @SerializedName("hostname") @Param(description="the name of the secondary storage host for the template")
    private String hostName;

    @SerializedName("templatetag") @Param(description="the tag of this template")
    private String templateTag;
    
    @SerializedName(ApiConstants.DETAILS) @Param(description="additional key/value details tied with template")
    private Map details;

    @Override
    public Long getObjectId() {
        return getId();
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public void setOsTypeId(Long osTypeId) {
        this.osTypeId = osTypeId;
    }

    public String getOsTypeName() {
        return osTypeName;
    }

    public void setOsTypeName(String osTypeName) {
        this.osTypeName = osTypeName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public Boolean isBootable() {
        return bootable;
    }

    public void setBootable(Boolean bootable) {
        this.bootable = bootable;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isCrossZones() {
        return crossZones;
    }

    public void setCrossZones(boolean crossZones) {
        this.crossZones = crossZones;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
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

    public long getDomainId() {
        return domainId;
    }

    public String getDomainName(){
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public Boolean isExtractable() {
        return extractable;
    }

    public void setExtractable(Boolean extractable) {
        this.extractable = extractable;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getSourceTemplateId() {
        return sourcetemplateId;
    }

    public void setSourceTemplateId(Long sourcetemplateId) {
        this.sourcetemplateId = sourcetemplateId;
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

    public String getTemplateTag() {
        return templateTag;
    }

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }    
    
    public Map getDetails() {
    	return this.details;
    }
    
    public void setDetails(Map details) {
    	this.details = details;
    }
}
