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

import com.cloud.utils.net.Ip;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = ("tungsten_guest_network_ip_address"))
public class TungstenGuestNetworkIpAddressVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "public_ip_address")
    @Enumerated(value = EnumType.STRING)
    private Ip publicIpAddress = null;

    @Column(name = "guest_ip_address")
    @Enumerated(value = EnumType.STRING)
    private Ip guestIpAddress = null;

    @Column(name = "logical_router_uuid")
    private String logicalRouterUuid;

    public TungstenGuestNetworkIpAddressVO() {
    }

    public TungstenGuestNetworkIpAddressVO(long networkId, Ip guestIpAddress) {
        this.networkId = networkId;
        this.guestIpAddress = guestIpAddress;
    }

    public TungstenGuestNetworkIpAddressVO(long networkId, Ip guestIpAddress, String logicalRouterUuid) {
        this.networkId = networkId;
        this.guestIpAddress = guestIpAddress;
        this.logicalRouterUuid = logicalRouterUuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final long networkId) {
        this.networkId = networkId;
    }

    public Ip getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(final Ip publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public Ip getGuestIpAddress() {
        return guestIpAddress;
    }

    public void setGuestIpAddress(final Ip guestIpAddress) {
        this.guestIpAddress = guestIpAddress;
    }

    public String getLogicalRouterUuid() {
        return logicalRouterUuid;
    }

    public void setLogicalRouterUuid(final String logicalRouterUuid) {
        this.logicalRouterUuid = logicalRouterUuid;
    }
}
