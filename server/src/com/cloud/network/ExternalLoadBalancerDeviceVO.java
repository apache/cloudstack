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
 * ExternalLoadBalancerDeviceVO contains information on external load balancer devices (F5/Netscaler VPX,MPX,SDX) added into a deployment
  */

@Entity
@Table(name="external_load_balancer_devices")
public class ExternalLoadBalancerDeviceVO {

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

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private LBDeviceState state;

    @Column(name = "allocation_state")
    @Enumerated(value=EnumType.STRING)
    private LBDeviceAllocationState allocationState;

    @Column(name="is_managed")
    private boolean isManagedDevice;

    @Column(name="is_inline")
    private boolean isInlineMode;

    @Column(name="is_dedicated")
    private boolean isDedicatedDevice;

    @Column(name = "parent_host_id")
    private long parentHostId;

    @Column(name = "capacity")
    private long capacity;

    //keeping it enum for future possible states Maintenance, Shutdown
    public enum LBDeviceState {
        Enabled,
        Disabled
    }

    public enum LBDeviceAllocationState {
        Free,      // In this state no networks are using this device for load balancing
        Shared,    // In this state one or more networks will be using this device for load balancing
        Dedicated, // In this state this device is dedicated for a single network
        Provider   // This state is set only for device that can dynamically provision LB appliances
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name,
            long capacity, boolean dedicated, boolean inline) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = provider_name;
        this.deviceName = device_name;
        this.hostId = hostId;
        this.state = LBDeviceState.Disabled;
        this.allocationState = LBDeviceAllocationState.Free;
        this.capacity = capacity;
        this.isDedicatedDevice = dedicated;
        this.isInlineMode = inline;
        this.isManagedDevice = false;
        this.uuid = UUID.randomUUID().toString();

        if (device_name.equalsIgnoreCase(ExternalNetworkDeviceManager.NetworkDevice.NetscalerSDXLoadBalancer.getName())) {
            this.allocationState = LBDeviceAllocationState.Provider;
        }
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name,
            long capacity, boolean dedicated, boolean inline, boolean managed, long parentHostId) {
        this(hostId, physicalNetworkId, provider_name, device_name, capacity, dedicated, inline);
        this.isManagedDevice = managed;
        this.parentHostId = parentHostId;
    }

    public ExternalLoadBalancerDeviceVO() {
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

    public long getParentHostId() {
        return parentHostId;
    }

    public void setParentHostId(long parentHostId) {
        this.parentHostId = parentHostId;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public LBDeviceState getState() {
        return state;
    }

    public void setState(LBDeviceState state) {
        this.state = state;
    }    

    public LBDeviceAllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(LBDeviceAllocationState allocationState) {
        this.allocationState = allocationState;
    }
    
    public boolean getIsManagedDevice() {
        return isManagedDevice;
    }

    public void setIsManagedDevice(boolean managed) {
        this.isManagedDevice = managed;
    }

    public boolean getIsInLineMode () {
        return isInlineMode;
    }

    public void  setIsInlineMode(boolean inline) {
        this.isInlineMode = inline;
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
