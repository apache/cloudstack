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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.schedule.VMSchedule;

import java.util.Date;

@EntityReference(value = VMSchedule.class)
public class VMScheduleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of VM schedule")
    private String id;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "ID of virtual machine")
    private String vmId;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of VM schedule")
    private String description;

    @SerializedName(ApiConstants.SCHEDULE)
    @Param(description = "Cron formatted VM schedule")
    private String schedule;

    @SerializedName(ApiConstants.TIMEZONE)
    @Param(description = "Timezone of the schedule")
    private String timeZone;

    @SerializedName(ApiConstants.ACTION)
    @Param(description = "Action")
    private VMSchedule.Action action;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "VM schedule is enabled")
    private boolean enabled;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Date from which the schedule is active")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Date after which the schedule becomes inactive")
    private Date endDate;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Date when the schedule was created")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setAction(VMSchedule.Action action) {
        this.action = action;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setCreated(Date created) {this.created = created;}
}
