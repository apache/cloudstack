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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * PhysicalNetworkExternalDeviceVO contains information about a external network devices (Network service provider instances)
 * added in to a physical network.
  */

@Entity
@Table(name="physical_network_external_devices")
public class PhysicalNetworkExternalDeviceVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "physical_network_service_provider_id")
    private long networkServiceProviderId;

    @Column(name = "host_id")
    private long hostId;

    public PhysicalNetworkExternalDeviceVO(long networkServiceProviderId, long hostId) {
        this.networkServiceProviderId = networkServiceProviderId;
        this.hostId = hostId;
    }
    
    protected PhysicalNetworkExternalDeviceVO(){
    }

    public long getId() {
        return id;
    }

    public long getNetworkServiceProviderId() {
        return networkServiceProviderId;
    }

    public long getHostId() {
        return hostId;
    }
}
