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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.org.Cluster;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Cluster.class)
public class ClusterResponse extends BaseResponseWithAnnotations {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the cluster ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the cluster name")
    private String name;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "the Pod ID of the cluster")
    private String podId;

    @SerializedName("podname")
    @Param(description = "the Pod name of the cluster")
    private String podName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID of the cluster")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name of the cluster")
    private String zoneName;

    @SerializedName("hypervisortype")
    @Param(description = "the hypervisor type of the cluster")
    private String hypervisorType;

    @SerializedName("clustertype")
    @Param(description = "the type of the cluster")
    private String clusterType;

    @SerializedName("allocationstate")
    @Param(description = "the allocation state of the cluster")
    private String allocationState;

    @SerializedName("managedstate")
    @Param(description = "whether this cluster is managed by cloudstack")
    private String managedState;

    @SerializedName("capacity")
    @Param(description = "the capacity of the Cluster", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacitites;

    @SerializedName("cpuovercommitratio")
    @Param(description = "The cpu overcommit ratio of the cluster")
    private String cpuovercommitratio;

    @SerializedName("memoryovercommitratio")
    @Param(description = "The memory overcommit ratio of the cluster")
    private String memoryovercommitratio;

    @SerializedName("ovm3vip")
    @Param(description = "Ovm3 VIP to use for pooling and/or clustering")
    private String ovm3vip;

    @SerializedName(ApiConstants.RESOURCE_DETAILS)
    @Param(description = "Meta data associated with the zone (key/value pairs)")
    private Map<String, String> resourceDetails;

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

    public String getPodId() {
        return podId;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
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

    public void setCpuOvercommitRatio(String cpuovercommitratio) {
        this.cpuovercommitratio = cpuovercommitratio;
    }

    public String getCpuOvercommitRatio() {
        return cpuovercommitratio;
    }

    public void setMemoryOvercommitRatio(String memoryovercommitratio) {
        this.memoryovercommitratio = memoryovercommitratio;
    }

    public String getMemoryOvercommitRatio() {
        return memoryovercommitratio;
    }

    public void setOvm3Vip(String ovm3vip) {
        this.ovm3vip = ovm3vip;
    }

    public String getOvm3Vip() {
        return ovm3vip;
    }

    public void setResourceDetails(Map<String, String> details) {
        if (details == null) {
            return;
        }
        resourceDetails = new HashMap<>(details);
        if (resourceDetails.containsKey("username")) {
            resourceDetails.remove("username");
        }
        if (resourceDetails.containsKey("password")) {
            resourceDetails.remove("password");
        }
    }

    public Map<String, String> getResourceDetails() {
        return resourceDetails;
    }
}
