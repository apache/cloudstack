// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for addition67al information
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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.cloud.vm.schedule.VMSchedule;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = VMSchedule.class)
public class VMScheduleResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the vm schedule")
    private String id;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the vm schedule")
    private String description;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the vm schedule")
    private VMSchedule.State state;

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    @SerializedName(ApiConstants.SCHEDULE)
    @Param(description = "the schedule of the vm schedule")
    private String schedule;

    @SerializedName(ApiConstants.INTERVAL_TYPE)
    @Param(description = "the scheduletype of the vm schedule")
    private String scheduleType;


    @SerializedName(ApiConstants.VM_SCHEDULE_ACTION)
    @Param(description = "the action of the vm schedule")
    private String action;

    @SerializedName(ApiConstants.VM_SCHEDULE_TIMEZONE)
    @Param(description = "the timezone of the vm schedule")
    private String timezone;

    @SerializedName(ApiConstants.VM_SCHEDULE_TAG)
    @Param(description = "the tag of the vm schedule")
    private String tag;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "Id of Virtual Machine")
    private Long virtualMachineId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VMSchedule.State getState() {
        return state;
    }

    public void setState(VMSchedule.State state) {
        this.state = state;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }
}
