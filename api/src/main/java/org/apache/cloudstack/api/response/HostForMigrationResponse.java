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

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Host.class)
public class HostForMigrationResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the host")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the host")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the host")
    private Status state;

    @SerializedName("disconnected")
    @Param(description = "True if the host is disconnected. False otherwise.")
    private Date disconnectedOn;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The host type")
    private Host.Type hostType;

    @SerializedName("oscategoryid")
    @Param(description = "The OS category ID of the host")
    private String osCategoryId;

    @SerializedName("oscategoryname")
    @Param(description = "The OS category name of the host")
    private String osCategoryName;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "The IP address of the host")
    private String ipAddress;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The Zone ID of the host")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The Zone name of the host")
    private String zoneName;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "The Pod ID of the host")
    private String podId;

    @SerializedName("podname")
    @Param(description = "The Pod name of the host")
    private String podName;

    @SerializedName("version")
    @Param(description = "The host version")
    private String version;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "The host hypervisor")
    private HypervisorType hypervisor;

    @SerializedName("cpunumber")
    @Param(description = "The CPU number of the host")
    private Integer cpuNumber;

    @SerializedName("cpuspeed")
    @Param(description = "The CPU speed of the host")
    private Long cpuSpeed;

    @Deprecated
    @SerializedName("cpuallocated")
    @Param(description = "The amount of the host's CPU currently allocated")
    private String cpuAllocated;

    @SerializedName("cpuallocatedvalue")
    @Param(description = "The amount of the host's CPU currently allocated in MHz")
    private Long cpuAllocatedValue;

    @SerializedName("cpuallocatedpercentage")
    @Param(description = "The amount of the host's CPU currently allocated in percentage")
    private String cpuAllocatedPercentage;

    @SerializedName("cpuallocatedwithoverprovisioning")
    @Param(description = "The amount of the host's CPU currently allocated after applying the cpu.overprovisioning.factor")
    private String cpuAllocatedWithOverprovisioning;

    @SerializedName("cpuused")
    @Param(description = "The amount of the host's CPU currently used")
    private String cpuUsed;

    @SerializedName("cpuwithoverprovisioning")
    @Param(description = "The amount of the host's CPU after applying the cpu.overprovisioning.factor ")
    private String cpuWithOverprovisioning;

    @Deprecated
    @SerializedName("memorytotal")
    @Param(description = "The memory total of the host, this parameter is deprecated use memorywithoverprovisioning")
    private Long memoryTotal;

    @SerializedName("memorywithoverprovisioning")
    @Param(description = "The amount of the host's memory after applying the mem.overprovisioning.factor ")
    private String memWithOverprovisioning;

    @SerializedName("averageload")
    @Param(description = "The CPU average load on the host")
    private Long averageLoad;

    @SerializedName("networkkbsread")
    @Param(description = "The incoming Network traffic on the host")
    private Long networkKbsRead;

    @SerializedName("networkkbswrite")
    @Param(description = "The outgoing Network traffic on the host")
    private Long networkKbsWrite;

    @Deprecated
    @SerializedName("memoryallocated")
    @Param(description = "The amount of the host's memory currently allocated")
    private String memoryAllocated;

    @SerializedName("memoryallocatedpercentage")
    @Param(description = "The amount of the host's memory currently allocated in percentage")
    private String memoryAllocatedPercentage;

    @SerializedName("memoryallocatedbytes")
    @Param(description = "The amount of the host's memory currently allocated in bytes")
    private Long memoryAllocatedBytes;

    @SerializedName("memoryused")
    @Param(description = "The amount of the host's memory currently used")
    private Long memoryUsed;

    @SerializedName("disksizetotal")
    @Param(description = "The total disk size of the host")
    private Long diskSizeTotal;

    @SerializedName("disksizeallocated")
    @Param(description = "The host's currently allocated disk size")
    private Long diskSizeAllocated;

    @SerializedName("capabilities")
    @Param(description = "Capabilities of the host")
    private String capabilities;

    @SerializedName("lastpinged")
    @Param(description = "The date and time the host was last pinged")
    private Date lastPinged;

    @SerializedName("managementserverid")
    @Param(description = "The management server ID of the host")
    private Long managementServerId;

    @SerializedName("clusterid")
    @Param(description = "The cluster ID of the host")
    private String clusterId;

    @SerializedName("clustername")
    @Param(description = "The cluster name of the host")
    private String clusterName;

    @SerializedName("clustertype")
    @Param(description = "The cluster type of the cluster that host belongs to")
    private String clusterType;

    @SerializedName("islocalstorageactive")
    @Param(description = "True if local storage is active, false otherwise")
    private Boolean localStorageActive;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date and time the host was created")
    private Date created;

    @SerializedName("removed")
    @Param(description = "The date and time the host was removed")
    private Date removed;

    @SerializedName("events")
    @Param(description = "Events available for the host")
    private String events;

    @SerializedName("hosttags")
    @Param(description = "Comma-separated list of tags for the host")
    private String hostTags;

    @SerializedName("hasenoughcapacity")
    @Param(description = "True if this host has enough CPU and RAM capacity to migrate an Instance to it, false otherwise")
    private Boolean hasEnoughCapacity;

    @SerializedName("suitableformigration")
    @Param(description = "True if this host is suitable(has enough capacity and satisfies all conditions like hosttags, " +
        "max guests Instance limit etc) to migrate an Instance to it , false otherwise")
    private Boolean suitableForMigration;

    @SerializedName("requiresStorageMotion")
    @Param(description = "True if migrating an Instance to this host requires storage motion, false otherwise")
    private Boolean requiresStorageMotion;

    @SerializedName("resourcestate")
    @Param(description = "The resource state of the host")
    private String resourceState;

    @SerializedName(ApiConstants.HYPERVISOR_VERSION)
    @Param(description = "The hypervisor version")
    private String hypervisorVersion;

    @SerializedName(ApiConstants.HA_HOST)
    @Param(description = "True if the host is HA host (dedicated to Instances started by HA process; false otherwise")
    private Boolean haHost;

    @Override
    public String getObjectId() {
        return getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(Status state) {
        this.state = state;
    }

    public void setDisconnectedOn(Date disconnectedOn) {
        this.disconnectedOn = disconnectedOn;
    }

    public void setHostType(Host.Type hostType) {
        this.hostType = hostType;
    }

    public void setOsCategoryId(String osCategoryId) {
        this.osCategoryId = osCategoryId;
    }

    public void setOsCategoryName(String osCategoryName) {
        this.osCategoryName = osCategoryName;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHypervisor(HypervisorType hypervisor) {
        this.hypervisor = hypervisor;
    }

    public void setCpuNumber(Integer cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public void setCpuSpeed(Long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public String getCpuAllocated() {
        return cpuAllocated;
    }

    public void setCpuAllocated(String cpuAllocated) {
        this.cpuAllocated = cpuAllocated;
    }

    public void setCpuAllocatedValue(Long cpuAllocatedValue) {
        this.cpuAllocatedValue = cpuAllocatedValue;
    }

    public void setCpuAllocatedPercentage(String cpuAllocatedPercentage) {
        this.cpuAllocatedPercentage = cpuAllocatedPercentage;
    }

    public void setCpuAllocatedWithOverprovisioning(String cpuAllocatedWithOverprovisioning) {
        this.cpuAllocatedWithOverprovisioning = cpuAllocatedWithOverprovisioning;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public void setAverageLoad(Long averageLoad) {
        this.averageLoad = averageLoad;
    }

    public void setNetworkKbsRead(Long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
    }

    public void setNetworkKbsWrite(Long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
    }

    public void setMemoryAllocated(String memoryAllocated) {
        this.memoryAllocated = memoryAllocated;
    }

    public void setMemoryAllocatedPercentage(String memoryAllocatedPercentage) {
        this.memoryAllocatedPercentage = memoryAllocatedPercentage;
    }

    public void setMemoryAllocatedBytes(Long memoryAllocatedBytes) {
        this.memoryAllocatedBytes = memoryAllocatedBytes;
    }

    public void setMemoryUsed(Long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public void setDiskSizeTotal(Long diskSizeTotal) {
        this.diskSizeTotal = diskSizeTotal;
    }

    public void setDiskSizeAllocated(Long diskSizeAllocated) {
        this.diskSizeAllocated = diskSizeAllocated;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public void setLastPinged(Date lastPinged) {
        this.lastPinged = lastPinged;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public void setLocalStorageActive(Boolean localStorageActive) {
        this.localStorageActive = localStorageActive;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public String getHostTags() {
        return hostTags;
    }

    public void setHostTags(String hostTags) {
        this.hostTags = hostTags;
    }

    public void setHasEnoughCapacity(Boolean hasEnoughCapacity) {
        this.hasEnoughCapacity = hasEnoughCapacity;
    }

    public void setSuitableForMigration(Boolean suitableForMigration) {
        this.suitableForMigration = suitableForMigration;
    }

    public void setRequiresStorageMotion(Boolean requiresStorageMotion) {
        this.requiresStorageMotion = requiresStorageMotion;
    }

    public String getResourceState() {
        return resourceState;
    }

    public void setResourceState(String resourceState) {
        this.resourceState = resourceState;
    }

    public String getCpuWithOverprovisioning() {
        return cpuWithOverprovisioning;
    }

    public void setCpuWithOverprovisioning(String cpuWithOverprovisioning) {
        this.cpuWithOverprovisioning = cpuWithOverprovisioning;
    }

    public void setMemWithOverprovisioning(String memWithOverprovisioning){
        this.memWithOverprovisioning=memWithOverprovisioning;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public Boolean getHaHost() {
        return haHost;
    }

    public void setHaHost(Boolean haHost) {
        this.haHost = haHost;
    }

    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }
}
