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

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapacityResponse extends BaseResponse {
    @SerializedName(ApiConstants.TYPE) @Param(description="the capacity type")
    private Short capacityType;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("zonename") @Param(description="the Zone name")
    private String zoneName;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    @SerializedName("podname") @Param(description="the Pod name")
    private String podName;
    
    @SerializedName(ApiConstants.CLUSTER_ID) @Param(description="the Cluster ID")
    private IdentityProxy clusterId = new IdentityProxy("cluster");

    @SerializedName("clustername") @Param(description="the Cluster name")
    private String clusterName;

    @SerializedName("capacityused") @Param(description="the capacity currently in use")
    private Long capacityUsed;

    @SerializedName("capacitytotal") @Param(description="the total capacity available")
    private Long capacityTotal;

    @SerializedName("percentused") @Param(description="the percentage of capacity currently in use")
    private String percentUsed;

    public Short getCapacityType() {
        return capacityType;
    }

    public void setCapacityType(Short capacityType) {
        this.capacityType = capacityType;
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

    public Long getClusterId() {
		return clusterId.getValue();
	}

	public void setClusterId(Long clusterId) {
		this.clusterId.setValue(clusterId);
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
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
