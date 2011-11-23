/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * ExternalFirewallDeviceVO contains information of a external firewall device (Juniper SRX) added into a deployment
  */

@Entity
@Table(name="external_firewall_devices")
public class ExternalFirewallDeviceVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name="device_state")
    @Enumerated(value=EnumType.STRING)
    FirewallDeviceState deviceState;

    @Column(name="is_dedicated")
    private boolean isDedicatedDevice;

    @Column(name = "capacity")
    private long capacity;

    @Column(name = "allocation_state")
    @Enumerated(value=EnumType.STRING)
    private FirewallDeviceAllocationState allocationState;

    //keeping it enum for future possible states Maintenance, Shutdown
    public enum FirewallDeviceState {
        Enabled,
        Disabled
    }

    public enum FirewallDeviceAllocationState {
        Free,
        Allocated
    }

    public ExternalFirewallDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name, long capacity, boolean dedicated) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = provider_name;
        this.deviceName = device_name;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
