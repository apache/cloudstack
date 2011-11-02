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

import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SnapshotPolicyResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the snapshot policy")
    private IdentityProxy id = new IdentityProxy("snapshot_policy");

    @SerializedName("volumeid") @Param(description="the ID of the disk volume")
    private IdentityProxy volumeId = new IdentityProxy("volumes");

    @SerializedName("schedule") @Param(description="time the snapshot is scheduled to be taken.")
    private String schedule;

    @SerializedName("intervaltype") @Param(description="the interval type of the snapshot policy")
    private short intervalType;

    @SerializedName("maxsnaps") @Param(description="maximum number of snapshots retained")
    private int maxSnaps;

    @SerializedName("timezone") @Param(description="the time zone of the snapshot policy")
    private String timezone;

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
