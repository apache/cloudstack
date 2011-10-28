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

import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkTrafficType;

@Entity
@Table(name = "physical_network_traffic_types")
public class PhysicalNetworkTrafficTypeVO implements PhysicalNetworkTrafficType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;
    
    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;

    @Column(name = "xen_network_label")
    private String xenNetworkLabel;

    @Column(name = "kvm_network_label")
    private String kvmNetworkLabel;

    @Column(name = "vmware_network_label")
    private String vmwareNetworkLabel;
    
    public PhysicalNetworkTrafficTypeVO() {
    }

    public PhysicalNetworkTrafficTypeVO(long physicalNetworkId, TrafficType trafficType, String xenLabel, String kvmLabel, String vmwareLabel) {
        this.physicalNetworkId = physicalNetworkId;
        this.trafficType = trafficType;
        this.xenNetworkLabel = xenLabel;
        this.kvmNetworkLabel = kvmLabel;
        this.vmwareNetworkLabel = vmwareLabel;
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
    public TrafficType getTrafficType() {
        return trafficType;
    }
    
    public void setXenNetworkLabel(String xenNetworkLabel) {
        this.xenNetworkLabel = xenNetworkLabel;
    }

    @Override
    public String getXenNetworkLabel() {
        return xenNetworkLabel;
    }

    public void setKvmNetworkLabel(String kvmNetworkLabel) {
        this.kvmNetworkLabel = kvmNetworkLabel;
    }

    @Override
    public String getKvmNetworkLabel() {
        return kvmNetworkLabel;
    }

    public void setVmwareNetworkLabel(String vmwareNetworkLabel) {
        this.vmwareNetworkLabel = vmwareNetworkLabel;
    }

    @Override
    public String getVmwareNetworkLabel() {
        return vmwareNetworkLabel;
    }    

}
