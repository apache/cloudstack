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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.VmwareCbtMigrationCycle;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VmwareCbtMigrationCycle.class)
public class VmwareCbtMigrationCycleResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the VMware CBT migration cycle")
    private String id;

    @SerializedName(ApiConstants.CYCLE_NUMBER)
    @Param(description = "the CBT delta synchronization cycle number")
    private int cycleNumber;

    @SerializedName(ApiConstants.SNAPSHOT_MOR)
    @Param(description = "the VMware snapshot managed object reference used for this cycle")
    private String snapshotMor;

    @SerializedName(ApiConstants.CHANGED_BYTES)
    @Param(description = "the changed bytes copied in this cycle")
    private Long changedBytes;

    @SerializedName(ApiConstants.DIRTY_RATE)
    @Param(description = "the dirty rate in bytes per second observed in this cycle")
    private Long dirtyRate;

    @SerializedName(ApiConstants.DURATION)
    @Param(description = "the cycle duration in milliseconds")
    private Long duration;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the cycle state")
    private String state;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the cycle status or failure description")
    private String description;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the VMware CBT migration cycle")
    private Date created;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the last updated date of the VMware CBT migration cycle")
    private Date lastUpdated;

    public void setId(String id) {
        this.id = id;
    }

    public void setCycleNumber(int cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public void setSnapshotMor(String snapshotMor) {
        this.snapshotMor = snapshotMor;
    }

    public void setChangedBytes(Long changedBytes) {
        this.changedBytes = changedBytes;
    }

    public void setDirtyRate(Long dirtyRate) {
        this.dirtyRate = dirtyRate;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
