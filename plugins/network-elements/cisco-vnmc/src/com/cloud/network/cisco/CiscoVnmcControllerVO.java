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
@Table(name="external_cisco_vnmc_devices")
public class CiscoVnmcControllerVO implements CiscoVnmcController {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="host_id")
    private long hostId;

    @Column(name="physical_network_id")
    private long physicalNetworkId;

    @Column(name="provider_name")
    private String providerName;

    @Column(name="device_name")
    private String deviceName;


    public CiscoVnmcControllerVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public CiscoVnmcControllerVO(long hostId, long physicalNetworkId,
            String providerName, String deviceName) {
        super();
        this.hostId = hostId;
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = providerName;
        this.deviceName = deviceName;
        this.uuid = UUID.randomUUID().toString();
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
    public long getHostId() {
        return hostId;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

}
