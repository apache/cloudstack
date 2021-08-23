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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = UnmanagedInstanceTO.class)
public class UnmanagedInstanceResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the virtual machine")
    private String name;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "the ID of the cluster to which virtual machine belongs")
    private String clusterId;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the ID of the host to which virtual machine belongs")
    private String hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the host to which virtual machine belongs")
    private String hostName;

    @SerializedName(ApiConstants.POWER_STATE)
    @Param(description = "the power state of the virtual machine")
    private String  powerState;

    @SerializedName(ApiConstants.CPU_NUMBER)
    @Param(description = "the CPU cores of the virtual machine")
    private Integer cpuCores;

    @SerializedName(ApiConstants.CPU_CORE_PER_SOCKET)
    @Param(description = "the CPU cores per socket for the virtual machine. VMware specific")
    private Integer cpuCoresPerSocket;

    @SerializedName(ApiConstants.CPU_SPEED)
    @Param(description = "the CPU speed of the virtual machine")
    private Integer cpuSpeed;

    @SerializedName(ApiConstants.MEMORY)
    @Param(description = "the memory of the virtual machine in MB")
    private Integer memory;

    @SerializedName(ApiConstants.OS_ID)
    @Param(description = "the operating system ID of the virtual machine")
    private String operatingSystemId;

    @SerializedName(ApiConstants.OS_DISPLAY_NAME)
    @Param(description = "the operating system of the virtual machine")
    private String operatingSystem;

    @SerializedName(ApiConstants.DISK)
    @Param(description = "the list of disks associated with the virtual machine", responseObject = UnmanagedInstanceDiskResponse.class)
    private Set<UnmanagedInstanceDiskResponse> disks;

    @SerializedName(ApiConstants.NIC)
    @Param(description = "the list of nics associated with the virtual machine", responseObject = NicResponse.class)
    private Set<NicResponse> nics;

    public UnmanagedInstanceResponse() {
        disks = new LinkedHashSet<UnmanagedInstanceDiskResponse>();
        nics = new LinkedHashSet<NicResponse>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPowerState() {
        return powerState;
    }

    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getCpuCoresPerSocket() {
        return cpuCoresPerSocket;
    }

    public void setCpuCoresPerSocket(Integer cpuCoresPerSocket) {
        this.cpuCoresPerSocket = cpuCoresPerSocket;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public String getOperatingSystemId() {
        return operatingSystemId;
    }

    public void setOperatingSystemId(String operatingSystemId) {
        this.operatingSystemId = operatingSystemId;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public Set<UnmanagedInstanceDiskResponse> getDisks() {
        return disks;
    }

    public void setDisks(Set<UnmanagedInstanceDiskResponse> disks) {
        this.disks = disks;
    }

    public void addDisk(UnmanagedInstanceDiskResponse disk) {
        this.disks.add(disk);
    }

    public Set<NicResponse> getNics() {
        return nics;
    }

    public void setNics(Set<NicResponse> nics) {
        this.nics = nics;
    }

    public void addNic(NicResponse nic) {
        this.nics.add(nic);
    }
}
