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
import com.cloud.storage.StoragePoolStatus;
import com.google.gson.annotations.SerializedName;

public class StoragePoolResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the storage pool")
    private IdentityProxy id = new IdentityProxy("storage_pool");

    @SerializedName("zoneid") @Param(description="the Zone ID of the storage pool")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("zonename") @Param(description="the Zone name of the storage pool")
    private String zoneName;

    @SerializedName("podid") @Param(description="the Pod ID of the storage pool")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    @SerializedName("podname") @Param(description="the Pod name of the storage pool")
    private String podName;

    @SerializedName("name") @Param(description="the name of the storage pool")
    private String name;

    @SerializedName("ipaddress") @Param(description="the IP address of the storage pool")
    private String ipAddress;

    @SerializedName("path") @Param(description="the storage pool path")
    private String path;

    @SerializedName("created") @Param(description="the date and time the storage pool was created")
    private Date created;

    @SerializedName("type") @Param(description="the storage pool type")
    private String type;

    @SerializedName("clusterid") @Param(description="the ID of the cluster for the storage pool")
    private IdentityProxy clusterId = new IdentityProxy("cluster");

    @SerializedName("clustername") @Param(description="the name of the cluster for the storage pool")
    private String clusterName;

    @SerializedName("disksizetotal") @Param(description="the total disk size of the storage pool")
    private Long diskSizeTotal;

    @SerializedName("disksizeallocated") @Param(description="the host's currently allocated disk size")
    private Long diskSizeAllocated;
    
    @SerializedName("disksizeused") @Param(description="the host's currently used disk size")
    private Long diskSizeUsed;

    @SerializedName("tags") @Param(description="the tags for the storage pool")
    private String tags;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the storage pool")
    private StoragePoolStatus state;

    @Override
    public Long getObjectId() {
        return getId();
    }

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Long getDiskSizeTotal() {
        return diskSizeTotal;
    }

    public void setDiskSizeTotal(Long diskSizeTotal) {
        this.diskSizeTotal = diskSizeTotal;
    }

    public Long getDiskSizeAllocated() {
        return diskSizeAllocated;
    }

    public void setDiskSizeAllocated(Long diskSizeAllocated) {
        this.diskSizeAllocated = diskSizeAllocated;
    }

    public Long getDiskSizeUsed() {
		return diskSizeUsed;
	}

	public void setDiskSizeUsed(Long diskSizeUsed) {
		this.diskSizeUsed = diskSizeUsed;
	}

	public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public StoragePoolStatus getState() {
        return state;
    }

    public void setState(StoragePoolStatus state) {
        this.state = state;
    }
    
}
