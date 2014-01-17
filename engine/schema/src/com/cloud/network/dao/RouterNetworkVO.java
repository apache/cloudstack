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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;

@Entity
@Table(name = "router_network_ref")
public class RouterNetworkVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "router_id")
    long routerId;

    @Column(name = "network_id")
    long networkId;

    @Column(name = "guest_type")
    @Enumerated(value = EnumType.STRING)
    Network.GuestType guestType;

    protected RouterNetworkVO() {
    }

    public RouterNetworkVO(long routerId, long networkId, GuestType guestType) {
        this.networkId = networkId;
        this.routerId = routerId;
        this.guestType = guestType;
    }

    public long getRouterId() {
        return routerId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public Network.GuestType getGuestType() {
        return guestType;
    }

    @Override
    public long getId() {
        return id;
    }
}
