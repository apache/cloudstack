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

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=SnapshotPolicy.class)
public class SnapshotPolicyResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the snapshot policy")
    private String id;

    @SerializedName("volumeid") @Param(description="the ID of the disk volume")
    private String volumeId;

    @SerializedName("schedule") @Param(description="time the snapshot is scheduled to be taken.")
    private String schedule;

    @SerializedName("intervaltype") @Param(description="the interval type of the snapshot policy")
    private short intervalType;

    @SerializedName("maxsnaps") @Param(description="maximum number of snapshots retained")
    private int maxSnaps;

    @SerializedName("timezone") @Param(description="the time zone of the snapshot policy")
    private String timezone;

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

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public short getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(short intervalType) {
        this.intervalType = intervalType;
    }

    public int getMaxSnaps() {
        return maxSnaps;
    }

    public void setMaxSnaps(int maxSnaps) {
        this.maxSnaps = maxSnaps;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
