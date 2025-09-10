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

import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.gpu.GpuDevice;

@EntityReference(value = GpuDevice.class)
public class GpuDeviceResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the GPU device")
    private String id;

    @SerializedName(ApiConstants.BUS_ADDRESS)
    @Param(description = "bus address of the GPU device or MDEV UUID for vGPU devices")
    private String bussAddress;

    @SerializedName(ApiConstants.GPU_DEVICE_TYPE)
    @Param(description = "bus address of the GPU device")
    private GpuDevice.DeviceType type;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the host ID where the GPU device is attached")
    private String hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the host name where the GPU device is attached")
    private String hostName;

    @SerializedName(ApiConstants.GPU_CARD_ID)
    @Param(description = "the GPU card ID associated with this GPU device")
    private String gpuCardId;

    @SerializedName(ApiConstants.GPU_CARD_NAME)
    @Param(description = "the GPU card name associated with this GPU device")
    private String gpuCardName;

    @SerializedName(ApiConstants.VGPU_PROFILE_ID)
    @Param(description = "the vGPU profile ID assigned to this GPU device")
    private String vgpuProfileId;

    @SerializedName(ApiConstants.VGPU_PROFILE_NAME)
    @Param(description = "the vGPU profile name assigned to this GPU device")
    private String vgpuProfileName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the vGPU profile ID assigned to this GPU device")
    private String vmId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "the vGPU profile name assigned to this GPU device")
    private String vmName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_STATE)
    @Param(description = "the state of the virtual machine to which this GPU device is allocated")
    private VirtualMachine.State vmState;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the vGPU profile name assigned to this GPU device")
    private GpuDevice.State state;

    @SerializedName(ApiConstants.MANAGED_STATE)
    @Param(description = "the managed state of the GPU device (Enabled/Disabled)")
    private GpuDevice.ManagedState managedState;

    @SerializedName(ApiConstants.PARENT_GPU_DEVICE_ID)
    @Param(description = "the ID of the parent GPU device, if this is a vGPU")
    private String parentGpuDeviceId;

    @SerializedName(ApiConstants.NUMA_NODE)
    @Param(description = "the NUMA node where the GPU device is located")
    private String numaNode;


    public GpuDeviceResponse() {
        // Empty constructor for serialization
        super("gpudevice");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBussAddress() {
        return bussAddress;
    }

    public void setBussAddress(String bussAddress) {
        this.bussAddress = bussAddress;
    }

    public GpuDevice.DeviceType getType() {
        return type;
    }

    public void setType(GpuDevice.DeviceType type) {
        this.type = type;
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

    public String getGpuCardId() {
        return gpuCardId;
    }

    public void setGpuCardId(String gpuCardId) {
        this.gpuCardId = gpuCardId;
    }

    public String getGpuCardName() {
        return gpuCardName;
    }

    public void setGpuCardName(String gpuCardName) {
        this.gpuCardName = gpuCardName;
    }

    public String getVgpuProfileId() {
        return vgpuProfileId;
    }

    public void setVgpuProfileId(String vgpuProfileId) {
        this.vgpuProfileId = vgpuProfileId;
    }

    public String getVgpuProfileName() {
        return vgpuProfileName;
    }

    public void setVgpuProfileName(String vgpuProfileName) {
        this.vgpuProfileName = vgpuProfileName;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }

    public void setVmState(VirtualMachine.State vmState) {
        this.vmState = vmState;
    }

    public GpuDevice.State getState() {
        return state;
    }

    public void setState(GpuDevice.State state) {
        this.state = state;
    }

    public GpuDevice.ManagedState getManagedState() {
        return managedState;
    }

    public void setManagedState(GpuDevice.ManagedState managedState) {
        this.managedState = managedState;
    }

    public String getParentGpuDeviceId() {
        return parentGpuDeviceId;
    }

    public void setParentGpuDeviceId(String parentGpuDeviceId) {
        this.parentGpuDeviceId = parentGpuDeviceId;
    }

    public String getNumaNode() {
        return numaNode;
    }

    public void setNumaNode(String numaNode) {
        this.numaNode = numaNode;
    }
}
