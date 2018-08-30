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

import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkTrafficType;

@Entity
@Table(name = "physical_network_traffic_types")
public class PhysicalNetworkTrafficTypeVO implements PhysicalNetworkTrafficType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "traffic_type")
    @Enumerated(value = EnumType.STRING)
    TrafficType trafficType;

    @Column(name = "xenserver_network_label")
    private String xenNetworkLabel;

    @Column(name = "kvm_network_label")
    private String kvmNetworkLabel;

    @Column(name = "vmware_network_label")
    private String vmwareNetworkLabel;

    @Column(name = "simulator_network_label")
    private String simulatorNetworkLabel;

    @Column(name = "hyperv_network_label")
    private String hypervNetworkLabel;

    @Column(name = "ovm_network_label")
    private String ovm3NetworkLabel;

    @Column(name = "vlan")
    private String vlan;

    public PhysicalNetworkTrafficTypeVO() {
    }

    public PhysicalNetworkTrafficTypeVO(long physicalNetworkId, TrafficType trafficType, String xenLabel, String kvmLabel, String vmwareLabel, String simulatorLabel,
            String vlan, String hypervLabel, String ovm3Label) {
        this.physicalNetworkId = physicalNetworkId;
        this.trafficType = trafficType;
        this.xenNetworkLabel = xenLabel;
        this.kvmNetworkLabel = kvmLabel;
        this.vmwareNetworkLabel = vmwareLabel;
        this.simulatorNetworkLabel = simulatorLabel;
        this.hypervNetworkLabel = hypervLabel;
        this.ovm3NetworkLabel = ovm3Label;
        this.setVlan(vlan);
        this.uuid = UUID.randomUUID().toString();
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

    public void setSimulatorNetworkLabel(String simulatorNetworkLabel) {
        this.simulatorNetworkLabel = simulatorNetworkLabel;
    }

    @Override
    public String getSimulatorNetworkLabel() {
        return simulatorNetworkLabel;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getVlan() {
        return vlan;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setHypervNetworkLabel(String hypervNetworkLable) {
        this.hypervNetworkLabel = hypervNetworkLable;

    }

    @Override
    public String getHypervNetworkLabel() {
        return hypervNetworkLabel;
    }

    public void setOvm3NetworkLabel(String ovm3NetworkLabel) {
        this.ovm3NetworkLabel = ovm3NetworkLabel;
    }

    @Override
    public String getOvm3NetworkLabel() {
        return ovm3NetworkLabel;
    }
}
