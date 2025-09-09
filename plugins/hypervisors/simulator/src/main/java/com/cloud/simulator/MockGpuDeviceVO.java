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
package com.cloud.simulator;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.gpu.GpuDevice;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mockgpudevice")
public class MockGpuDeviceVO implements MockGpuDevice, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    // PCI address for parent devices, MDEV UUID for MDEV devices, VF PCI address for VF devices
    @Column(name = "bus_address", nullable = false)
    private String busAddress;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "max_vgpu_per_pgpu", nullable = false)
    private Long maxVgpuPerPgpu = 1L;

    @Column(name = "video_ram", nullable = false)
    private Long videoRam = 0L;

    @Column(name = "max_resolution_x", nullable = false)
    private Long maxResolutionX = 0L;

    @Column(name = "max_resolution_y", nullable = false)
    private Long maxResolutionY = 0L;

    @Column(name = "max_heads", nullable = false)
    private Long maxHeads = 1L;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    private GpuDevice.DeviceType deviceType = GpuDevice.DeviceType.PCI;

    @Column(name = "parent_device_id")
    private Long parentDeviceId;

    @Column(name = "profile_name")
    private String profileName;

    @Column(name = "passthrough_enabled")
    private boolean passthroughEnabled = true;

    @Column(name = "numa_node")
    private Integer numaNode;

    @Column(name = "pci_root")
    private String pciRoot;

    public MockGpuDeviceVO() {
    }

    public MockGpuDeviceVO(String busAddress, String vendorId, String deviceId, String vendorName, String deviceName,
            Long hostId) {
        this.busAddress = busAddress;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.vendorName = vendorName;
        this.deviceName = deviceName;
        this.hostId = hostId;
        this.state = State.Available;
        this.deviceType = GpuDevice.DeviceType.PCI;
        this.profileName = "passthrough";
        this.passthroughEnabled = true;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getBusAddress() {
        return busAddress;
    }

    public void setBusAddress(String busAddress) {
        this.busAddress = busAddress;
    }

    @Override
    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    @Override
    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getMaxVgpuPerPgpu() {
        return maxVgpuPerPgpu;
    }

    public void setMaxVgpuPerPgpu(Long maxVgpuPerGpu) {
        this.maxVgpuPerPgpu = maxVgpuPerGpu;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
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

    public Long getMaxHeads() {
        return maxHeads;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    public GpuDevice.DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(GpuDevice.DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public Long getParentDeviceId() {
        return parentDeviceId;
    }

    public void setParentDeviceId(Long parentDeviceId) {
        this.parentDeviceId = parentDeviceId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public boolean isPassthroughEnabled() {
        return passthroughEnabled;
    }

    public void setPassthroughEnabled(boolean passthroughEnabled) {
        this.passthroughEnabled = passthroughEnabled;
    }

    public Integer getNumaNode() {
        return numaNode;
    }

    public void setNumaNode(Integer numaNode) {
        this.numaNode = numaNode;
    }

    public String getPciRoot() {
        return pciRoot;
    }

    public void setPciRoot(String pciRoot) {
        this.pciRoot = pciRoot;
    }

    /**
     * Helper method to get the MDEV UUID (when device_type is MDEV)
     *
     * @return MDEV UUID or null if not an MDEV device
     */
    public String getMdevUuid() {
        return GpuDevice.DeviceType.MDEV.equals(this.deviceType) ? this.busAddress : null;
    }

    /**
     * Helper method to get the VF PCI address (when device_type is PCI and has
     * parent)
     *
     * @return VF PCI address or null if not a VF device
     */
    public String getVfPciAddress() {
        return GpuDevice.DeviceType.PCI.equals(this.deviceType) && this.parentDeviceId != null ? this.busAddress : null;
    }

    /**
     * Helper method to get the parent PCI bus address (when device_type is PCI and
     * no parent)
     *
     * @return Parent PCI bus address or null if not a parent device
     */
    public String getParentPciBusAddress() {
        return GpuDevice.DeviceType.PCI.equals(this.deviceType) && this.parentDeviceId == null ? this.busAddress : null;
    }
}
