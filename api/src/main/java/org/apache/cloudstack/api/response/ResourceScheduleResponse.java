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
package org.apache.cloudstack.api.response;

import java.util.Date;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.schedule.ResourceSchedule;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ResourceSchedule.class)
public class ResourceScheduleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of resource schedule")
    private String id;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "Type of the resource")
    private ApiCommandResourceType resourceType;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "ID of the resource")
    private String resourceId;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of resource schedule")
    private String description;

    @SerializedName(ApiConstants.SCHEDULE)
    @Param(description = "Cron formatted resource schedule")
    private String schedule;

    @SerializedName(ApiConstants.TIMEZONE)
    @Param(description = "Timezone of the schedule")
    private String timeZone;

    @SerializedName(ApiConstants.ACTION)
    @Param(description = "Action")
    private ResourceSchedule.Action action;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "Resource schedule is enabled")
    private boolean enabled;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Date from which the schedule is active")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Date after which the schedule becomes inactive")
    private Date endDate;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Schedule details")
    private Map<String, String> details;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Date when the schedule was created")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setResourceType(ApiCommandResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ApiCommandResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setAction(ResourceSchedule.Action action) {
        this.action = action;
    }

    public ResourceSchedule.Action getAction() {
        return action;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getCreated() {
        return created;
    }
}
