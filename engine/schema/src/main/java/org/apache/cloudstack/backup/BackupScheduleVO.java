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

import com.cloud.utils.DateUtil;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

@Entity
@Table(name = "backup_schedule")
public class BackupScheduleVO implements BackupSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "schedule_type")
    private Short scheduleType;

    @Column(name = "schedule")
    String schedule;

    @Column(name = "timezone")
    String timezone;

    @Column(name = "scheduled_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date scheduledTimestamp;

    @Column(name = "async_job_id")
    Long asyncJobId;

    @Column(name = "max_backups")
    private int maxBackups = 0;

    @Column(name = "quiescevm")
    Boolean quiesceVM = false;

    @Column(name = "account_id")
    Long accountId;

    @Column(name = "domain_id")
    Long domainId;

    public BackupScheduleVO() {
    }

    public BackupScheduleVO(Long vmId, DateUtil.IntervalType scheduleType, String schedule, String timezone, Date scheduledTimestamp, int maxBackups, Boolean quiesceVM, Long accountId, Long domainId) {
        this.vmId = vmId;
        this.scheduleType = (short) scheduleType.ordinal();
        this.schedule = schedule;
        this.timezone = timezone;
        this.scheduledTimestamp = scheduledTimestamp;
        this.maxBackups = maxBackups;
        this.quiesceVM = quiesceVM;
        this.accountId = accountId;
        this.domainId = domainId;
    }

    @Override
    public String toString() {
        return String.format("BackupSchedule %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "vmId", "schedule", "scheduleType"));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public DateUtil.IntervalType getScheduleType() {
        return scheduleType == null ? null : DateUtil.getIntervalType(scheduleType);
    }

    public void setScheduleType(Short intervalType) {
        this.scheduleType = intervalType;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Date getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    public void setScheduledTimestamp(Date scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    public Long getAsyncJobId() {
        return asyncJobId;
    }

    public void setAsyncJobId(Long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }

    public int getMaxBackups() {
        return maxBackups;
    }

    public void setMaxBackups(int maxBackups) {
        this.maxBackups = maxBackups;
    }

    public void setQuiesceVM(Boolean quiesceVM) {
        this.quiesceVM = quiesceVM;
    }

    public Boolean getQuiesceVM() {
        return quiesceVM;
    }

    @Override
    public Class<?> getEntityType() {
        return BackupSchedule.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }
}
