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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VirtualMachineTemplate.class)
@SuppressWarnings("unused")
public class TemplateResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse, SetResourceIconResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the template ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the template name")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "the template display text")
    private String displayText;

    @SerializedName(ApiConstants.IS_PUBLIC)
    // propName="public"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description = "true if this template is a public template, false otherwise")
    private boolean isPublic;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this template was created")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the date this template was removed")
    private Date removed;

    @SerializedName(ApiConstants.IS_READY)
    // propName="ready"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description = "true if the template is ready to be deployed from, false otherwise.")
    private boolean isReady;

    @SerializedName(ApiConstants.PASSWORD_ENABLED)
    @Param(description = "true if the reset password feature is enabled, false otherwise")
    private Boolean passwordEnabled;

    @SerializedName(ApiConstants.FORMAT)
    @Param(description = "the format of the template.")
    private ImageFormat format;

    @SerializedName(ApiConstants.BOOTABLE)
    @Param(description = "true if the ISO is bootable, false otherwise")
    private Boolean bootable;

    @SerializedName(ApiConstants.IS_FEATURED)
    @Param(description = "true if this template is a featured template, false otherwise")
    private boolean featured;

    @SerializedName(ApiConstants.CROSS_ZONES)
    @Param(description = "true if the template is managed across all Zones, false otherwise")
    private boolean crossZones;

    @SerializedName(ApiConstants.OS_TYPE_ID)
    @Param(description = "the ID of the OS type for this template.")
    private String osTypeId;

    @SerializedName("ostypename")
    @Param(description = "the name of the OS type for this template.")
    private String osTypeName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account id to which the template belongs")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name to which the template belongs")
    private String account;

    //TODO: since a template can be associated to more than one zones, this model is not accurate. For backward-compatibility, keep these fields
    // here, but add a zones field to capture multiple zones.
    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the ID of the zone for this template")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the zone for this template")
    private String zoneName;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "the status of the template")
    private String status;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "the size of the template")
    private Long size;

    @SerializedName(ApiConstants.PHYSICAL_SIZE)
    @Param(description = "the physical size of the template")
    private Long physicalSize;

    @SerializedName(ApiConstants.TEMPLATETYPE)
    @Param(description = "the type of the template")
    private String templateType;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "the hypervisor on which the template runs")
    private String hypervisor;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain to which the template belongs")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain to which the template belongs")
    private String domainId;

    @SerializedName(ApiConstants.IS_EXTRACTABLE)
    @Param(description = "true if the template is extractable, false otherwise")
    private Boolean extractable;

    @SerializedName(ApiConstants.CHECKSUM)
    @Param(description = "checksum of the template")
    private String checksum;

    @SerializedName(ApiConstants.SOURCETEMPLATEID)
    @Param(description = "the template ID of the parent template if present")
    private String sourcetemplateId;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the ID of the secondary storage host for the template")
    private String hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the secondary storage host for the template")
    private String hostName;

    @SerializedName(ApiConstants.TEMPLATE_TAG)
    @Param(description = "the tag of this template")
    private String templateTag;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the template")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the template")
    private String projectName;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "additional key/value details tied with template")
    private Map<String, String> details;

    @SerializedName(ApiConstants.DOWNLOAD_DETAILS)
    @Param(description = "Lists the download progress of a template across all secondary storages")
    private List<Map<String, String>> downloadDetails;

    @SerializedName(ApiConstants.BITS)
    @Param(description = "the processor bit size", since = "4.10")
    private int bits;

    @SerializedName(ApiConstants.SSHKEY_ENABLED)
    @Param(description = "true if template is sshkey enabled, false otherwise")
    private Boolean sshKeyEnabled;

    @SerializedName(ApiConstants.IS_DYNAMICALLY_SCALABLE)
    @Param(description = "true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory")
    private Boolean isDynamicallyScalable;

    @SerializedName(ApiConstants.DIRECT_DOWNLOAD)
    @Param(description = "KVM Only: true if template is directly downloaded to Primary Storage bypassing Secondary Storage")
    private Boolean directDownload;

    @SerializedName(ApiConstants.DEPLOY_AS_IS)
    @Param(description = "VMware only: true if template is deployed without orchestrating disks and networks but \"as-is\" defined in the template.",
            since = "4.15")
    private Boolean deployAsIs;

    @SerializedName(ApiConstants.DEPLOY_AS_IS_DETAILS)
    @Param(description = "VMware only: additional key/value details tied with deploy-as-is template",
            since = "4.15")
    private Map<String, String> deployAsIsDetails;

    @SerializedName("parenttemplateid")
    @Param(description = "if Datadisk template, then id of the root disk template this template belongs to")
    @Deprecated(since = "4.15")
    private String parentTemplateId;

    @SerializedName("childtemplates")
    @Param(description = "if root disk template, then ids of the datas disk templates this template owns")
    @Deprecated(since = "4.15")
    private Set<ChildTemplateResponse> childTemplates;

    @SerializedName(ApiConstants.REQUIRES_HVM)
    @Param(description = "true if template requires HVM enabled, false otherwise")
    private Boolean requiresHvm;

    @SerializedName(ApiConstants.URL)
    @Param(description = "the URL which the template/iso is registered from")
    private String url;

    @SerializedName(ApiConstants.RESOURCE_ICON)
    @Param(description = "Base64 string representation of the resource icon", since = "4.16.0.0")
    ResourceIconResponse icon;

    @SerializedName(ApiConstants.USER_DATA_ID) @Param(description="the id of userdata linked to this template", since = "4.18.0")
    private String userDataId;

    @SerializedName(ApiConstants.USER_DATA_NAME) @Param(description="the name of userdata linked to this template", since = "4.18.0")
    private String userDataName;

    @SerializedName(ApiConstants.USER_DATA_POLICY) @Param(description="the userdata override policy with the userdata provided while deploying VM", since = "4.18.0")
    private String userDataPolicy;

    @SerializedName(ApiConstants.USER_DATA_PARAMS) @Param(description="list of parameters which contains the list of keys or string parameters that are needed to be passed for any variables declared in userdata", since = "4.18.0")
    private String userDataParams;

    public TemplateResponse() {
        tags = new LinkedHashSet<>();
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public void setAccountName(String account) {
        this.account = account;
    }

    public void setOsTypeId(String osTypeId) {
        this.osTypeId = osTypeId;
    }

    public void setOsTypeName(String osTypeName) {
        this.osTypeName = osTypeName;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setDownloadProgress(List<Map<String, String>> downloadDetails) {
        this.downloadDetails = downloadDetails;
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

    public void setPhysicalSize(Long physicalSize) {
        this.physicalSize = physicalSize;
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
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setExtractable(Boolean extractable) {
        this.extractable = extractable;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setSourceTemplateId(String sourcetemplateId) {
        this.sourcetemplateId = sourcetemplateId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Map<String, String> getDetails() {
        return this.details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public void addDetail(String key, String value) {
        if (this.details == null) {
            setDetails(new HashMap<>());
        }
        this.details.put(key,value);
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setSshKeyEnabled(boolean sshKeyEnabled) {
        this.sshKeyEnabled = sshKeyEnabled;
    }

    public void setDynamicallyScalable(boolean isDynamicallyScalable) {
        this.isDynamicallyScalable = isDynamicallyScalable;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public void setDirectDownload(Boolean directDownload) {
        this.directDownload = directDownload;
    }

    public Boolean getDirectDownload() {
        return directDownload;
    }

    public void setDeployAsIs(Boolean deployAsIs) {
        this.deployAsIs = deployAsIs;
    }

    public void setParentTemplateId(String parentTemplateId) {
        this.parentTemplateId = parentTemplateId;
    }

    public void setChildTemplates(Set<ChildTemplateResponse> childTemplateIds) {
        this.childTemplates = childTemplateIds;
    }

    public Boolean isRequiresHvm() {
        return requiresHvm;
    }

    public void setRequiresHvm(Boolean requiresHvm) {
        this.requiresHvm = requiresHvm;
    }

    public Map<String, String> getDeployAsIsDetails() {
        return this.deployAsIsDetails;
    }

    public void setDeployAsIsDetails(Map<String, String> details) {
        this.deployAsIsDetails = details;
    }

    public void addDeployAsIsDetail(String key, String value) {
        if (this.deployAsIsDetails == null) {
            setDeployAsIsDetails(new HashMap<>());
        }
        this.deployAsIsDetails.put(key,value);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setResourceIconResponse(ResourceIconResponse icon) {
        this.icon = icon;
    }

    public String getUserDataId() {
        return userDataId;
    }

    public void setUserDataId(String userDataId) {
        this.userDataId = userDataId;
    }

    public String getUserDataName() {
        return userDataName;
    }

    public void setUserDataName(String userDataName) {
        this.userDataName = userDataName;
    }

    public String getUserDataPolicy() {
        return userDataPolicy;
    }

    public void setUserDataPolicy(String userDataPolicy) {
        this.userDataPolicy = userDataPolicy;
    }

    public String getUserDataParams() {
        return userDataParams;
    }

    public void setUserDataParams(String userDataParams) {
        this.userDataParams = userDataParams;
    }
}
