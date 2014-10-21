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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.storage.snapshot.SnapshotSchedule;

@Entity
@Table(name = "snapshot_schedule")
public class SnapshotScheduleVO implements SnapshotSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    // DB constraint: For a given volume and policyId, there will only be one
    // entry in this table.
    @Column(name = "volume_id")
    long volumeId;

    @Column(name = "policy_id")
    long policyId;

    @Column(name = "scheduled_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date scheduledTimestamp;

    @Column(name = "async_job_id")
    Long asyncJobId;

    @Column(name = "snapshot_id")
    Long snapshotId;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    public SnapshotScheduleVO() {
    }

    public SnapshotScheduleVO(long volumeId, long policyId, Date scheduledTimestamp) {
        this.volumeId = volumeId;
        this.policyId = policyId;
        this.scheduledTimestamp = scheduledTimestamp;
        this.snapshotId = null;
        this.asyncJobId = null;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getVolumeId() {
        return volumeId;
    }

    @Override
    public Long getPolicyId() {
        return policyId;
    }

    @Override
    public void setPolicyId(long policyId) {
        this.policyId = policyId;
    }

    /**
     * @return the scheduledTimestamp
     */
    @Override
    public Date getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    @Override
    public void setScheduledTimestamp(Date scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    @Override
    public Long getAsyncJobId() {
        return asyncJobId;
    }

    @Override
    public void setAsyncJobId(Long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }

    @Override
    public Long getSnapshotId() {
        return snapshotId;
    }

    @Override
    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
