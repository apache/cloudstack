/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.storage;

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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.utils.db.GenericDaoBase;

@Entity
@Table(name = "vm_snapshot_spool_ref")
public class VMSnapshotTemplateStoragePoolVO implements VMSnapshotTemplateStorageResourceAssoc, DataObjectInStore {

    protected VMSnapshotTemplateStoragePoolVO() {
    }

    public VMSnapshotTemplateStoragePoolVO(long id, long poolId, long vmSnapshotId, Date created, String jobId, String installPath, long templateSize, boolean markedForGC,
            State state, Status status) {
        super();
        this.id = id;
        this.poolId = poolId;
        this.vmSnapshotId = vmSnapshotId;
        this.created = created;
        this.jobId = jobId;
        this.installPath = installPath;
        this.templateSize = templateSize;
        this.markedForGC = markedForGC;
        this.state = state;
        this.status = status;
    }

    public VMSnapshotTemplateStoragePoolVO(long poolId, long vmSnapshotId) {
        super();
        this.poolId = poolId;
        this.vmSnapshotId = vmSnapshotId;
        state = ObjectInDataStoreStateMachine.State.Allocated;
        status = Status.NOT_CREATED;
        markedForGC = false;
        templateSize = 0;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    long id;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "pool_id")
    private long poolId;

    @Column(name = "vm_snapshot_id")
    long vmSnapshotId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    Date created = null;

    @Column(name = "job_id")
    String jobId;

    @Column(name = "install_path")
    String installPath;

    @Column(name = "template_size")
    long templateSize;

    @Column(name = "marked_for_gc")
    boolean markedForGC;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    Status status;

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VMSnapshotTemplateStoragePoolVO) {
            VMSnapshotTemplateStoragePoolVO other = (VMSnapshotTemplateStoragePoolVO)obj;
            return (vmSnapshotId == other.getVmSnapshotId() && poolId == other.getPoolId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long vmSnapId = new Long(vmSnapshotId);
        Long pId = new Long(poolId);
        return vmSnapId.hashCode() + pId.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("VmSnapTmplPool[").append(id).append("-").append(vmSnapshotId).append("-").append(poolId).append("-").append(installPath).append("]").toString();
    }

    @Override
    public long getDataStoreId() {
        return poolId;
    }

    @Override
    public State getObjectInStoreState() {
        return state;
    }

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public long getSnapshotTemplateId() {
        return getId();
    }

    @Override
    public long getTemplateSize() {
        return templateSize;
    }

    public void setTemplateSize(long size) {
        templateSize = size;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    @Override
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    public long getPoolId() {
        return poolId;
    }

    public long getVmSnapshotId() {
        return vmSnapshotId;
    }

    @Override
    public long getObjectId() {
        return id;
    }

    public boolean isMarkedForGC() {
        return markedForGC;
    }

    public void setMarkedForGC(boolean markForGc) {
        markedForGC = markForGc;
    }

}
