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
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.cloud.storage.Storage.ImageFormat;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class TemplateResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the template ID")
    private IdentityProxy id = new IdentityProxy("vm_template");

    @SerializedName(ApiConstants.NAME) @Param(description="the template name")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="the template display text")
    private String displayText;

    @SerializedName(ApiConstants.IS_PUBLIC) // propName="public"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description="true if this template is a public template, false otherwise")
    private boolean isPublic;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date this template was created")
    private Date created;

    @SerializedName("removed") @Param(description="the date this template was removed")
    private Date removed;

    @SerializedName(ApiConstants.IS_READY) // propName="ready"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description="true if the template is ready to be deployed from, false otherwise.")
    private boolean isReady;

    @SerializedName(ApiConstants.PASSWORD_ENABLED) @Param(description="true if the reset password feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName(ApiConstants.FORMAT) @Param(description="the format of the template.")
    private ImageFormat format;

    @SerializedName(ApiConstants.BOOTABLE) @Param(description="true if the ISO is bootable, false otherwise")
    private Boolean bootable;

    @SerializedName(ApiConstants.IS_FEATURED) @Param(description="true if this template is a featured template, false otherwise")
    private boolean featured;

    @SerializedName("crossZones") @Param(description="true if the template is managed across all Zones, false otherwise")
    private boolean crossZones;

    @SerializedName(ApiConstants.OS_TYPE_ID) @Param(description="the ID of the OS type for this template.")
    private IdentityProxy osTypeId = new IdentityProxy("guest_os");

    @SerializedName("ostypename") @Param(description="the name of the OS type for this template.")
    private String osTypeName;

    @SerializedName(ApiConstants.ACCOUNT_ID) @Param(description="the account id to which the template belongs")
    private IdentityProxy accountId = new IdentityProxy("account");

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account name to which the template belongs")
    private String account;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the zone for this template")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("zonename") @Param(description="the name of the zone for this template")
    private String zoneName;

    @SerializedName(ApiConstants.STATUS) @Param(description="the status of the template")
    private String status;

    @SerializedName(ApiConstants.SIZE) @Param(description="the size of the template")
    private Long size;

    @SerializedName("templatetype") @Param(description="the type of the template")
    private String templateType;

    @SerializedName(ApiConstants.HYPERVISOR) @Param(description="the hypervisor on which the template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the domain to which the template belongs")
    private String domainName;  

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain to which the template belongs")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.IS_EXTRACTABLE) @Param(description="true if the template is extractable, false otherwise")
    private Boolean extractable;

    @SerializedName(ApiConstants.CHECKSUM) @Param(description="checksum of the template")
    private String checksum;

    @SerializedName("sourcetemplateid") @Param(description="the template ID of the parent template if present")
    private IdentityProxy sourcetemplateId = new IdentityProxy("vm_template");    

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the ID of the secondary storage host for the template")
    private IdentityProxy hostId = new IdentityProxy("host");

    @SerializedName("hostname") @Param(description="the name of the secondary storage host for the template")
    private String hostName;

    @SerializedName(ApiConstants.TEMPLATE_TAG) @Param(description="the tag of this template")
    private String templateTag;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the template")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the template")
    private String projectName;

    @Override
    public Long getObjectId() {
        return getId();
    }
    
    public Long getId() {
        return id.getValue();
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setAccountId(Long accountId) {
        this.accountId.setValue(accountId);
    }

    public void setAccountName(String account) {
        this.account = account;
    }

    public void setOsTypeId(Long osTypeId) {
        this.osTypeId.setValue(osTypeId);
    }

    public void setOsTypeName(String osTypeName) {
        this.osTypeName = osTypeName;
    }

    public void setId(long id) {
        this.id.setValue(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public void setBootable(Boolean bootable) {
        this.bootable = bootable;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public void setCrossZones(boolean crossZones) {
        this.crossZones = crossZones;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }
    
    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setExtractable(Boolean extractable) {
        this.extractable = extractable;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setSourceTemplateId(Long sourcetemplateId) {
        this.sourcetemplateId.setValue(sourcetemplateId);
    }    

    public void setHostId(Long hostId) {
        this.hostId.setValue(hostId);
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
