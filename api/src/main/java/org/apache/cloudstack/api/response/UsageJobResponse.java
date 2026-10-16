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

import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class UsageJobResponse extends BaseResponse {

    @SerializedName(ApiConstants.USAGE_SERVER)
    @Param(description = "the usage server host")
    private String usageServer;

    @SerializedName("jobtype")
    @Param(description = "the job type (0 - Recurring, 1 - Single)")
    private Integer jobType;

    @SerializedName("scheduled")
    @Param(description = "the job is scheduled or not")
    private Integer scheduled;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "  the start date of the job")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "  the end date of the job")
    private Date endDate;

    @SerializedName("executiontime")
    @Param(description = "  the execution time of the job")
    private Long executionTime;

    @SerializedName(ApiConstants.SUCCESS)
    @Param(description = "the job is success or not")
    private Boolean success;

    @SerializedName("heartbeat")
    @Param(description = "the job heartbeat")
    private Date heartbeat;

    public void setUsageServer(String usageServer) {
        this.usageServer = usageServer;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public void setScheduled(Integer scheduled) {
        this.scheduled = scheduled;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setHeartbeat(final Date heartbeat) {
        this.heartbeat = heartbeat;
    }
}
