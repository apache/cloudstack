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
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name = "volume_view")
public class VolumeJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "device_id")
    Long deviceId = null;

    @Column(name = "volume_type")
    @Enumerated(EnumType.STRING)
    Volume.Type volumeType;

    @Column(name = "provisioning_type")
    @Enumerated(EnumType.STRING)
    Storage.ProvisioningType provisioningType;

    @Column(name = "size")
    long size;

    @Column(name = "min_iops")
    Long minIops;

    @Column(name = "max_iops")
    Long maxIops;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private Volume.State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "attached")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date attached;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

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

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "data_center_uuid")
    private String dataCenterUuid;

    @Column(name = "data_center_name")
    private String dataCenterName;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "vm_uuid")
    private String vmUuid;

    @Column(name = "vm_name")
    private String vmName;

    @Column(name = "vm_display_name")
    private String vmDisplayName;

    @Column(name = "vm_state")
    @Enumerated(value = EnumType.STRING)
    protected VirtualMachine.State vmState = null;

    @Column(name = "vm_type")
    @Enumerated(value = EnumType.STRING)
    protected VirtualMachine.Type vmType;

    @Column(name = "volume_store_size")
    private long volumeStoreSize;

    @Column(name = "created_on_store")
    private Date createdOnStore;

    @Column(name = "format")
    private Storage.ImageFormat format;

    @Column(name = "download_pct")
    private int downloadPercent;

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    private Status downloadState;

    @Column(name = "error_str")
    private String errorString;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "disk_offering_id")
    private long diskOfferingId;

    @Column(name = "disk_offering_uuid")
    private String diskOfferingUuid;

    @Column(name = "disk_offering_name")
    private String diskOfferingName;

    @Column(name = "disk_offering_display_text")
    private String diskOfferingDisplayText;

    @Column(name = "system_use")
    private boolean systemUse;

    @Column(name = "use_local_storage")
    private boolean useLocalStorage;

    @Column(name = "bytes_read_rate")
    Long bytesReadRate;

    @Column(name = "bytes_write_rate")
    Long bytesWriteRate;

    @Column(name = "iops_read_rate")
    Long iopsReadRate;

    @Column(name = "iops_write_rate")
    Long iopsWriteRate;

    @Column(name = "cache_mode")
    String cacheMode;

    @Column(name = "pool_id")
    private long poolId;

    @Column(name = "pool_uuid")
    private String poolUuid;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "template_id")
    private long templateId;

    @Column(name = "template_uuid")
    private String templateUuid;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_display_text", length = 4096)
    private String templateDisplayText;

    @Column(name = "extractable")
    private boolean extractable;

    @Column(name = "template_type")
    private Storage.TemplateType templateType;

    @Column(name = "iso_id", updatable = true, nullable = true, length = 17)
    private long isoId;

    @Column(name = "iso_uuid")
    private String isoUuid;

    @Column(name = "iso_name")
    private String isoName;

    @Column(name = "iso_display_text", length = 4096)
    private String isoDisplayText;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "tag_id")
    private long tagId;

    @Column(name = "tag_uuid")
    private String tagUuid;

    @Column(name = "tag_key")
    private String tagKey;

    @Column(name = "tag_value")
    private String tagValue;

    @Column(name = "tag_domain_id")
    private long tagDomainId;

    @Column(name = "tag_account_id")
    private long tagAccountId;

    @Column(name = "tag_resource_id")
    private long tagResourceId;

    @Column(name = "tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name = "tag_resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceObjectType tagResourceType;

    @Column(name = "tag_customer")
    private String tagCustomer;

    @Column(name = "display_volume", updatable = true, nullable = false)
    protected boolean displayVolume;

    @Column(name = "path")
    protected String path;

    @Column(name = "chain_info", length = 65535)
    String chainInfo;

    public VolumeJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Volume.Type getVolumeType() {
        return volumeType;
    }

    public Storage.ProvisioningType getProvisioningType(){
        return provisioningType;
    }

    public long getSize() {
        return size;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Volume.State getState() {
        return state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getAttached() {
        return attached;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public boolean isDisplayVolume() {
        return displayVolume;
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

    public long getVmId() {
        return vmId;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVmDisplayName() {
        return vmDisplayName;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }

    public VirtualMachine.Type getVmType() {
        return vmType;
    }

    public long getVolumeStoreSize() {
        return volumeStoreSize;
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

    public Status getDownloadState() {
        return downloadState;
    }

    public String getErrorString() {
        return errorString;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getDiskOfferingDisplayText() {
        return diskOfferingDisplayText;
    }

    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public long getPoolId() {
        return poolId;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public long getTemplateId() {
        return templateId;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public boolean isExtractable() {
        return extractable;
    }

    public Storage.TemplateType getTemplateType() {
        return templateType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateDisplayText() {
        return templateDisplayText;
    }

    public long getIsoId() {
        return isoId;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public String getIsoName() {
        return isoName;
    }

    public String getIsoDisplayText() {
        return isoDisplayText;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public long getTagId() {
        return tagId;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public String getTagKey() {
        return tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public long getTagDomainId() {
        return tagDomainId;
    }

    public long getTagAccountId() {
        return tagAccountId;
    }

    public long getTagResourceId() {
        return tagResourceId;
    }

    public String getTagResourceUuid() {
        return tagResourceUuid;
    }

    public ResourceObjectType getTagResourceType() {
        return tagResourceType;
    }

    public String getTagCustomer() {
        return tagCustomer;
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

    public long getPodId() {
        return podId;
    }

    public boolean isSystemUse() {
        return systemUse;
    }

    public String getPath() {
        return path;
    }


    public String getChainInfo() {
        return chainInfo;
    }

    @Override
    public Class<?> getEntityType() {
        return Volume.class;
    }
}
