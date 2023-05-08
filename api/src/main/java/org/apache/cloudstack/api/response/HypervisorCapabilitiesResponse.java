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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = HypervisorCapabilities.class)
public class HypervisorCapabilitiesResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the hypervisor capabilities row")
    private String id;

    @SerializedName(ApiConstants.HYPERVISOR_VERSION)
    @Param(description = "the hypervisor version")
    private String hypervisorVersion;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "the hypervisor type")
    private HypervisorType hypervisor;

    @SerializedName(ApiConstants.MAX_GUESTS_LIMIT)
    @Param(description = "the maximum number of guest vms recommended for this hypervisor")
    private Long maxGuestsLimit;

    @SerializedName(ApiConstants.SECURITY_GROUP_EANBLED)
    @Param(description = "true if security group is supported")
    private boolean isSecurityGroupEnabled;

    @SerializedName(ApiConstants.MAX_DATA_VOLUMES_LIMIT)
    @Param(description = "the maximum number of Data Volumes that can be attached for this hypervisor")
    private Integer maxDataVolumesLimit;

    @SerializedName(ApiConstants.MAX_HOSTS_PER_CLUSTER)
    @Param(description = "the maximum number of Hosts per cluster for this hypervisor")
    private Integer maxHostsPerCluster;

    @SerializedName(ApiConstants.STORAGE_MOTION_ENABLED)
    @Param(description = "true if storage motion is supported")
    private boolean isStorageMotionSupported;

    @SerializedName(ApiConstants.VM_SNAPSHOT_ENABELD)
    @Param(description = "true if VM snapshots are enabled for this hypervisor")
    private boolean isVmSnapshotEnabled;

    public HypervisorCapabilitiesResponse(){
        super("hypervisorcapabilities");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public HypervisorType getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(HypervisorType hypervisor) {
        this.hypervisor = hypervisor;
    }

    public Long getMaxGuestsLimit() {
        return maxGuestsLimit;
    }

    public void setMaxGuestsLimit(Long maxGuestsLimit) {
        this.maxGuestsLimit = maxGuestsLimit;
    }

    public Boolean getIsSecurityGroupEnabled() {
        return this.isSecurityGroupEnabled;
    }

    public void setIsSecurityGroupEnabled(Boolean sgEnabled) {
        this.isSecurityGroupEnabled = sgEnabled;
    }

    public Boolean getIsStorageMotionSupported() {
        return this.isStorageMotionSupported;
    }

    public void setIsStorageMotionSupported(Boolean smSupported) {
        this.isStorageMotionSupported = smSupported;
    }

    public Integer getMaxDataVolumesLimit() {
        return maxDataVolumesLimit;
    }

    public void setMaxDataVolumesLimit(Integer maxDataVolumesLimit) {
        this.maxDataVolumesLimit = maxDataVolumesLimit;
    }

    public Integer getMaxHostsPerCluster() {
        return maxHostsPerCluster;
    }

    public void setMaxHostsPerCluster(Integer maxHostsPerCluster) {
        this.maxHostsPerCluster = maxHostsPerCluster;
    }

    public boolean isVmSnapshotEnabled() {
        return isVmSnapshotEnabled;
    }

    public void setVmSnapshotEnabled(boolean vmSnapshotEnabled) {
        isVmSnapshotEnabled = vmSnapshotEnabled;
    }
}
