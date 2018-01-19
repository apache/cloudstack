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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.utils.DateUtil.IntervalType;

@Entity
@Table(name = "snapshot_policy")
public class SnapshotPolicyVO implements SnapshotPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "volume_id")
    long volumeId;

    @Column(name = "schedule")
    String schedule;

    @Column(name = "timezone")
    String timezone;

    @Column(name = "interval")
    private short interval;

    @Column(name = "max_snaps")
    private int maxSnaps;

    @Column(name = "active")
    boolean active = false;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public SnapshotPolicyVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public SnapshotPolicyVO(long volumeId, String schedule, String timezone, IntervalType intvType, int maxSnaps, boolean display) {
        this.volumeId = volumeId;
        this.schedule = schedule;
        this.timezone = timezone;
        this.interval = (short)intvType.ordinal();
        this.maxSnaps = maxSnaps;
        this.active = true;
        this.display = display;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getVolumeId() {
        return volumeId;
    }

    @Override
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @Override
    public String getSchedule() {
        return schedule;
    }

    @Override
    public void setInterval(short interval) {
        this.interval = interval;
    }

    @Override
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public short getInterval() {
        return interval;
    }

    @Override
    public void setMaxSnaps(int maxSnaps) {
        this.maxSnaps = maxSnaps;
    }

    @Override
    public int getMaxSnaps() {
        return maxSnaps;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }
}
