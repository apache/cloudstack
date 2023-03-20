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
package com.cloud.vm.schedule;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;
import javax.persistence.Temporal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vm_schedule")
public class VMScheduleVO implements VMSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "description")
    private String description;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state = State.Disabled;

    @Column(name = "action")
    @Enumerated(value = EnumType.STRING)
    private String action;

    @Column(name = "period")
    private String period;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "tag")
    private String tag;

    @Column(name = "schedule_type")
    private String scheduleType;

    @Column(name = "schedule")
    private String schedule;

    @Column(name = "scheduled_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date scheduledTimestamp;

    @Column(name = "async_job_id")
    Long asyncJobId;

    public VMScheduleVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public VMScheduleVO(long vmId, String description, String action, String scheduleType,
                        String schedule, String timezone, Date scheduledTimestamp, String tag, Long asyncJobId) {
        this.uuid = UUID.randomUUID().toString();
        this.vmId = vmId;
        this.description = description;
        this.action = action;
        this.scheduleType = scheduleType;
        this.schedule = schedule;
        this.timezone = timezone;
        this.scheduledTimestamp = scheduledTimestamp;
        this.tag = tag;
        this.asyncJobId = asyncJobId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public VMSchedule.State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Date getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setScheduledTimestamp(Date scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    @Override
    public String getScheduleType() {
        return scheduleType;
    }


    public Long getAsyncJobId() {
        return asyncJobId;
    }

    public void setAsyncJobId(Long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }
}
