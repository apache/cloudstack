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

package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.BackupSchedule;

import com.cloud.serializer.Param;
import com.cloud.utils.DateUtil;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = BackupSchedule.class)
public class BackupScheduleResponse extends BaseResponse {

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "name of the VM")
    private String vmName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "ID of the VM")
    private String vmId;

    @SerializedName("schedule")
    @Param(description = "time the backup is scheduled to be taken.")
    private String schedule;

    @SerializedName("intervaltype")
    @Param(description = "the interval type of the backup schedule")
    private DateUtil.IntervalType intervalType;

    @SerializedName("timezone")
    @Param(description = "the time zone of the backup schedule")
    private String timezone;

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public DateUtil.IntervalType getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(DateUtil.IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
