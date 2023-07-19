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
import org.apache.cloudstack.jobs.JobInfo;

public class ClusterDrsPlanMigrationResponse extends BaseResponse {
    @SerializedName(ApiConstants.VM)
    @Param(description = "POST url to upload the file to")
    UserVmResponse vm;

    @SerializedName("sourcehost")
    @Param(description = "POST url to upload the file to")
    HostResponse srcHost;

    @SerializedName("destinationhost")
    @Param(description = "POST url to upload the file to")
    HostResponse destHost;

    @SerializedName(ApiConstants.JOB_ID)
    @Param(description = "Job id for migration of VM")
    private Long jobId;

    @SerializedName(ApiConstants.JOB_STATUS)
    @Param(description = "Job id for migration of VM")
    private JobInfo.Status jobStatus;


    public ClusterDrsPlanMigrationResponse(UserVmResponse vm, HostResponse srcHost, HostResponse destHost, Long jobId, JobInfo.Status jobStatus) {
        this.vm = vm;
        this.srcHost = srcHost;
        this.destHost = destHost;
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.setObjectName(ApiConstants.MIGRATIONS);
    }
}
