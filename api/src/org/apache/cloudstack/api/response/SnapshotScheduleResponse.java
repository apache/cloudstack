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

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class SnapshotScheduleResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the snapshot schedule")
    private String id;

    @SerializedName("volumeid")
    @Param(description = "the volume ID the snapshot schedule applied for")
    private String volumeId;

    @SerializedName("snapshotpolicyid")
    @Param(description = "the snapshot policy ID used by the snapshot schedule")
    private String snapshotPolicyId;

    @SerializedName("scheduled")
    @Param(description = "time the snapshot is scheduled to be taken")
    private Date scheduled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getSnapshotPolicyId() {
        return snapshotPolicyId;
    }

    public void setSnapshotPolicyId(String snapshotPolicyId) {
        this.snapshotPolicyId = snapshotPolicyId;
    }

    public Date getScheduled() {
        return scheduled;
    }

    public void setScheduled(Date scheduled) {
        this.scheduled = scheduled;
    }
}
