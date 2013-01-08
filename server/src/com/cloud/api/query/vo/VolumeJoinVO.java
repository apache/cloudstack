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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name="volume_view")
public class VolumeJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;


    @Column(name="name")
    private String name;

    @Column(name = "device_id")
    Long deviceId = null;

    @Column(name = "volume_type")
    @Enumerated(EnumType.STRING)
    Volume.Type volumeType;

    @Column(name = "size")
    long size;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private Volume.State state;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "attached")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date attached;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;


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

    @Column(name="pod_id")
    private long podId;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="data_center_uuid")
    private String dataCenterUuid;

    @Column(name="data_center_name")
    private String dataCenterName;

    @Column(name="vm_id")
    private long vmId;

    @Column(name="vm_uuid")
    private String vmUuid;

    @Column(name="vm_name")
    private String vmName;

    @Column(name="vm_display_name")
    private String vmDisplayName;

    @Column(name="vm_state")
    @Enumerated(value=EnumType.STRING)
    protected VirtualMachine.State vmState = null;

    @Column(name="vm_type")
    @Enumerated(value=EnumType.STRING)
    protected VirtualMachine.Type vmType;

    @Column (name="volume_host_size")
    private long volumeHostSize;

    @Column(name="volume_host_created")
    private Date volumeHostCreated;

    @Column(name="format")
    private Storage.ImageFormat format;

    @Column (name="download_pct")
    private int downloadPercent;

    @Column (name="download_state")
    @Enumerated(EnumType.STRING)
    private Status downloadState;

    @Column (name="error_str")
    private String errorString;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;


    @Column(name="disk_offering_id")
    private long diskOfferingId;

    @Column(name="disk_offering_uuid")
    private String diskOfferingUuid;

    @Column(name="disk_offering_name")
    private String diskOfferingName;

    @Column(name="disk_offering_display_text")
    private String diskOfferingDisplayText;

    @Column(name="system_use")
    private boolean systemUse;


    @Column(name="use_local_storage")
    private boolean useLocalStorage;

    @Column(name="pool_id")
    private long poolId;

    @Column(name="pool_uuid")
    private String poolUuid;

    @Column(name="pool_name")
    private String poolName;

    @Column(name="template_id")
    private long templateId;

    @Column(name="template_uuid")
    private String templateUuid;

    @Column(name="extractable")
    private boolean extractable;

    @Column(name="template_type")
    private Storage.TemplateType templateType;

    @Column(name="job_id")
    private long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;

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



    public VolumeJoinVO() {
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



    public Long getDeviceId() {
        return deviceId;
    }



    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }



    public Volume.Type getVolumeType() {
        return volumeType;
    }



    public void setVolumeType(Volume.Type volumeType) {
        this.volumeType = volumeType;
    }



    public long getSize() {
        return size;
    }



    public void setSize(long size) {
        this.size = size;
    }



    public Volume.State getState() {
        return state;
    }



    public void setState(Volume.State state) {
        this.state = state;
    }



    public Date getCreated() {
        return created;
    }



    public void setCreated(Date created) {
        this.created = created;
    }



    public Date getAttached() {
        return attached;
    }



    public void setAttached(Date attached) {
        this.attached = attached;
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



    public long getVmId() {
        return vmId;
    }



    public void setVmId(long vmId) {
        this.vmId = vmId;
    }



    public String getVmUuid() {
        return vmUuid;
    }



    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }



    public String getVmName() {
        return vmName;
    }



    public void setVmName(String vmName) {
        this.vmName = vmName;
    }



    public String getVmDisplayName() {
        return vmDisplayName;
    }



    public void setVmDisplayName(String vmDisplayName) {
        this.vmDisplayName = vmDisplayName;
    }



    public VirtualMachine.State getVmState() {
        return vmState;
    }



    public void setVmState(VirtualMachine.State vmState) {
        this.vmState = vmState;
    }



    public VirtualMachine.Type getVmType() {
        return vmType;
    }



    public void setVmType(VirtualMachine.Type vmType) {
        this.vmType = vmType;
    }



    public long getVolumeHostSize() {
        return volumeHostSize;
    }



    public void setVolumeHostSize(long volumeHostSize) {
        this.volumeHostSize = volumeHostSize;
    }



    public Date getVolumeHostCreated() {
        return volumeHostCreated;
    }



    public void setVolumeHostCreated(Date volumeHostCreated) {
        this.volumeHostCreated = volumeHostCreated;
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



    public Status getDownloadState() {
        return downloadState;
    }



    public void setDownloadState(Status downloadState) {
        this.downloadState = downloadState;
    }



    public String getErrorString() {
        return errorString;
    }



    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }



    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }



    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }



    public long getDiskOfferingId() {
        return diskOfferingId;
    }



    public void setDiskOfferingId(long diskOfferingId) {
        this.diskOfferingId = diskOfferingId;
    }



    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }



    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }



    public String getDiskOfferingName() {
        return diskOfferingName;
    }



    public void setDiskOfferingName(String diskOfferingName) {
        this.diskOfferingName = diskOfferingName;
    }



    public String getDiskOfferingDisplayText() {
        return diskOfferingDisplayText;
    }



    public void setDiskOfferingDisplayText(String diskOfferingDisplayText) {
        this.diskOfferingDisplayText = diskOfferingDisplayText;
    }



    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }



    public void setUseLocalStorage(boolean useLocalStorage) {
        this.useLocalStorage = useLocalStorage;
    }



    public long getPoolId() {
        return poolId;
    }



    public void setPoolId(long poolId) {
        this.poolId = poolId;
    }



    public String getPoolUuid() {
        return poolUuid;
    }



    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }



    public String getPoolName() {
        return poolName;
    }



    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }



    public long getTemplateId() {
        return templateId;
    }



    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }



    public String getTemplateUuid() {
        return templateUuid;
    }



    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
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



    public long getJobId() {
        return jobId;
    }



    public void setJobId(long jobId) {
        this.jobId = jobId;
    }



    public String getJobUuid() {
        return jobUuid;
    }



    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }



    public int getJobStatus() {
        return jobStatus;
    }



    public void setJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
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



    public long getPodId() {
        return podId;
    }



    public void setPodId(long podId) {
        this.podId = podId;
    }



    public boolean isSystemUse() {
        return systemUse;
    }



    public void setSystemUse(boolean systemUse) {
        this.systemUse = systemUse;
    }




}
