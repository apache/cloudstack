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
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

/**
 * ExternalLoadBalancerDeviceVO contains information on external load balancer devices (F5/Netscaler VPX,MPX,SDX) added into a deployment
  */

@Entity
@Table(name = "external_load_balancer_devices")
public class ExternalLoadBalancerDeviceVO implements InternalIdentity, Identity {

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
    private LBDeviceState state;

    @Column(name = "allocation_state")
    @Enumerated(value = EnumType.STRING)
    private LBDeviceAllocationState allocationState;

    @Column(name = "is_managed")
    private boolean isManagedDevice;

    @Column(name = "is_dedicated")
    private boolean isDedicatedDevice;

    @Column(name = "is_gslb_provider")
    private boolean gslbProvider;

    @Column(name = "is_exclusive_gslb_provider")
    private boolean exclusiveGslbProvider;

    @Column(name = "gslb_site_publicip")
    private String gslbSitePublicIP;

    @Column(name = "gslb_site_privateip")
    private String gslbSitePrivateIP;

    @Column(name = "parent_host_id")
    private long parentHostId;

    @Column(name = "capacity")
    private long capacity;

    //keeping it enum for future possible states Maintenance, Shutdown
    public enum LBDeviceState {
        Enabled, Disabled
    }

    public enum LBDeviceAllocationState {
        Free,      // In this state no networks are using this device for load balancing
        Shared,    // In this state one or more networks will be using this device for load balancing
        Dedicated, // In this state this device is dedicated for a single network
        Provider   // This state is set only for device that can dynamically provision LB appliances
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String providerName, String deviceName, long capacity, boolean dedicated,
            boolean gslbProvider) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = providerName;
        this.deviceName = deviceName;
        this.hostId = hostId;
        this.state = LBDeviceState.Disabled;
        this.allocationState = LBDeviceAllocationState.Free;
        this.capacity = capacity;
        this.isDedicatedDevice = dedicated;
        this.isManagedDevice = false;
        this.state = LBDeviceState.Enabled;
        this.uuid = UUID.randomUUID().toString();
        this.gslbProvider = gslbProvider;
        this.gslbSitePublicIP = null;
        this.gslbSitePrivateIP = null;
        if (deviceName.equalsIgnoreCase(ExternalNetworkDeviceManager.NetworkDevice.NetscalerSDXLoadBalancer.getName())) {
            this.allocationState = LBDeviceAllocationState.Provider;
        }
    }

    public ExternalLoadBalancerDeviceVO(long hostId, long physicalNetworkId, String providerName, String deviceName, long capacity, boolean dedicated, boolean managed,
            long parentHostId) {
        this(hostId, physicalNetworkId, providerName, deviceName, capacity, dedicated, false);
        this.isManagedDevice = managed;
        this.parentHostId = parentHostId;
    }

    public ExternalLoadBalancerDeviceVO() {
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

    public boolean getIsDedicatedDevice() {
        return isDedicatedDevice;
    }

    public void setIsDedicatedDevice(boolean isDedicated) {
        isDedicatedDevice = isDedicated;
    }

    public boolean getGslbProvider() {
        return gslbProvider;
    }

    public void setGslbProvider(boolean gslbProvider) {
        this.gslbProvider = gslbProvider;
    }

    public boolean getExclusiveGslbProvider() {
        return exclusiveGslbProvider;
    }

    public void setExclusiveGslbProvider(boolean exclusiveGslbProvider) {
        this.exclusiveGslbProvider = exclusiveGslbProvider;
    }

    public void setGslbSitePublicIP(String gslbSitePublicIP) {
        this.gslbSitePublicIP = gslbSitePublicIP;
    }

    public String getGslbSitePublicIP() {
        return gslbSitePublicIP;
    }

    public void setGslbSitePrivateIP(String gslbSitePrivateIP) {
        this.gslbSitePrivateIP = gslbSitePrivateIP;
    }

    public String getGslbSitePrivateIP() {
        return gslbSitePrivateIP;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return String.format("ExternalLoadBalancerDevice %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "providerName"));
    }
}
