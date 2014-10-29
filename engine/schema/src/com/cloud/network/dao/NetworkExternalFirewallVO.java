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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

/**
 * NetworkExternalFirewallVO contains information on the networks that are using external firewall
  */

@Entity
@Table(name = "network_external_firewall_device_map")
public class NetworkExternalFirewallVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "external_firewall_device_id")
    private long externalFirewallDeviceId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public NetworkExternalFirewallVO(long networkId, long externalFirewallDeviceId) {
        this.networkId = networkId;
        this.externalFirewallDeviceId = externalFirewallDeviceId;
        this.uuid = UUID.randomUUID().toString();
    }

    public NetworkExternalFirewallVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public long getExternalFirewallDeviceId() {
        return externalFirewallDeviceId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
