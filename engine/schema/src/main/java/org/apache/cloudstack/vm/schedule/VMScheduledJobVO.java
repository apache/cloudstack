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
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vm_scheduled_job")
public class VMScheduledJobVO implements VMScheduledJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "uuid", nullable = false)
    String uuid;

    @Column(name = "vm_id", nullable = false)
    long vmId;

    @Column(name = "vm_schedule_id", nullable = false)
    long vmScheduleId;

    @Column(name = "async_job_id")
    Long asyncJobId;

    @Column(name = "action", nullable = false)
    @Enumerated(value = EnumType.STRING)
    VMSchedule.Action action;

    @Column(name = "scheduled_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date scheduledTime;

    public VMScheduledJobVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VMScheduledJobVO(long vmId, long vmScheduleId, VMSchedule.Action action, Date scheduledTime) {
        uuid = UUID.randomUUID().toString();
        this.vmId = vmId;
        this.vmScheduleId = vmScheduleId;
        this.action = action;
        this.scheduledTime = scheduledTime;
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
    public long getVmId() {
        return vmId;
    }

    @Override
    public long getVmScheduleId() {
        return vmScheduleId;
    }

    @Override
    public Long getAsyncJobId() {
        return asyncJobId;
    }

    @Override
    public void setAsyncJobId(long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }

    @Override
    public VMSchedule.Action getAction() {
        return action;
    }

    @Override
    public Date getScheduledTime() {
        return scheduledTime;
    }
}
