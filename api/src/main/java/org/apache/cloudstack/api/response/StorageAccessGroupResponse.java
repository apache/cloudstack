// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import com.cloud.serializer.Param;

public class StorageAccessGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the storage access group")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the storage access group")
    private String name;

    @SerializedName("hosts")
    @Param(description = "List of Hosts in the Storage Access Group")
    private ListResponse<HostResponse> hostResponseList;

    @SerializedName("clusters")
    @Param(description = "List of Clusters in the Storage Access Group")
    private ListResponse<ClusterResponse> clusterResponseList;

    @SerializedName("pods")
    @Param(description = "List of Pods in the Storage Access Group")
    private ListResponse<PodResponse> podResponseList;

    @SerializedName("zones")
    @Param(description = "List of Zones in the Storage Access Group")
    private ListResponse<ZoneResponse> zoneResponseList;

    @SerializedName("storagepools")
    @Param(description = "List of Storage Pools in the Storage Access Group")
    private ListResponse<StoragePoolResponse> storagePoolResponseList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ListResponse<HostResponse> getHostResponseList() {
        return hostResponseList;
    }

    public void setHostResponseList(ListResponse<HostResponse> hostResponseList) {
        this.hostResponseList = hostResponseList;
    }

    public ListResponse<ClusterResponse> getClusterResponseList() {
        return clusterResponseList;
    }

    public void setClusterResponseList(ListResponse<ClusterResponse> clusterResponseList) {
        this.clusterResponseList = clusterResponseList;
    }

    public ListResponse<PodResponse> getPodResponseList() {
        return podResponseList;
    }

    public void setPodResponseList(ListResponse<PodResponse> podResponseList) {
        this.podResponseList = podResponseList;
    }

    public ListResponse<ZoneResponse> getZoneResponseList() {
        return zoneResponseList;
    }

    public void setZoneResponseList(ListResponse<ZoneResponse> zoneResponseList) {
        this.zoneResponseList = zoneResponseList;
    }

    public ListResponse<StoragePoolResponse> getStoragePoolResponseList() {
        return storagePoolResponseList;
    }

    public void setStoragePoolResponseList(ListResponse<StoragePoolResponse> storagePoolResponseList) {
        this.storagePoolResponseList = storagePoolResponseList;
    }
}
