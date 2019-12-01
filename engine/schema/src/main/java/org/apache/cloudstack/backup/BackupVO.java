//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
import com.google.gson.Gson;

@Entity
@Table(name = "backups")
public class BackupVO implements Backup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "offering_id")
    private long offeringId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "volumes", length = 65535)
    private String volumes;

    @Column(name = "size")
    private Long size;

    @Column(name = "protected_size")
    private Long protectedSize;

    @Column(name = "status")
    private Status status;

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

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public BackupVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public BackupVO(final Long vmId, final long offeringId, final Status status, final long accountId, final Long zoneId) {
        this.uuid = UUID.randomUUID().toString();
        this.vmId = vmId;
        this.offeringId = offeringId;
        this.status = status;
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.created = new Date();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getVmId() {
        return vmId;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public Long getSize() {
        return size;
    }

    public Long getProtectedSize() {
        return protectedSize;
    }

    @Override
    public Boolean hasUserDefinedSchedule() {
        return scheduleType != null;
    }

    @Override
    public DateUtil.IntervalType getScheduleType() {
        return scheduleType == null ? null : DateUtil.getIntervalType(scheduleType);
    }

    @Override
    public String getSchedule() {
        return schedule;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public Date getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    @Override
    public Long getAsyncJobId() {
        return asyncJobId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setProtectedSize(Long protectedSize) {
        this.protectedSize = protectedSize;
    }

    public void setScheduleType(short scheduleType) {
        this.scheduleType = scheduleType;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setScheduledTimestamp(Date scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    public void setAsyncJobId(Long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }

    public void setCreated(Date start) {
        this.created = start;
    }

    @Override
    public List<VolumeInfo> getBackedUpVolumes() {
        return Arrays.asList(new Gson().fromJson(this.volumes, VolumeInfo[].class));
    }

    public void setBackedUpVolumes(List<VolumeInfo> volumes) {
        this.volumes = new Gson().toJson(volumes);
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public String getVolumes() {
        return volumes;
    }

    protected void setVolumes(String volumes) {
        this.volumes = volumes;
    }

    @Override
    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(long offeringId) {
        this.offeringId = offeringId;
    }
}
