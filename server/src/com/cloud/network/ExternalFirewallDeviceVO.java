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
import com.cloud.network.PhysicalNetworkServiceProvider.State;

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

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;

    @Column(name = "capacity")
    private long capacity;

    @Column(name = "capacity_type")
    private String capacity_type;

    @Column(name = "allocation_state")
    @Enumerated(value=EnumType.STRING)
    private AllocationState allocationState;

    public enum AllocationState {
        Free,
        Allocated
    }

    public ExternalFirewallDeviceVO(long hostId, long physicalNetworkId, String provider_name) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = provider_name;
        this.hostId = hostId;
        this.state = PhysicalNetworkServiceProvider.State.Disabled;
        this.allocationState = AllocationState.Free;
    }

    public ExternalFirewallDeviceVO() {

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

    public long getHostId() {
        return hostId;
    }

    public long getCapacity() {
        return capacity;
    }

    public String getCapacityType() {
        return capacity_type;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public AllocationState getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState) {
        this.allocationState = allocationState;
    }
}
