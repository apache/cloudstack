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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="template_view")
public class TemplateJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="unique_name")
    private String uniqueName;

    @Column(name="name")
    private String name;

    @Column(name="format")
    private Storage.ImageFormat format;

    @Column(name="public")
    private boolean publicTemplate = true;

    @Column(name="featured")
    private boolean featured;

    @Column(name="type")
    private Storage.TemplateType templateType;

    @Column(name="url")
    private String url = null;

    @Column(name="hvm")
    private boolean requiresHvm;

    @Column(name="bits")
    private int bits;

    @Temporal(value=TemporalType.TIMESTAMP)
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created = null;

    @Temporal(value=TemporalType.TIMESTAMP)
    @Column(name="created_on_store")
    private Date createdOnStore = null;

    @Column(name=GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name="checksum")
    private String checksum;

    @Column(name="display_text", length=4096)
    private String displayText;

    @Column(name="enable_password")
    private boolean enablePassword;

    @Column(name="dynamically_scalable")
    private boolean dynamicallyScalable;
    
    @Column(name="guest_os_id")
    private long guestOSId;

    @Column(name="guest_os_uuid")
    private String guestOSUuid;

    @Column(name="guest_os_name")
    private String guestOSName;

    @Column(name="bootable")
    private boolean bootable = true;

    @Column(name="prepopulate")
    private boolean prepopulate = false;

    @Column(name="cross_zones")
    private boolean crossZones = false;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name="extractable")
    private boolean extractable = true;

    @Column(name="source_template_id")
    private Long sourceTemplateId;

    @Column(name="source_template_uuid")
    private String sourceTemplateUuid;


    @Column(name="template_tag")
    private String templateTag;

    @Column(name="sort_key")
    private int sortKey;

    @Column(name="enable_sshkey")
    private boolean enableSshKey;

    @Column(name="account_id")
    private long accountId;

    @Column(name="account_uuid")
    private String accountUuid;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="account_type")
    private short accountType;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;

    @Column(name="project_id")
    private long projectId;

    @Column(name="project_uuid")
    private String projectUuid;

    @Column(name="project_name")
    private String projectName;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="data_center_uuid")
    private String dataCenterUuid;

    @Column(name="data_center_name")
    private String dataCenterName;

    @Column(name="store_scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType dataStoreScope;

    @Column(name="store_id")
    private Long dataStoreId; // this can be null for baremetal templates

    @Column (name="download_state")
    @Enumerated(EnumType.STRING)
    private Status downloadState;

    @Column (name="download_pct")
    private int downloadPercent;

    @Column (name="error_str")
    private String errorString;

    @Column (name="size")
    private long size;

    @Column(name="destroyed")
    boolean destroyed = false;

    @Column(name="lp_account_id")
    private Long sharedAccountId;

    @Column(name="detail_name")
    private String detailName;

    @Column(name="detail_value")
    private String detailValue;


    @Column(name="tag_id")
    private long tagId;

    @Column(name="tag_uuid")
    private String tagUuid;

    @Column(name="tag_key")
    private String tagKey;

    @Column(name="tag_value")
    private String tagValue;

    @Column(name="tag_domain_id")
    private long tagDomainId;

    @Column(name="tag_account_id")
    private long tagAccountId;

    @Column(name="tag_resource_id")
    private long tagResourceId;

    @Column(name="tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name="tag_resource_type")
    @Enumerated(value=EnumType.STRING)
    private TaggedResourceType tagResourceType;

    @Column(name="tag_customer")
    private String tagCustomer;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name="temp_zone_pair")
    private String tempZonePair; // represent a distinct (templateId, data_center_id) pair

    public TemplateJoinVO() {
    }



    @Override
    public long getId() {
        return id;
    }



    @Override
    public void setId(long id) {
        this.id = id;
    }



    @Override
    public String getUuid() {
        return uuid;
    }



    public void setUuid(String uuid) {
        this.uuid = uuid;
    }



    public String getName() {
        return name;
    }



    public void setName(String name) {
        this.name = name;
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



    @Override
    public long getAccountId() {
        return accountId;
    }



    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }



    @Override
    public String getAccountUuid() {
        return accountUuid;
    }



    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }



    @Override
    public String getAccountName() {
        return accountName;
    }



    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }



    @Override
    public short getAccountType() {
        return accountType;
    }



    public void setAccountType(short accountType) {
        this.accountType = accountType;
    }



    @Override
    public long getDomainId() {
        return domainId;
    }



    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }



    @Override
    public String getDomainUuid() {
        return domainUuid;
    }



    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }



    @Override
    public String getDomainName() {
        return domainName;
    }



    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }



    @Override
    public String getDomainPath() {
        return domainPath;
    }



    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }



    public long getProjectId() {
        return projectId;
    }



    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }



    @Override
    public String getProjectUuid() {
        return projectUuid;
    }



    public void setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
    }



    @Override
    public String getProjectName() {
        return projectName;
    }



    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }




    public boolean isExtractable() {
        return extractable;
    }



    public void setExtractable(boolean extractable) {
        this.extractable = extractable;
    }



    public Storage.TemplateType getTemplateType() {
        return templateType;
    }



    public void setTemplateType(Storage.TemplateType templateType) {
        this.templateType = templateType;
    }






    public long getTagId() {
        return tagId;
    }



    public void setTagId(long tagId) {
        this.tagId = tagId;
    }



    public String getTagUuid() {
        return tagUuid;
    }



    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }



    public String getTagKey() {
        return tagKey;
    }



    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }



    public String getTagValue() {
        return tagValue;
    }



    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }



    public long getTagDomainId() {
        return tagDomainId;
    }



    public void setTagDomainId(long tagDomainId) {
        this.tagDomainId = tagDomainId;
    }



    public long getTagAccountId() {
        return tagAccountId;
    }



    public void setTagAccountId(long tagAccountId) {
        this.tagAccountId = tagAccountId;
    }



    public long getTagResourceId() {
        return tagResourceId;
    }



    public void setTagResourceId(long tagResourceId) {
        this.tagResourceId = tagResourceId;
    }



    public String getTagResourceUuid() {
        return tagResourceUuid;
    }



    public void setTagResourceUuid(String tagResourceUuid) {
        this.tagResourceUuid = tagResourceUuid;
    }



    public TaggedResourceType getTagResourceType() {
        return tagResourceType;
    }



    public void setTagResourceType(TaggedResourceType tagResourceType) {
        this.tagResourceType = tagResourceType;
    }



    public String getTagCustomer() {
        return tagCustomer;
    }



    public void setTagCustomer(String tagCustomer) {
        this.tagCustomer = tagCustomer;
    }



    public long getDataCenterId() {
        return dataCenterId;
    }



    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }



    public String getDataCenterUuid() {
        return dataCenterUuid;
    }



    public void setDataCenterUuid(String dataCenterUuid) {
        this.dataCenterUuid = dataCenterUuid;
    }



    public String getDataCenterName() {
        return dataCenterName;
    }



    public void setDataCenterName(String dataCenterName) {
        this.dataCenterName = dataCenterName;
    }



    public String getUniqueName() {
        return uniqueName;
    }



    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }



    public boolean isPublicTemplate() {
        return publicTemplate;
    }



    public void setPublicTemplate(boolean publicTemplate) {
        this.publicTemplate = publicTemplate;
    }



    public boolean isFeatured() {
        return featured;
    }



    public void setFeatured(boolean featured) {
        this.featured = featured;
    }



    public String getUrl() {
        return url;
    }



    public void setUrl(String url) {
        this.url = url;
    }



    public boolean isRequiresHvm() {
        return requiresHvm;
    }



    public void setRequiresHvm(boolean requiresHvm) {
        this.requiresHvm = requiresHvm;
    }



    public int getBits() {
        return bits;
    }



    public void setBits(int bits) {
        this.bits = bits;
    }



    public String getChecksum() {
        return checksum;
    }



    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }



    public String getDisplayText() {
        return displayText;
    }



    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }



    public boolean isEnablePassword() {
        return enablePassword;
    }



    public void setEnablePassword(boolean enablePassword) {
        this.enablePassword = enablePassword;
    }

    
    
    public boolean isDynamicallyScalable() {
        return dynamicallyScalable;
    }

    public void setDynamicallyScalable(boolean dynamicallyScalable) {
        this.dynamicallyScalable = dynamicallyScalable;
    }
       
    

    public long getGuestOSId() {
        return guestOSId;
    }



    public void setGuestOSId(long guestOSId) {
        this.guestOSId = guestOSId;
    }



    public String getGuestOSUuid() {
        return guestOSUuid;
    }



    public void setGuestOSUuid(String guestOSUuid) {
        this.guestOSUuid = guestOSUuid;
    }



    public String getGuestOSName() {
        return guestOSName;
    }



    public void setGuestOSName(String guestOSName) {
        this.guestOSName = guestOSName;
    }



    public boolean isBootable() {
        return bootable;
    }



    public void setBootable(boolean bootable) {
        this.bootable = bootable;
    }



    public boolean isPrepopulate() {
        return prepopulate;
    }



    public void setPrepopulate(boolean prepopulate) {
        this.prepopulate = prepopulate;
    }



    public boolean isCrossZones() {
        return crossZones;
    }



    public void setCrossZones(boolean crossZones) {
        this.crossZones = crossZones;
    }



    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }



    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }



    public Long getSourceTemplateId() {
        return sourceTemplateId;
    }



    public void setSourceTemplateId(Long sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId;
    }



    public String getSourceTemplateUuid() {
        return sourceTemplateUuid;
    }



    public void setSourceTemplateUuid(String sourceTemplateUuid) {
        this.sourceTemplateUuid = sourceTemplateUuid;
    }



    public String getTemplateTag() {
        return templateTag;
    }



    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }



    public int getSortKey() {
        return sortKey;
    }



    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }



    public boolean isEnableSshKey() {
        return enableSshKey;
    }



    public void setEnableSshKey(boolean enableSshKey) {
        this.enableSshKey = enableSshKey;
    }



    public Status getDownloadState() {
        return downloadState;
    }



    public void setDownloadState(Status downloadState) {
        this.downloadState = downloadState;
    }



    public long getSize() {
        return size;
    }



    public void setSize(long size) {
        this.size = size;
    }



    public boolean isDestroyed() {
        return destroyed;
    }



    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }



    public Long getSharedAccountId() {
        return sharedAccountId;
    }



    public void setSharedAccountId(Long sharedAccountId) {
        this.sharedAccountId = sharedAccountId;
    }



    public String getDetailName() {
        return detailName;
    }



    public void setDetailName(String detailName) {
        this.detailName = detailName;
    }



    public String getDetailValue() {
        return detailValue;
    }



    public void setDetailValue(String detailValue) {
        this.detailValue = detailValue;
    }



    public Date getCreatedOnStore() {
        return createdOnStore;
    }



    public void setCreatedOnStore(Date createdOnStore) {
        this.createdOnStore = createdOnStore;
    }



    public Storage.ImageFormat getFormat() {
        return format;
    }



    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
    }



    public int getDownloadPercent() {
        return downloadPercent;
    }



    public void setDownloadPercent(int downloadPercent) {
        this.downloadPercent = downloadPercent;
    }



    public String getErrorString() {
        return errorString;
    }



    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }



    public Long getDataStoreId() {
        return dataStoreId;
    }



    public void setDataStoreId(Long dataStoreId) {
        this.dataStoreId = dataStoreId;
    }



    public ObjectInDataStoreStateMachine.State getState() {
        return state;
    }



    public void setState(ObjectInDataStoreStateMachine.State state) {
        this.state = state;
    }



    public ScopeType getDataStoreScope() {
        return dataStoreScope;
    }


    public void setDataStoreScope(ScopeType dataStoreScope) {
        this.dataStoreScope = dataStoreScope;
    }


    public String getTempZonePair() {
        return tempZonePair;
    }



    public void setTempZonePair(String tempZonePair) {
        this.tempZonePair = tempZonePair;
    }


}
