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
package com.cloud.vm;

import org.apache.cloudstack.vm.VmwareCbtMigrationDisk;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vmware_cbt_migration_disk")
public class VmwareCbtMigrationDiskVO implements VmwareCbtMigrationDisk {

    public VmwareCbtMigrationDiskVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VmwareCbtMigrationDiskVO(long migrationId, String sourceDiskId, String sourceDiskPath,
                                    String datastoreName, Long capacityBytes) {
        this();
        this.migrationId = migrationId;
        this.sourceDiskId = sourceDiskId;
        this.sourceDiskPath = sourceDiskPath;
        this.datastoreName = datastoreName;
        this.capacityBytes = capacityBytes;
        this.state = State.Created;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "migration_id")
    private long migrationId;

    @Column(name = "source_disk_id")
    private String sourceDiskId;

    @Column(name = "source_disk_path")
    private String sourceDiskPath;

    @Column(name = "datastore_name")
    private String datastoreName;

    @Column(name = "capacity_bytes")
    private Long capacityBytes;

    @Column(name = "target_path")
    private String targetPath;

    @Column(name = "target_format")
    private String targetFormat;

    @Column(name = "change_id")
    private String changeId;

    @Column(name = "snapshot_moref")
    private String snapshotMor;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getMigrationId() {
        return migrationId;
    }

    public String getSourceDiskId() {
        return sourceDiskId;
    }

    public String getSourceDiskPath() {
        return sourceDiskPath;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public String getSnapshotMor() {
        return snapshotMor;
    }

    public void setSnapshotMor(String snapshotMor) {
        this.snapshotMor = snapshotMor;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
