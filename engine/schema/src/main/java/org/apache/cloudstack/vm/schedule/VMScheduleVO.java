/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.vm.schedule;

import com.cloud.utils.db.GenericDao;

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
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

@Entity
@Table(name = "vm_schedule")
public class VMScheduleVO implements VMSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "uuid", nullable = false)
    String uuid;

    @Column(name = "description")
    String description;

    @Column(name = "vm_id", nullable = false)
    long vmId;

    @Column(name = "schedule", nullable = false)
    String schedule;

    @Column(name = "timezone", nullable = false)
    String timeZone;

    @Column(name = "action", nullable = false)
    @Enumerated(value = EnumType.STRING)
    Action action;

    @Column(name = "enabled", nullable = false)
    boolean enabled;

    @Column(name = "start_date", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    Date startDate;

    @Column(name = "end_date", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    Date endDate;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public VMScheduleVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VMScheduleVO(long vmId, String description, String schedule, String timeZone, Action action, Date startDate, Date endDate, boolean enabled) {
        uuid = UUID.randomUUID().toString();
        this.vmId = vmId;
        this.description = description;
        this.schedule = schedule;
        this.timeZone = timeZone;
        this.action = action;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enabled = enabled;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchedule() {
        return schedule.substring(2);
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @Override
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public ZoneId getTimeZoneId() {
        return TimeZone.getTimeZone(getTimeZone()).toZoneId();
    }

    public Date getCreated() {
        return created;
    }
}
