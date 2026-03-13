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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;

public class BackupServiceJobResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "Compression job ID.")
    private Long id;

    @SerializedName(ApiConstants.BACKUP_ID)
    @Param(description = "Backup ID.")
    private String backupId;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "Host where the job is being executed.")
    private String hostId;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone where the job is being executed.")
    private String zoneId;

    @SerializedName(ApiConstants.ATTEMPTS)
    @Param(description = "Number of attempts already made to complete this job.")
    private Integer attempts;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Compression job type.")
    private String type;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Compression job start date.")
    private Date startDate;

    @SerializedName(ApiConstants.SCHEDULED_DATE)
    @Param(description = "Compression job scheduled start date.")
    private Date scheduledDate;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Compression job scheduled removed date.")
    private Date removed;

    public BackupServiceJobResponse(Long id, String backupId, String zoneId, Integer attempts, String type, Date startDate, Date scheduledDate, Date removed) {
        super("backupcompressionjob");
        this.id = id;
        this.backupId = backupId;
        this.zoneId = zoneId;
        this.attempts = attempts;
        this.type = type;
        this.startDate = startDate;
        this.scheduledDate = scheduledDate;
        this.removed = removed;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
