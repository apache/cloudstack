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
package com.cloud.gpu;

import org.apache.cloudstack.gpu.GpuDevice;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gpu_device")
public class GpuDeviceVO implements GpuDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "card_id")
    private long cardId;

    @Column(name = "vgpu_profile_id")
    private long vgpuProfileId;

    @Column(name = "bus_address")
    private String busAddress;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    private DeviceType type = DeviceType.PCI;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state = State.Free;

    @Column(name = "managed_state")
    @Enumerated(value = EnumType.STRING)
    private ManagedState managedState = ManagedState.Managed;

    @Column(name = "parent_gpu_device_id")
    private Long parentGpuDeviceId;

    @Column(name = "numa_node")
    private String numaNode;

    @Column(name = "pci_root")
    private String pciRoot;

    public GpuDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public GpuDeviceVO(long cardId, long vgpuProfileId, String busAddress, long hostId, Long parentGpuDeviceId,
            String numaNode, String pciRoot) {
        this.uuid = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.vgpuProfileId = vgpuProfileId;
        this.busAddress = busAddress;
        this.hostId = hostId;
        this.parentGpuDeviceId = parentGpuDeviceId;
        this.numaNode = numaNode;
        this.pciRoot = pciRoot;
    }

    @Override
    public String toString() {
        return String.format("GpuDevice %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "cardId", "vgpuProfileId", "busAddress", "hostId", "vmId",
                "parentGpuDeviceId", "numaNode", "pciRoot", "state", "resourceState"));
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public long getVgpuProfileId() {
        return vgpuProfileId;
    }

    public void setVgpuProfileId(long vgpuProfileId) {
        this.vgpuProfileId = vgpuProfileId;
    }

    public String getBusAddress() {
        return busAddress;
    }

    public void setBusAddress(String busAddress) {
        this.busAddress = busAddress;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public ManagedState getManagedState() {
        return managedState;
    }

    public void setManagedState(ManagedState managedState) {
        this.managedState = managedState;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public Long getParentGpuDeviceId() {
        return parentGpuDeviceId;
    }

    public void setParentGpuDeviceId(Long parentGpuDeviceId) {
        this.parentGpuDeviceId = parentGpuDeviceId;
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
}
