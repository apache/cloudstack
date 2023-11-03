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
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "VM to migrate")
    String vmId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "VM to migrate")
    String vmName;

    @SerializedName("sourcehostid")
    @Param(description = "Original host for VM migration")
    String srcHostId;

    @SerializedName("sourcehostname")
    @Param(description = "Original host for VM migration")
    String srcHostName;

    @SerializedName("destinationhostid")
    @Param(description = "Destination host for VM migration")
    String destHostId;

    @SerializedName("destinationhostname")
    @Param(description = "Destination host for VM migration")
    String destHostName;

    @SerializedName(ApiConstants.JOB_ID)
    @Param(description = "id of VM migration async job")
    private Long jobId;

    @SerializedName(ApiConstants.JOB_STATUS)
    @Param(description = "Job status of VM migration async job")
    private JobInfo.Status jobStatus;


    public ClusterDrsPlanMigrationResponse(String vmId, String vmName, String srcHostId, String srcHostName,
                                           String destHostId, String destHostName, Long jobId,
                                           JobInfo.Status jobStatus) {
        this.vmId = vmId;
        this.vmName = vmName;
        this.srcHostId = srcHostId;
        this.srcHostName = srcHostName;
        this.destHostId = destHostId;
        this.destHostName = destHostName;
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.setObjectName(ApiConstants.MIGRATIONS);
    }
}
