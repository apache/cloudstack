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

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "snapshots")
public class SnapshotVO implements Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "data_center_id")
    long dataCenterId;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "volume_id")
    Long volumeId;

    @Column(name = "disk_offering_id")
    Long diskOfferingId;

    @Expose
    @Column(name = "name")
    String name;

    @Expose
    @Column(name = "status", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "snapshot_type")
    short snapshotType;

    @Expose
    @Column(name = "location_type", updatable = true, nullable = true)
    @Enumerated(value = EnumType.STRING)
    private LocationType locationType;

    @Column(name = "type_description")
    String typeDescription;

    @Column(name = "size")
    long size;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    HypervisorType hypervisorType;

    @Expose
    @Column(name = "version")
    String version;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "min_iops")
    Long minIops;

    @Column(name = "max_iops")
    Long maxIops;

    public SnapshotVO() {
        uuid = UUID.randomUUID().toString();
    }

    public SnapshotVO(long dcId, long accountId, long domainId, Long volumeId, Long diskOfferingId, String name, short snapshotType, String typeDescription, long size,
            Long minIops, Long maxIops, HypervisorType hypervisorType, LocationType locationType) {
        dataCenterId = dcId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.volumeId = volumeId;
        this.diskOfferingId = diskOfferingId;
        this.name = name;
        this.snapshotType = snapshotType;
        this.typeDescription = typeDescription;
        this.size = size;
        this.minIops = minIops;
        this.maxIops = maxIops;
        state = State.Allocated;
        this.hypervisorType = hypervisorType;
        version = "2.2";
        uuid = UUID.randomUUID().toString();
        this.locationType = locationType;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getVolumeId() {
        return volumeId;
    }

    public long getDiskOfferingId() {
        return diskOfferingId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSnapshotId() {
        return id;
    }

    @Override
    public short getSnapshotType() {
        return snapshotType;
    }

    @Override
    public Type getRecurringType() {
        if (snapshotType < 0 || snapshotType >= Type.values().length) {
            return null;
        }
        return Type.values()[snapshotType];
    }

    @Override
    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setSnapshotType(short snapshotType) {
        this.snapshotType = snapshotType;
    }

    @Override
    public boolean isRecursive() {
        if (snapshotType >= Type.HOURLY.ordinal() && snapshotType <= Type.MONTHLY.ordinal()) {
            return true;
        }
        return false;
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

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static Type getSnapshotType(String snapshotType) {
        for (Type type : Type.values()) {
            if (type.equals(snapshotType)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Class<?> getEntityType() {
        return Snapshot.class;
    }

    @Override
    public String toString() {
        return String.format("Snapshot %s", new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append("uuid", getUuid()).append("name", getName())
                .append("volumeId", getVolumeId()).toString());
    }
}
