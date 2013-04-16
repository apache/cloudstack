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
package com.cloud.network.cisco;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="external_cisco_asa1000v_devices")
public class CiscoAsa1000vDeviceVO implements CiscoAsa1000vDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="physical_network_id")
    private long physicalNetworkId;

    @Column(name="management_ip")
    private String managementIp;

    @Column(name="in_Port_profile")
    private String inPortProfile;

    @Column(name="cluster_id")
    private long clusterId;

    public CiscoAsa1000vDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public CiscoAsa1000vDeviceVO(long physicalNetworkId,
            String managementIp, String inPortProfile, long clusterId) {
        super();
        this.physicalNetworkId = physicalNetworkId;
        this.managementIp = managementIp;
        this.inPortProfile = inPortProfile;
        this.uuid = UUID.randomUUID().toString();
        this.clusterId = clusterId;
    }

    @Override
	public long getId() {
        return id;
    }

    @Override
	public String getUuid() {
        return uuid;
    }

    @Override
	public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
	public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @Override
	public String getManagementIp() {
        return managementIp;
    }

    @Override
	public String getInPortProfile() {
        return inPortProfile;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

}
