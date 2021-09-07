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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "template_view")
public class TemplateJoinVO extends BaseViewWithTagInformationVO implements ControlledViewEntity {

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "unique_name")
    private String uniqueName;

    @Column(name = "name")
    private String name;

    @Column(name = "format")
    private Storage.ImageFormat format;

    @Column(name = "public")
    private boolean publicTemplate = true;

    @Column(name = "featured")
    private boolean featured;

    @Column(name = "type")
    private Storage.TemplateType templateType;

    @Column(name = "url")
    private String url = null;

    @Column(name = "hvm")
    private boolean requiresHvm;

    @Column(name = "bits")
    private int bits;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created = null;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "created_on_store")
    private Date createdOnStore = null;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "display_text", length = 4096)
    private String displayText;

    @Column(name = "enable_password")
    private boolean enablePassword;

    @Column(name = "dynamically_scalable")
    private boolean dynamicallyScalable;

    @Column(name = "guest_os_id")
    private long guestOSId;

    @Column(name = "guest_os_uuid")
    private String guestOSUuid;

    @Column(name = "guest_os_name")
    private String guestOSName;

    @Column(name = "bootable")
    private boolean bootable = true;

    @Column(name = "prepopulate")
    private boolean prepopulate = false;

    @Column(name = "cross_zones")
    private boolean crossZones = false;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "extractable")
    private boolean extractable = true;

    @Column(name = "source_template_id")
    private Long sourceTemplateId;

    @Column(name = "source_template_uuid")
    private String sourceTemplateUuid;

    @Column(name = "template_tag")
    private String templateTag;

    @Column(name = "sort_key")
    private int sortKey;

    @Column(name = "enable_sshkey")
    private boolean enableSshKey;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    private short accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "data_center_uuid")
    private String dataCenterUuid;

    @Column(name = "data_center_name")
    private String dataCenterName;

    @Column(name = "store_scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType dataStoreScope;

    @Column(name = "store_id")
    private Long dataStoreId; // this can be null for baremetal templates

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    private Status downloadState;

    @Column(name = "download_pct")
    private int downloadPercent;

    @Column(name = "error_str")
    private String errorString;

    @Column(name = "size")
    private long size;

    @Column(name = "physical_size")
    private long physicalSize;

    @Column(name = "template_state")
    @Enumerated(EnumType.STRING)
    private State templateState;

    @Column(name = "destroyed")
    boolean destroyed = false;

    @Column(name = "lp_account_id")
    private Long sharedAccountId;

    @Column(name = "parent_template_id")
    private Long parentTemplateId;

    @Column(name = "parent_template_uuid")
    private String parentTemplateUuid;

    @Column(name = "detail_name")
    private String detailName;

    @Column(name = "detail_value")
    private String detailValue;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name = "temp_zone_pair")
    private String tempZonePair; // represent a distinct (templateId, data_center_id) pair

    @Column(name = "direct_download")
    private boolean directDownload;

    @Column(name = "deploy_as_is")
    private boolean deployAsIs;

    public TemplateJoinVO() {
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public short getAccountType() {
        return accountType;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    public long getProjectId() {
        return projectId;
    }

    @Override
    public String getProjectUuid() {
        return projectUuid;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public boolean isExtractable() {
        return extractable;
    }

    public Storage.TemplateType getTemplateType() {
        return templateType;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public String getDataCenterUuid() {
        return dataCenterUuid;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public boolean isPublicTemplate() {
        return publicTemplate;
    }

    public boolean isFeatured() {
        return featured;
    }

    public String getUrl() {
        return url;
    }

    public boolean isRequiresHvm() {
        return requiresHvm;
    }

    public int getBits() {
        return bits;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getDisplayText() {
        return displayText;
    }

    public boolean isEnablePassword() {
        return enablePassword;
    }

    public boolean isDynamicallyScalable() {
        return dynamicallyScalable;
    }

    public long getGuestOSId() {
        return guestOSId;
    }

    public String getGuestOSUuid() {
        return guestOSUuid;
    }

    public String getGuestOSName() {
        return guestOSName;
    }

    public boolean isBootable() {
        return bootable;
    }

    public boolean isPrepopulate() {
        return prepopulate;
    }

    public boolean isCrossZones() {
        return crossZones;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public Long getSourceTemplateId() {
        return sourceTemplateId;
    }

    public String getSourceTemplateUuid() {
        return sourceTemplateUuid;
    }

    public String getTemplateTag() {
        return templateTag;
    }

    public int getSortKey() {
        return sortKey;
    }

    public boolean isEnableSshKey() {
        return enableSshKey;
    }

    public Status getDownloadState() {
        return downloadState;
    }

    public long getSize() {
        return size;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public Long getSharedAccountId() {
        return sharedAccountId;
    }

    public String getDetailName() {
        return detailName;
    }

    public String getDetailValue() {
        return detailValue;
    }

    public Date getCreatedOnStore() {
        return createdOnStore;
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public int getDownloadPercent() {
        return downloadPercent;
    }

    public String getErrorString() {
        return errorString;
    }

    public Long getDataStoreId() {
        return dataStoreId;
    }

    public ObjectInDataStoreStateMachine.State getState() {
        return state;
    }

    public ScopeType getDataStoreScope() {
        return dataStoreScope;
    }

    public String getTempZonePair() {
        return tempZonePair;
    }

    public State getTemplateState() {
        return templateState;
    }

    @Override
    public Class<?> getEntityType() {
        return VirtualMachineTemplate.class;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public boolean isDirectDownload() {
        return directDownload;
    }

    public boolean isDeployAsIs() {
        return deployAsIs;
    }

    public Object getParentTemplateId() {
        return parentTemplateId;
    }

    public String getParentTemplateUuid() {
        return parentTemplateUuid;
    }

}
