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

package com.cloud.vm.snapshot;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vm_snapshots")
public class VMSnapshotVO implements VMSnapshot {
    @Id
    @TableGenerator(name = "vm_snapshots_sq",
                    table = "sequence",
                    pkColumnName = "name",
                    valueColumnName = "value",
                    pkColumnValue = "vm_snapshots_seq",
                    allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    Long id;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "name")
    String name;

    @Column(name = "display_name")
    String displayName;

    @Column(name = "description")
    String description;

    @Column(name = "vm_id")
    long vmId;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "vm_snapshot_type")
    @Enumerated(EnumType.STRING)
    VMSnapshot.Type type;

    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "current")
    Boolean current;

    @Column(name = "parent")
    Long parent;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount;

    @Transient
    VMSnapshotOptions options;

    public VMSnapshotOptions getOptions() {
        return options;
    }

    public void setOptions(VMSnapshotOptions options) {
        this.options = options;
    }

    @Override
    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public VMSnapshotVO() {

    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public VMSnapshotVO(Long accountId, Long domainId, Long vmId, String description, String vmSnapshotName, String vsDisplayName, Long serviceOfferingId, Type type,
            Boolean current) {
        this.accountId = accountId;
        this.domainId = domainId;
        this.vmId = vmId;
        this.state = State.Allocated;
        this.description = description;
        this.name = vmSnapshotName;
        this.displayName = vsDisplayName;
        this.type = type;
        this.current = current;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getUuid() {
        return uuid;
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
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    @Override
    public long getUpdatedCount() {
        return updatedCount;
    }

    @Override
    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    @Override
    public Type getType() {
        return type;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
