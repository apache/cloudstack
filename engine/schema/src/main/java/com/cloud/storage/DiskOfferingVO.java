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
package com.cloud.storage;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.cloud.offering.DiskOffering;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "disk_offering")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 32)
public class DiskOfferingVO implements DiskOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "domain_id")
    Long domainId;

    @Column(name = "unique_name")
    private String uniqueName;

    @Column(name = "name")
    private String name = null;

    @Column(name = "display_text", length = 4096)
    private String displayText = null;

    @Column(name = "disk_size")
    long diskSize;

    @Column(name = "tags", length = 4096)
    String tags;

    @Column(name = "type")
    Type type;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "recreatable")
    private boolean recreatable;

    @Column(name = "use_local_storage")
    private boolean useLocalStorage;

    @Column(name = "system_use")
    private boolean systemUse;

    @Column(name = "customized")
    private boolean customized;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "customized_iops")
    private Boolean customizedIops;

    @Column(name = "min_iops")
    private Long minIops;

    @Column(name = "max_iops")
    private Long maxIops;

    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "bytes_read_rate")
    Long bytesReadRate;

    @Column(name = "bytes_write_rate")
    Long bytesWriteRate;

    @Column(name = "iops_read_rate")
    Long iopsReadRate;

    @Column(name = "iops_write_rate")
    Long iopsWriteRate;

    @Column(name = "cache_mode", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private DiskCacheMode cacheMode;

    @Column(name="provisioning_type")
    Storage.ProvisioningType provisioningType;

    @Column(name="display_offering")
    boolean displayOffering = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    State state;

    @Column(name = "hv_ss_reserve")
    Integer hypervisorSnapshotReserve;

    public DiskOfferingVO() {
        uuid = UUID.randomUUID().toString();
    }

    public DiskOfferingVO(Long domainId, String name, String displayText, Storage.ProvisioningType provisioningType, long diskSize, String tags, boolean isCustomized,
            Boolean isCustomizedIops, Long minIops, Long maxIops, DiskCacheMode cacheMode) {
        this.domainId = domainId;
        this.name = name;
        this.displayText = displayText;
        this.provisioningType = provisioningType;
        this.diskSize = diskSize;
        this.tags = tags;
        recreatable = false;
        type = Type.Disk;
        useLocalStorage = false;
        customized = isCustomized;
        uuid = UUID.randomUUID().toString();
        customizedIops = isCustomizedIops;
        this.minIops = minIops;
        this.maxIops = maxIops;
        this.cacheMode = cacheMode;
    }

    public DiskOfferingVO(Long domainId, String name, String displayText, Storage.ProvisioningType provisioningType, long diskSize, String tags, boolean isCustomized,
            Boolean isCustomizedIops, Long minIops, Long maxIops) {
        this.domainId = domainId;
        this.name = name;
        this.displayText = displayText;
        this.provisioningType = provisioningType;
        this.diskSize = diskSize;
        this.tags = tags;
        recreatable = false;
        type = Type.Disk;
        useLocalStorage = false;
        customized = isCustomized;
        uuid = UUID.randomUUID().toString();
        customizedIops = isCustomizedIops;
        this.minIops = minIops;
        this.maxIops = maxIops;
        state = State.Active;
    }

    public DiskOfferingVO(String name, String displayText, Storage.ProvisioningType provisioningType, boolean mirrored, String tags, boolean recreatable,
            boolean useLocalStorage, boolean systemUse, boolean customized) {
        domainId = null;
        type = Type.Service;
        this.name = name;
        this.displayText = displayText;
        this.provisioningType = provisioningType;
        this.tags = tags;
        this.recreatable = recreatable;
        this.useLocalStorage = useLocalStorage;
        this.systemUse = systemUse;
        this.customized = customized;
        uuid = UUID.randomUUID().toString();
        state = State.Active;
    }

    // domain specific offerings constructor (null domainId implies public
    // offering)
    public DiskOfferingVO(String name, String displayText, Storage.ProvisioningType provisioningType, boolean mirrored, String tags, boolean recreatable,
            boolean useLocalStorage, boolean systemUse, boolean customized, Long domainId) {
        type = Type.Service;
        this.name = name;
        this.displayText = displayText;
        this.provisioningType = provisioningType;
        this.tags = tags;
        this.recreatable = recreatable;
        this.useLocalStorage = useLocalStorage;
        this.systemUse = systemUse;
        this.customized = customized;
        this.domainId = domainId;
        uuid = UUID.randomUUID().toString();
        state = State.Active;
    }

    public DiskOfferingVO(long id, String name, String displayText, Storage.ProvisioningType provisioningType, boolean mirrored, String tags, boolean recreatable,
            boolean useLocalStorage, boolean systemUse, boolean customized, boolean customizedIops, Long domainId, Long minIops, Long maxIops) {
        this.id = id;
        type = Type.Service;
        this.name = name;
        this.displayText = displayText;
        this.provisioningType = provisioningType;
        this.tags = tags;
        this.recreatable = recreatable;
        this.useLocalStorage = useLocalStorage;
        this.systemUse = systemUse;
        this.customized = customized;
        this.customizedIops = customizedIops;
        this.domainId = domainId;
        uuid = UUID.randomUUID().toString();
        state = State.Active;
        this.minIops = minIops;
        this.maxIops = maxIops;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean isCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    @Override
    public Boolean isCustomizedIops() {
        return customizedIops;
    }

    @Override
    public void setCustomizedIops(Boolean customizedIops) {
        this.customizedIops = customizedIops;
    }

    @Override
    public Long getMinIops() {
        return minIops;
    }

    @Override
    public void setMinIops(Long minIops) {
        this.minIops = minIops;
    }

    @Override
    public Long getMaxIops() {
        return maxIops;
    }

    @Override
    public void setMaxIops(Long maxIops) {
        this.maxIops = maxIops;
    }

    @Override
    public DiskCacheMode getCacheMode() {
        return cacheMode;
    }

    @Override
    public void setCacheMode(DiskCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    @Override
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public boolean getUseLocalStorage() {
        return useLocalStorage;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean isRecreatable() {
        return recreatable;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean getSystemUse() {
        return systemUse;
    }

    public void setSystemUse(boolean systemUse) {
        this.systemUse = systemUse;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public Storage.ProvisioningType getProvisioningType(){
        return provisioningType;
    }

    @Override
    public long getDiskSize() {
        return diskSize;
    }

    @Override
    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    protected void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String getTags() {
        return tags;
    }

    public void setUniqueName(String name) {
        uniqueName = name;
    }

    @Override
    @Transient
    public String[] getTagsArray() {
        String tags = getTags();
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }

        return tags.split(",");
    }

    @Transient
    public boolean containsTag(String... tags) {
        if (this.tags == null) {
            return false;
        }

        for (String tag : tags) {
            if (!this.tags.matches(tag)) {
                return false;
            }
        }

        return true;
    }

    @Transient
    public void setTagsArray(List<String> newTags) {
        if (newTags.isEmpty()) {
            setTags(null);
            return;
        }

        StringBuilder buf = new StringBuilder();
        for (String tag : newTags) {
            buf.append(tag).append(",");
        }

        buf.delete(buf.length() - 1, buf.length());

        setTags(buf.toString());
    }

    public void setUseLocalStorage(boolean useLocalStorage) {
        this.useLocalStorage = useLocalStorage;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setSortKey(int key) {
        sortKey = key;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setRecreatable(boolean recreatable) {
        this.recreatable = recreatable;
    }

    public boolean getDisplayOffering() {
        return displayOffering;
    }

    public void setDisplayOffering(boolean displayOffering) {
        this.displayOffering = displayOffering;
    }

    @Override
    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    @Override
    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    @Override
    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    @Override
    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    @Override
    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    @Override
    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    @Override
    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    @Override
    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    @Override
    public void setHypervisorSnapshotReserve(Integer hypervisorSnapshotReserve) {
        this.hypervisorSnapshotReserve = hypervisorSnapshotReserve;
    }

    @Override
    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }
}
