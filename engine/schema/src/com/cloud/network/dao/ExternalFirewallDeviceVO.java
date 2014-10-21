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
package com.cloud.network.dao;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * ExternalFirewallDeviceVO contains information of a external firewall device (Juniper SRX) added into a deployment
  */

@Entity
@Table(name = "external_firewall_devices")
public class ExternalFirewallDeviceVO implements InternalIdentity, Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_state")
    @Enumerated(value = EnumType.STRING)
    FirewallDeviceState deviceState;

    @Column(name = "is_dedicated")
    private boolean isDedicatedDevice;

    @Column(name = "capacity")
    private long capacity;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    private FirewallDeviceAllocationState allocationState;

    //keeping it enum for future possible states Maintenance, Shutdown
    public enum FirewallDeviceState {
        Enabled, Disabled
    }

    public enum FirewallDeviceAllocationState {
        Free, Allocated
    }

    public ExternalFirewallDeviceVO(long hostId, long physicalNetworkId, String providerName, String deviceName, long capacity, boolean dedicated) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = providerName;
        this.deviceName = deviceName;
        this.hostId = hostId;
        this.deviceState = FirewallDeviceState.Disabled;
        this.allocationState = FirewallDeviceAllocationState.Free;
        this.capacity = capacity;
        this.isDedicatedDevice = dedicated;
        this.deviceState = FirewallDeviceState.Enabled;
        this.uuid = UUID.randomUUID().toString();
    }

    public ExternalFirewallDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public long getHostId() {
        return hostId;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public FirewallDeviceState getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(FirewallDeviceState state) {
        this.deviceState = state;
    }

    public FirewallDeviceAllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(FirewallDeviceAllocationState allocationState) {
        this.allocationState = allocationState;
    }

    public boolean getIsDedicatedDevice() {
        return isDedicatedDevice;
    }

    public void setIsDedicatedDevice(boolean isDedicated) {
        isDedicatedDevice = isDedicated;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
