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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "usage_ip_address")
public class UsageIPAddressVO implements InternalIdentity {
    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "id")
    private long id;

    @Column(name = "public_ip_address")
    private String address = null;

    @Column(name = "is_source_nat")
    private boolean isSourceNat = false;

    @Column(name = "is_system")
    private boolean isSystem = false;

    @Column(name = "assigned")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date assigned = null;

    @Column(name = "released")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date released = null;

    protected UsageIPAddressVO() {
    }

    public UsageIPAddressVO(long id, long accountId, long domainId, long zoneId, String address, boolean isSourceNat, boolean isSystem, Date assigned, Date released) {
        this.id = id;
        this.accountId = accountId;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.address = address;
        this.isSourceNat = isSourceNat;
        this.isSystem = isSystem;
        this.assigned = assigned;
        this.released = released;
    }

    public UsageIPAddressVO(long accountId, String address, Date assigned, Date released) {
        this.accountId = accountId;
        this.address = address;
        this.assigned = assigned;
        this.released = released;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    public long getZoneId() {
        return zoneId;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public boolean isSourceNat() {
        return isSourceNat;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public Date getAssigned() {
        return assigned;
    }

    public Date getReleased() {
        return released;
    }

    public void setReleased(Date released) {
        this.released = released;
    }
}
