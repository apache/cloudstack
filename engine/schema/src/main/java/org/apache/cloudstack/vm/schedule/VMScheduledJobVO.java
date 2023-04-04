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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vm_scheduled_job")
public class VMScheduledJobVO implements VMScheduledJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;
    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();
    @Column(name = "vm_id")
    long vmId;
    @Column(name = "vm_schedule_id")
    long vmScheduleId;
    @Column(name = "async_job_id")
    long asyncJobId;
    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;
    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public VMScheduledJobVO() {}

    public VMScheduledJobVO(long vmId, long vmScheduleId, long asyncJobId) {
        this.vmId = vmId;
        this.vmScheduleId = vmScheduleId;
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

    public long getVmId() {
        return vmId;
    }
    public long getVmScheduleId() {
        return vmScheduleId;
    }

    public long getAsyncJobId() {
        return asyncJobId;
    }

    public void setAsyncJobId(long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }
}
