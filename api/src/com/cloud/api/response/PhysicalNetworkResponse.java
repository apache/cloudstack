/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PhysicalNetworkResponse extends BaseResponse{
    
    @SerializedName(ApiConstants.ID) @Param(description="the id of the physical network")
    private Long id;
    
    @SerializedName(ApiConstants.BROADCAST_DOMAIN_RANGE) @Param(description="Broadcast domain range of the physical network")
    private String broadcastDomainRange;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="zone id of the physical network")
    private Long zoneId;
    
    @SerializedName(ApiConstants.STATE) @Param(description="state of the physical network")
    private String state;

    @SerializedName(ApiConstants.VLAN) @Param(description="the vlan of the physical network")
    private String vlan;
    
    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the physical network owner")
    private Long domainId;
    
    @SerializedName(ApiConstants.TAGS) @Param(description="comma separated tag")
    private String tags;

    @SerializedName(ApiConstants.ISOLATION_METHODS) @Param(description="isolation methods")
    private String isolationMethods;

    @SerializedName(ApiConstants.NETWORK_SPEED) @Param(description="the speed of the physical network")
    private String networkSpeed;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getZoneId() {
        return this.zoneId;
    }

    public void setState(String state) {
        this.state = state;
    }
    
    public String getState() {
        return this.state;
    }
    

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getDomainId() {
        return this.domainId;
    }
    
    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getVlan() {
        return this.vlan;
    }
    

    public void setTags(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }
        
        StringBuilder buf = new StringBuilder();
        for (String tag : tags) {
            buf.append(tag).append(",");
        }
        
        this.tags = buf.delete(buf.length()-1, buf.length()).toString();
    }

    public String getTags() {
        return tags;
    }

    public void setBroadcastDomainRange(String broadcastDomainRange) {
        this.broadcastDomainRange = broadcastDomainRange;
    }

    public String getBroadcastDomainRange() {
        return broadcastDomainRange;
    }

    public void setNetworkSpeed(String networkSpeed) {
        this.networkSpeed = networkSpeed;
    }

    public String getNetworkSpeed() {
        return networkSpeed;
    }

    public void setIsolationMethods(List<String> isolationMethods) {
        if (isolationMethods == null || isolationMethods.size() == 0) {
            return;
        }
        
        StringBuilder buf = new StringBuilder();
        for (String isolationMethod : isolationMethods) {
            buf.append(isolationMethod).append(",");
        }
        
        this.isolationMethods = buf.delete(buf.length()-1, buf.length()).toString();
    }

    public String getIsolationMethods() {
        return isolationMethods;
    }

}
