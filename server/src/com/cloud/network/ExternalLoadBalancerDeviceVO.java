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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * ExternalLoadBalancerDeviceVO contains information of a external load balancer devices (F5/Netscaler VPX,MPX,SDX) added into a deployment
  */

@Entity
@Table(name="external_load_balancer_devices")
public class ExternalLoadBalancerDeviceVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

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

    @Column(name="managed")
    boolean managedDevice;
    
    @Column(name = "parent_host_id")
    private long parentHostId;

    @Column(name = "capacity")
    private long capacity;

    public enum LBDeviceState {
        Enabled,
        Disabled
    }

    public enum LBDeviceAllocationState {
        Free,           // In this state no networks are using this device for load balancing
        InSharedUse,    // In this state one or more networks will be using this device for load balancing
        InDedicatedUse  // In this state this device is dedicated for a single network
    }

    public enum LBDeviceManagedType {
        CloudManaged,     // Cloudstack managed load balancer (e.g. VPX instances on SDX for now, in future releases load balancer provisioned from template)
        ExternalManaged   // Externally managed
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = provider_name;
        this.deviceName = device_name;
        this.hostId = hostId;
        this.state = LBDeviceState.Disabled;
        this.allocationState = LBDeviceAllocationState.Free;
        this.managedDevice = false;
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name, boolean managed, long parentHostId) {
        this(hostId, physicalNetworkId, provider_name, device_name);
        this.managedDevice = managed;
        this.parentHostId = parentHostId;
    }
    
    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String provider_name, String device_name, long capacity) {
        this(hostId, physicalNetworkId, provider_name, device_name);
        this.capacity = capacity;
    }

    public ExternalLoadBalancerDeviceVO() {
    
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
}
