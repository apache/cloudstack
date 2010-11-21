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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PreallocatedLunResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the preallocated LUN")
    private Long id;

    @SerializedName("volumeid") @Param(description="the ID of the preallocated LUN")
    private Long volumeId;

    @SerializedName("zoneid") @Param(description="the zone ID of the preallocated LUN")
    private Long zoneId;

    @SerializedName("lun") @Param(description="the name of the preallocated LUN")
    private Integer lun;

    //FIXME - add description
    @SerializedName("portal")
    private String portal;

    @SerializedName("size") @Param(description="the size of the preallocated LUN")
    private Long size;

    @SerializedName("taken") @Param(description="true if the preallocated LUN is used by the volume, false otherwise")
    private Date taken;

    @SerializedName("targetiqn") @Param(description="the target IQN of the preallocated LUN")
    private String targetIqn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getLun() {
        return lun;
    }

    public void setLun(Integer lun) {
        this.lun = lun;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Date getTaken() {
        return taken;
    }

    public void setTaken(Date taken) {
        this.taken = taken;
    }

    public String getTargetIqn() {
        return targetIqn;
    }

    public void setTargetIqn(String targetIqn) {
        this.targetIqn = targetIqn;
    }
}
