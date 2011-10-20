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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.PhysicalNetworkServiceProvider;

@Entity
@Table(name = "physical_network_service_providers")
public class PhysicalNetworkServiceProviderVO implements PhysicalNetworkServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;
    
    @Column(name = "destination_physical_network_id")
    private long destPhysicalNetworkId;
    

    @Column(name = "provider_name")
    private String providerName;

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;
    
    public PhysicalNetworkServiceProviderVO() {
    }

    public PhysicalNetworkServiceProviderVO(long physicalNetworkId, String name) {
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = name;
        this.state = State.Disabled;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }


    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void setState(State state) {
        this.state = state;
    }    

    @Override
    public String getProviderName() {
        return providerName;
    }

    public void setDestinationPhysicalNetworkId(long destPhysicalNetworkId) {
        this.destPhysicalNetworkId = destPhysicalNetworkId;
    }
    
    @Override
    public long getDestinationPhysicalNetworkId() {
        return destPhysicalNetworkId;
    }
}
