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
package org.apache.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.MetricConstants;
import org.apache.cloudstack.management.ManagementServerHost.State;

import java.util.Date;

public class UsageServerMetricsResponse  extends BaseResponse {
    @SerializedName(MetricConstants.COLLECTION_TIME)
    @Param(description = "the time these statistics were collected")
    private Date collectionTime;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the active usage server")
    private String hostname;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the usage server")
    private State state;

    @SerializedName(MetricConstants.LAST_HEARTBEAT)
    @Param(description = "the last time this Usage Server checked for jobs")
    private Date lastHeartbeat;

    @SerializedName(MetricConstants.LAST_SUCCESSFUL_JOB)
    @Param(description = "the last time a usage job successfully completed")
    private Date lastSuccessfulJob;

    public void setCollectionTime(Date collectionTime) {
        this.collectionTime = collectionTime;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setLastHeartbeat(Date lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public void setLastSuccessfulJob(Date lastSuccessfulJob) {
        this.lastSuccessfulJob = lastSuccessfulJob;
    }
}
