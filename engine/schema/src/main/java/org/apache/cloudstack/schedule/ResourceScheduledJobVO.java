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
package org.apache.cloudstack.schedule;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

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
@Table(name = "resource_scheduled_job")
public class ResourceScheduledJobVO implements ResourceScheduledJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "uuid", nullable = false)
    String uuid;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    ApiCommandResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    long resourceId;

    @Column(name = "schedule_id", nullable = false)
    long scheduleId;

    @Column(name = "async_job_id")
    Long asyncJobId;

    @Column(name = "action", nullable = false)
    String actionName;

    @Column(name = "scheduled_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date scheduledTime;

    public ResourceScheduledJobVO() {
        uuid = UUID.randomUUID().toString();
    }

    public ResourceScheduledJobVO(ApiCommandResourceType resourceType, long resourceId, long scheduleId, String action, Date scheduledTime) {
        uuid = UUID.randomUUID().toString();
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.scheduleId = scheduleId;
        this.actionName = action;
        this.scheduledTime = scheduledTime;
    }

    @Override
    public String toString() {
        return String.format("ResourceScheduledJob %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "resourceType", "actionName", "scheduleId", "resourceId", "asyncJobId"));
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
    public ApiCommandResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    @Override
    public long getScheduleId() {
        return scheduleId;
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
    public String getActionName() {
        return actionName;
    }

    public void setActionName(String action) {
        this.actionName = action;
    }

    @Override
    public Date getScheduledTime() {
        return scheduledTime;
    }
}
