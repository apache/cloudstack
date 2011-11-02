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

import java.util.List;

import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PodResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the Pod")
    private IdentityProxy id = new IdentityProxy("host_pod_ref");

    @SerializedName("name") @Param(description="the name of the Pod")
    private String name;

    @SerializedName("zoneid") @Param(description="the Zone ID of the Pod")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("zonename") @Param(description="the Zone name of the Pod")
    private String zoneName;

    @SerializedName("gateway") @Param(description="the gateway of the Pod")
    private String gateway;

    @SerializedName("netmask") @Param(description="the netmask of the Pod")
    private String netmask;

    @SerializedName("startip") @Param(description="the starting IP for the Pod")
    private String startIp;

    @SerializedName("endip") @Param(description="the ending IP for the Pod")
    private String endIp;
    
    @SerializedName("allocationstate") @Param(description="the allocation state of the Pod")
    private String allocationState;    

    @SerializedName("capacity")  @Param(description="the capacity of the Pod", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacitites;
    
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

    public Long getZoneId() {
        return zoneId.getValue();
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }
    
    public String getAllocationState() {
    	return allocationState;
    }
    
    public void setAllocationState(String allocationState) {
    	this.allocationState = allocationState;
    }

	public List<CapacityResponse> getCapacitites() {
		return capacitites;
	}

	public void setCapacitites(List<CapacityResponse> capacitites) {
		this.capacitites = capacitites;
	}       
}
