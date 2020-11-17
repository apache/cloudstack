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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.offering.DiskOffering;
import com.cloud.offering.DiskOffering.Type;
import com.cloud.storage.Storage;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "disk_offering_view")
public class DiskOfferingJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "display_text")
    private String displayText;

    @Column(name = "provisioning_type")
    Storage.ProvisioningType provisioningType;

    @Column(name = "disk_size")
    long diskSize;

    @Column(name = "tags", length = 4096)
    String tags;

    @Column(name = "use_local_storage")
    private boolean useLocalStorage;

    @Column(name = "system_use")
    private boolean systemUse;

    @Column(name = "customized")
    private boolean customized;

    @Column(name = "customized_iops")
    private Boolean customizedIops;

    @Column(name = "min_iops")
    private Long minIops;

    @Column(name = "max_iops")
    private Long maxIops;

    @Column(name = "hv_ss_reserve")
    private Integer hypervisorSnapshotReserve;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "bytes_read_rate")
    Long bytesReadRate;

    @Column(name = "bytes_read_rate_max")
    Long bytesReadRateMax;

    @Column(name = "bytes_read_rate_max_length")
    Long bytesReadRateMaxLength;

    @Column(name = "bytes_write_rate")
    Long bytesWriteRate;

    @Column(name = "bytes_write_rate_max")
    Long bytesWriteRateMax;

    @Column(name = "bytes_write_rate_max_length")
    Long bytesWriteRateMaxLength;

    @Column(name = "iops_read_rate")
    Long iopsReadRate;

    @Column(name = "iops_read_rate_max")
    Long iopsReadRateMax;

    @Column(name = "iops_read_rate_max_length")
    Long iopsReadRateMaxLength;

    @Column(name = "iops_write_rate")
    Long iopsWriteRate;

    @Column(name = "iops_write_rate_max")
    Long iopsWriteRateMax;

    @Column(name = "iops_write_rate_max_length")
    Long iopsWriteRateMaxLength;

    @Column(name = "cache_mode")
    String cacheMode;

    @Column(name = "type")
    Type type;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "domain_id")
    private String domainId = null;

    @Column(name = "domain_uuid")
    private String domainUuid = null;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "zone_id")
    private String zoneId = null;

    @Column(name = "zone_uuid")
    private String zoneUuid = null;

    @Column(name = "zone_name")
    private String zoneName = null;

    @Column(name = "display_offering")
    boolean displayOffering;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    DiskOffering.State state;

    @Column(name = "vsphere_storage_policy")
    String vsphereStoragePolicy;

    public DiskOfferingJoinVO() {
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

    public String getDisplayText() {
        return displayText;
    }

    public Storage.ProvisioningType getProvisioningType(){
        return provisioningType;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public String getTags() {
        return tags;
    }

    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }

    public boolean isSystemUse() {
        return systemUse;
    }

    public boolean isCustomized() {
        return customized;
    }

    public Boolean isCustomizedIops() {
        return customizedIops;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public boolean isDisplayOffering() {
        return displayOffering;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public int getSortKey() {
        return sortKey;
    }

    public Type getType() {
        return type;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public Long getBytesReadRateMax() { return bytesReadRateMax; }

    public Long getBytesReadRateMaxLength() { return bytesReadRateMaxLength; }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public Long getBytesWriteRateMax() { return bytesWriteRateMax; }

    public Long getBytesWriteRateMaxLength() { return bytesWriteRateMaxLength; }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public Long getIopsReadRateMax() { return iopsReadRateMax; }

    public Long getIopsReadRateMaxLength() { return iopsReadRateMaxLength; }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public Long getIopsWriteRateMax() { return iopsWriteRateMax; }

    public Long getIopsWriteRateMaxLength() { return iopsWriteRateMaxLength; }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public DiskOffering.State getState() {
        return state;
    }

    public void setState(DiskOffering.State state) {
        this.state = state;
    }

    public String getVsphereStoragePolicy() {
        return vsphereStoragePolicy;
    }
}
