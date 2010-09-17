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

import com.cloud.serializer.Param;

public class CapacityResponse extends BaseResponse {
    @Param(name="type")
    private Short capacityType;

    @Param(name="zoneid")
    private Long zoneId;

    @Param(name="zonename")
    private String zoneName;

    @Param(name="podid")
    private Long podId;

    @Param(name="podname")
    private String podName;

    @Param(name="capacityused")
    private Long capacityUsed;

    @Param(name="capacityTotal")
    private Long capacityTotal;

    @Param(name="percentused")
    private String percentUsed;

    public Short getCapacityType() {
        return capacityType;
    }

    public void setCapacityType(Short capacityType) {
        this.capacityType = capacityType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public Long getCapacityUsed() {
        return capacityUsed;
    }

    public void setCapacityUsed(Long capacityUsed) {
        this.capacityUsed = capacityUsed;
    }

    public Long getCapacityTotal() {
        return capacityTotal;
    }

    public void setCapacityTotal(Long capacityTotal) {
        this.capacityTotal = capacityTotal;
    }

    public String getPercentUsed() {
        return percentUsed;
    }

    public void setPercentUsed(String percentUsed) {
        this.percentUsed = percentUsed;
    }
}
