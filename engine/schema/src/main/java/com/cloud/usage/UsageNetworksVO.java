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
package com.cloud.usage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

@Entity
@Table(name = "usage_networks")
public class UsageNetworksVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "network_offering_id")
    private long networkOfferingId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "state")
    private String state;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    protected UsageNetworksVO() {
    }

    public UsageNetworksVO(long id, long networkId, long networkOfferingId, long zoneId, long accountId, long domainId, String state, Date created, Date removed) {
        this.id = id;
        this.networkId = networkId;
        this.networkOfferingId = networkOfferingId;
        this.zoneId = zoneId;
        this.domainId = domainId;
        this.accountId = accountId;
        this.state = state;
        this.created = created;
        this.removed = removed;
    }

    public UsageNetworksVO(long networkId, long networkOfferingId, long zoneId, long accountId, long domainId, String state, Date created, Date removed) {
        this.networkId = networkId;
        this.networkOfferingId = networkOfferingId;
        this.zoneId = zoneId;
        this.domainId = domainId;
        this.accountId = accountId;
        this.state = state;
        this.created = created;
        this.removed = removed;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }

    public long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public void setNetworkOfferingId(long networkOfferingId) {
        this.networkOfferingId = networkOfferingId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
