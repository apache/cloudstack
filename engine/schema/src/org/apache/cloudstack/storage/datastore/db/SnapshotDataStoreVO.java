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
package org.apache.cloudstack.storage.datastore.db;

import java.util.Date;

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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.storage.DataStoreRole;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.fsm.StateObject;

/**
 * Join table for image_data_store and snapshots
 * 
 */
@Entity
@Table(name = "snapshot_store_ref")
public class SnapshotDataStoreVO implements StateObject<ObjectInDataStoreStateMachine.State>, DataObjectInStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "store_id")
    private long dataStoreId;

    @Column(name = "store_role")
    @Enumerated(EnumType.STRING)
    private DataStoreRole role;

    @Column(name = "snapshot_id")
    private long snapshotId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdated = null;

    @Column(name = "size")
    private long size;

    @Column(name = "physical_size")
    private long physicalSize;

    @Column(name = "parent_snapshot_id")
    private long parentSnapshotId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "install_path")
    private String installPath;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name = "ref_cnt")
    Long refCnt = 0L;

    @Column(name = "volume_id")
    Long volumeId;

    public String getInstallPath() {
        return installPath;
    }

    @Override
    public long getDataStoreId() {
        return dataStoreId;
    }

    public void setDataStoreId(long storeId) {
        this.dataStoreId = storeId;
    }

    public long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date date) {
        lastUpdated = date;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public SnapshotDataStoreVO(long hostId, long snapshotId) {
        super();
        this.dataStoreId = hostId;
        this.snapshotId = snapshotId;
        this.state = ObjectInDataStoreStateMachine.State.Allocated;
    }

    public SnapshotDataStoreVO() {

    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SnapshotDataStoreVO) {
            SnapshotDataStoreVO other = (SnapshotDataStoreVO) obj;
            return (this.snapshotId == other.getSnapshotId() && this.dataStoreId == other.getDataStoreId());
        }
        return false;
    }

    public int hashCode() {
        Long tid = new Long(snapshotId);
        Long hid = new Long(dataStoreId);
        return tid.hashCode() + hid.hashCode();
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setPhysicalSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public long getVolumeSize() {
        return -1;
    }

    public String toString() {
        return new StringBuilder("SnapshotDataStore[").append(id).append("-").append(snapshotId).append("-")
                .append(dataStoreId).append(installPath).append("]").toString();
    }

    public long getUpdatedCount() {
        return this.updatedCount;
    }

    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    public void decrUpdatedCount() {
        this.updatedCount--;
    }

    public Date getUpdated() {
        return updated;
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        // TODO Auto-generated method stub
        return this.state;
    }

    public void setState(ObjectInDataStoreStateMachine.State state) {
        this.state = state;
    }

    @Override
    public long getObjectId() {
        return this.getSnapshotId();
    }

    public DataStoreRole getRole() {
        return role;
    }

    public void setRole(DataStoreRole role) {
        this.role = role;
    }

    @Override
    public State getObjectInStoreState() {
        return this.state;
    }

    public long getParentSnapshotId() {
        return parentSnapshotId;
    }

    public void setParentSnapshotId(long parentSnapshotId) {
        this.parentSnapshotId = parentSnapshotId;
    }

    public Long getRefCnt() {
        return refCnt;
    }

    public void incrRefCnt() {
        this.refCnt++;
    }

    public void decrRefCnt() {
        this.refCnt--;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }
}
