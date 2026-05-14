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

import org.apache.cloudstack.vm.VmwareCbtMigrationCycle;

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
@Table(name = "vmware_cbt_migration_cycle")
public class VmwareCbtMigrationCycleVO implements VmwareCbtMigrationCycle {

    public VmwareCbtMigrationCycleVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VmwareCbtMigrationCycleVO(long migrationId, int cycleNumber) {
        this();
        this.migrationId = migrationId;
        this.cycleNumber = cycleNumber;
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

    @Column(name = "cycle_number")
    private int cycleNumber;

    @Column(name = "snapshot_moref")
    private String snapshotMor;

    @Column(name = "changed_bytes")
    private Long changedBytes;

    @Column(name = "dirty_rate")
    private Long dirtyRate;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "description")
    private String description;

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

    public int getCycleNumber() {
        return cycleNumber;
    }

    public String getSnapshotMor() {
        return snapshotMor;
    }

    public Long getChangedBytes() {
        return changedBytes;
    }

    public Long getDirtyRate() {
        return dirtyRate;
    }

    public Long getDuration() {
        return duration;
    }

    public State getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }
}
