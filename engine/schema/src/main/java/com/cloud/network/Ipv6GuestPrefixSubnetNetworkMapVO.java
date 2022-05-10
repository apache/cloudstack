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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "ip6_guest_prefix_subnet_network_map")
public class Ipv6GuestPrefixSubnetNetworkMapVO implements Ipv6GuestPrefixSubnetNetworkMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "prefix_id")
    private Long prefixId;

    @Column(name = "subnet")
    private String subnet;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "state")
    private State state;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated")
    Date updated;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    protected Ipv6GuestPrefixSubnetNetworkMapVO() {
        uuid = UUID.randomUUID().toString();
    }

    protected Ipv6GuestPrefixSubnetNetworkMapVO(long prefixId, String subnet, Long networkId, Ipv6GuestPrefixSubnetNetworkMap.State state) {
        this.prefixId = prefixId;
        this.subnet = subnet;
        this.networkId = networkId;
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
    public long getPrefixId() {
        return prefixId;
    }

    @Override
    public String getSubnet() {
        return subnet;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(Ipv6GuestPrefixSubnetNetworkMap.State state) {
        this.state = state;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getCreated() {
        return created;
    }
}
