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
import com.google.gson.annotations.SerializedName;

public class NicResponse extends BaseResponse {
    
    @SerializedName("id") @Param(description="the ID of the nic")
    private Long id;

    @SerializedName("networkid") @Param(description="the ID of the corresponding network")
    private Long networkid;
    
    @SerializedName("netmask") @Param(description="the netmask of the nic")
    private String netmask;
    
    @SerializedName("gateway") @Param(description="the gateway of the nic")
    private String gateway;
    
    @SerializedName("ipaddress") @Param(description="the ip address of the nic")
    private String ipaddress;
    
    @SerializedName("isolationuri") @Param(description="the isolation uri of the nic")
    private String isolationUri;
    
    @SerializedName("broadcasturi") @Param(description="the broadcast uri of the nic")
    private String broadcastUri;
    
    //TODO - add description
    @SerializedName("traffictype")
    private String trafficType;
    
    //TODO - add description
    @SerializedName("type")
    private String type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNetworkid() {
        return networkid;
    }

    public void setNetworkid(Long networkid) {
        this.networkid = networkid;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public String getIsolationUri() {
        return isolationUri;
    }

    public void setIsolationUri(String isolationUri) {
        this.isolationUri = isolationUri;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
