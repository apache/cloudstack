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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("user_ipv6_address"))
public class UserIpv6AddressVO implements UserIpv6Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "ip_address")
    @Enumerated(value = EnumType.STRING)
    private String address = null;

    @Column(name = "data_center_id", updatable = false)
    private long dataCenterId;

    @Column(name = "vlan_id")
    private long vlanId;

    @Column(name = "state")
    private State state;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "source_network_id")
    private Long sourceNetworkId;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "physical_network_id")
    private Long physicalNetworkId;

    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    protected UserIpv6AddressVO() {
        uuid = UUID.randomUUID().toString();
    }

    public UserIpv6AddressVO(String address, long dataCenterId, String macAddress, long vlanDbId) {
        this.address = address;
        this.dataCenterId = dataCenterId;
        vlanId = vlanDbId;
        state = State.Free;
        setMacAddress(macAddress);
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public long getVlanId() {
        return vlanId;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public Long getSourceNetworkId() {
        return sourceNetworkId;
    }

    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setSourceNetworkId(Long sourceNetworkId) {
        this.sourceNetworkId = sourceNetworkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Class<?> getEntityType() {
        return UserIpv6Address.class;
    }
}
