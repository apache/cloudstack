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
package com.cloud.agent.api;

import org.apache.cloudstack.gpu.GpuDevice;

public class VgpuTypesInfo {

    private boolean passthroughEnabled = true;
    private GpuDevice.DeviceType deviceType;
    private String parentBusAddress;
    private String busAddress;
    private String numaNode;
    private String pciRoot;
    private String deviceId;
    private String deviceName;
    private String vendorId;
    private String vendorName;
    private String modelName;
    private String groupName;
    private String vmName;
    private Long maxHeads;
    private Long videoRam;
    private Long maxResolutionX;
    private Long maxResolutionY;
    private Long maxVgpuPerGpu;
    private Long remainingCapacity;
    private Long maxCapacity;
    private boolean display = false;

    public String getModelName() {
        return modelName;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
    }

    public Long getMaxHeads() {
        return maxHeads;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public void setMaxResolutionX(Long maxResolutionX) {
        this.maxResolutionX = maxResolutionX;
    }

    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    public void setMaxResolutionY(Long maxResolutionY) {
        this.maxResolutionY = maxResolutionY;
    }

    public Long getMaxVpuPerGpu() {
        return maxVgpuPerGpu;
    }

    public void setMaxVgpuPerGpu(Long maxVgpuPerGpu) {
        this.maxVgpuPerGpu = maxVgpuPerGpu;
    }

    public Long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public Long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxVmCapacity(Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean isPassthroughEnabled() {
        return passthroughEnabled;
    }

    public void setPassthroughEnabled(boolean passthroughEnabled) {
        this.passthroughEnabled = passthroughEnabled;
    }

    public GpuDevice.DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(GpuDevice.DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getParentBusAddress() {
        return parentBusAddress;
    }

    public void setParentBusAddress(String parentBusAddress) {
        this.parentBusAddress = parentBusAddress;
    }

    public String getBusAddress() {
        return busAddress;
    }

    public void setBusAddress(String busAddress) {
        this.busAddress = busAddress;
    }

    public String getNumaNode() {
        return numaNode;
    }

    public void setNumaNode(String numaNode) {
        this.numaNode = numaNode;
    }

    public String getPciRoot() {
        return pciRoot;
    }

    public void setPciRoot(String pciRoot) {
        this.pciRoot = pciRoot;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public VgpuTypesInfo(GpuDevice.DeviceType deviceType, String groupName, String modelName, String busAddress,
            String vendorId, String vendorName, String deviceId, String deviceName, String numaNode, String pciRoot
    ) {
        this.deviceType = deviceType;
        this.groupName = groupName;
        this.modelName = modelName;
        this.busAddress = busAddress;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.numaNode = numaNode;
        this.pciRoot = pciRoot;
    }

    public VgpuTypesInfo(GpuDevice.DeviceType deviceType, String groupName, String modelName, String busAddress,
                         String vendorId, String vendorName, String deviceId, String deviceName
    ) {
        this.deviceType = deviceType;
        this.groupName = groupName;
        this.modelName = modelName;
        this.busAddress = busAddress;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
    }

    public VgpuTypesInfo(String groupName, String modelName, Long videoRam, Long maxHeads, Long maxResolutionX,
            Long maxResolutionY, Long maxVgpuPerGpu, Long remainingCapacity, Long maxCapacity
    ) {
        this.groupName = groupName;
        this.modelName = modelName;
        this.videoRam = videoRam;
        this.maxHeads = maxHeads;
        this.maxResolutionX = maxResolutionX;
        this.maxResolutionY = maxResolutionY;
        this.maxVgpuPerGpu = maxVgpuPerGpu;
        this.remainingCapacity = remainingCapacity;
        this.maxCapacity = maxCapacity;
    }
}
