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

import java.util.ArrayList;
import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ClusterResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the cluster ID")
    private IdentityProxy id = new IdentityProxy("cluster");

    @SerializedName(ApiConstants.NAME) @Param(description="the cluster name")
    private String name;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID of the cluster")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    @SerializedName("podname") @Param(description="the Pod name of the cluster")
    private String podName;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID of the cluster")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the Zone name of the cluster")
    private String zoneName;

    @SerializedName("hypervisortype") @Param(description="the hypervisor type of the cluster")
    private String hypervisorType;
    
    @SerializedName("clustertype") @Param(description="the type of the cluster")
    private String clusterType;
    
    @SerializedName("allocationstate") @Param(description="the allocation state of the cluster")
    private String allocationState;
    
    @SerializedName("managedstate") @Param(description="whether this cluster is managed by cloudstack")
    private String managedState;
    
    @SerializedName("capacity")  @Param(description="the capacity of the Cluster", responseObject = CapacityResponse.class)
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

    public Long getPodId() {
        return podId.getValue();
    }

    public void setPodId(Long podId) {
        this.podId.setValue(podId);
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
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
    
    public String getClusterType() {
    	return clusterType;
    }
    
    public void setClusterType(String clusterType) {
    	this.clusterType = clusterType;
    }
    
    public String getHypervisorType() {
    	return this.hypervisorType;
    }
    
    public void setHypervisorType(String hypervisorType) {
    	this.hypervisorType = hypervisorType;
    }
    
    public String getAllocationState() {
    	return allocationState;
    }
    
    public void setAllocationState(String allocationState) {
    	this.allocationState = allocationState;
    }

    public String getManagedState() {
        return managedState;
    }

    public void setManagedState(String managedState) {
        this.managedState = managedState;
    }

	public List<CapacityResponse> getCapacitites() {
		return capacitites;
	}

	public void setCapacitites(ArrayList<CapacityResponse> arrayList) {
		this.capacitites = arrayList;
	}     
}
