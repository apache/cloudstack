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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "dns_zone_network_map")
public class DnsZoneNetworkMapVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "dns_zone_id")
    private long dnsZoneId;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "sub_domain")
    private String subDomain;

    @Column(name = GenericDao.CREATED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public DnsZoneNetworkMapVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public DnsZoneNetworkMapVO(long dnsZoneId, long networkId, String subDomain) {
        this();
        this.dnsZoneId = dnsZoneId;
        this.networkId = networkId;
        this.subDomain = subDomain;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDnsZoneId() {
        return dnsZoneId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getUuid() {
        return uuid;
    }
}
