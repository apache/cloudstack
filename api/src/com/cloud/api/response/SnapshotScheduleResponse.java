/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.Date;

import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SnapshotScheduleResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the snapshot schedule")
    private IdentityProxy id = new IdentityProxy("snapshot_schedule");

    @SerializedName("volumeid") @Param(description="the volume ID the snapshot schedule applied for")
    private IdentityProxy volumeId = new IdentityProxy("volumes");

    @SerializedName("snapshotpolicyid") @Param(description="the snapshot policy ID used by the snapshot schedule")
    private IdentityProxy snapshotPolicyId = new IdentityProxy("snapshot_policy");

    @SerializedName("scheduled") @Param(description="time the snapshot is scheduled to be taken")
    private Date scheduled;

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public Long getVolumeId() {
        return volumeId.getValue();
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId.setValue(volumeId);
    }

    public Long getSnapshotPolicyId() {
        return snapshotPolicyId.getValue();
    }

    public void setSnapshotPolicyId(Long snapshotPolicyId) {
        this.snapshotPolicyId.setValue(snapshotPolicyId);
    }

    public Date getScheduled() {
        return scheduled;
    }

    public void setScheduled(Date scheduled) {
        this.scheduled = scheduled;
    }
}
