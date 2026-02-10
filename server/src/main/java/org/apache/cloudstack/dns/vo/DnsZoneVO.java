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

package org.apache.cloudstack.dns.vo;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.dns.DnsZone;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "dns_zone")
public class DnsZoneVO implements DnsZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "dns_server_id")
    private long dnsServerId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ZoneType type;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = GenericDao.CREATED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public DnsZoneVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public DnsZoneVO(String name, long dnsServerId, long accountId) {
        this();
        this.name = name;
        this.dnsServerId = dnsServerId;
        this.accountId = accountId;
        this.type = ZoneType.Public;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getDnsServerId() {
        return dnsServerId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public ZoneType getType() {
        return type;
    }

    @Override
    public List<Long> getAssociatedNetworks() {
        return List.of();
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }
}
