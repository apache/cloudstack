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

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkOfferingResponse extends BaseResponse{
    @SerializedName("id") @Param(description="the id of the network offering")
    private IdentityProxy id = new IdentityProxy("network_offerings");

    @SerializedName("name") @Param(description="the name of the network offering")
    private String name;
    
    @SerializedName("displaytext") @Param(description="an alternate display text of the network offering.")
    private String displayText;
    
    @SerializedName("tags") @Param(description="the tags for the network offering")
    private String tags;
    
    @SerializedName("created") @Param(description="the date this network offering was created")
    private Date created;
    
    @SerializedName("maxconnections") @Param(description="the max number of concurrent connection the network offering supports")
    private Integer maxConnections;
    
    @SerializedName("traffictype") @Param(description="the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage.")
    private String trafficType;
    
    @SerializedName("isdefault") @Param(description="true if network offering is default, false otherwise")
    private Boolean isDefault;
   
    @SerializedName("specifyvlan") @Param(description="true if network offering supports vlans, false otherwise")
    private Boolean specifyVlan;
    
    @SerializedName("availability") @Param(description="availability of the network offering")
    private String availability;
    
    @SerializedName(ApiConstants.GUEST_IP_TYPE) @Param(description="guest ip type of the network offering")
    private String guestIpType;
    
    @SerializedName(ApiConstants.NETWORKRATE) @Param(description="data transfer rate in megabits per second allowed.")
    private Integer networkRate;

    @SerializedName(ApiConstants.REDUNDANT_ROUTER) @Param(description="true if redundant router would be enabled, false otherwise")
    private Boolean redundantRouter;
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getMaxconnections() {
        return maxConnections;
    }

    public void setMaxconnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Boolean getSpecifyVlan() {
        return specifyVlan;
    }

    public void setSpecifyVlan(Boolean specifyVlan) {
        this.specifyVlan = specifyVlan;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public void setNetworkRate(Integer networkRate) {
        this.networkRate = networkRate;
    }

    public String getGuestIpType() {
        return guestIpType;
    }

    public void setGuestIpType(String guestIpType) {
        this.guestIpType = guestIpType;
    }
    
    public Boolean getRedundantRouter() {
        return redundantRouter;
    }

    public void setRedundantRouter(Boolean redundantRouter) {
        this.redundantRouter = redundantRouter;
    }

}
