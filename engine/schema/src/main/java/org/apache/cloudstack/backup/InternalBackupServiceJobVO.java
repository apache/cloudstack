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
package org.apache.cloudstack.backup;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "internal_backup_service_job")
public class InternalBackupServiceJobVO implements InternalIdentity, Comparable<InternalBackupServiceJobVO> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "backup_id")
    private long backupId;

    @Column(name = "instance_id")
    private long instanceId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "attempts")
    private int attempts;

    @Column (name = "type")
    private InternalBackupServiceJobType type;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "scheduled_start_time")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date scheduledStartTime;

    @Column(name = "start_time")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public InternalBackupServiceJobVO() {
    }

    public InternalBackupServiceJobVO(long backupId, long zoneId, long instanceId, long accountId, InternalBackupServiceJobType type) {
        this.created = new Date();
        this.backupId = backupId;
        this.zoneId = zoneId;
        this.instanceId = instanceId;
        this.accountId = accountId;
        this.type = type;
        this.scheduledStartTime = this.created;
    }

    public InternalBackupServiceJobVO(long backupId, long zoneId, long instanceId, long accountId, InternalBackupServiceJobType type, Date scheduledStartTime) {
        this(backupId, zoneId, instanceId, accountId, type);
        this.scheduledStartTime = scheduledStartTime;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getBackupId() {
        return backupId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public long getAccountId() {
        return accountId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public InternalBackupServiceJobType getType() {
        return type;
    }

    public Date getCreated() {
        return created;
    }

    public Date getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(Date scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public int compareTo(InternalBackupServiceJobVO that) {
        return this.created.compareTo(that.created);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "backupId", "zoneId", "hostId", "created", "scheduledStartTime", "startTime", "attempts",
                "type");
    }
}
