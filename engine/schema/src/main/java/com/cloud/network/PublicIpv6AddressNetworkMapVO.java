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

package com.cloud.network;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "public_ip6_address_network_map")
public class PublicIpv6AddressNetworkMapVO implements PublicIpv6AddressNetworkMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vlan_db_id")
    private Long rangeId;

    @Column(name = "public_ip_address")
    private String ip6Address;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "nic_mac_address")
    private String nicMacAddress;

    @Column(name = "state")
    private PublicIpv6AddressNetworkMap.State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    protected PublicIpv6AddressNetworkMapVO() {
        uuid = UUID.randomUUID().toString();
    }

    protected PublicIpv6AddressNetworkMapVO(long rangeId, String ip6Address, Long networkId, String nicMacAddress, State state) {
        this.rangeId = rangeId;
        this.ip6Address = ip6Address;
        this.networkId = networkId;
        this.nicMacAddress = nicMacAddress;
        this.state = state;
        uuid = UUID.randomUUID().toString();
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
    public long getRangeId() {
        return rangeId;
    }

    @Override
    public String getIp6Address() {
        return ip6Address;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    @Override
    public String getNicMacAddress() {
        return nicMacAddress;
    }

    public void setNicMacAddress(String nicMacAddress) {
        this.nicMacAddress = nicMacAddress;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(PublicIpv6AddressNetworkMap.State state) {
        this.state = state;
    }
}
