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
package com.cloud.user;

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
@Table(name = "account")
public class AccountVO implements Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "type")
    private short type = ACCOUNT_TYPE_NORMAL;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "cleanup_needed")
    private boolean needsCleanup = false;

    @Column(name = "network_domain")
    private String networkDomain;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "default_zone_id")
    private Long defaultZoneId = null;

    @Column(name = "default")
    boolean isDefault;

    public AccountVO() {
        uuid = UUID.randomUUID().toString();
    }

    public AccountVO(long id) {
        this.id = id;
        uuid = UUID.randomUUID().toString();
    }

    public AccountVO(String accountName, long domainId, String networkDomain, short type, String uuid) {
        this.accountName = accountName;
        this.domainId = domainId;
        this.networkDomain = networkDomain;
        this.type = type;
        state = State.enabled;
        this.uuid = uuid;
    }

    public void setNeedsCleanup(boolean value) {
        needsCleanup = value;
    }

    public boolean getNeedsCleanup() {
        return needsCleanup;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    @Override
    public Long getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(Long defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public long getAccountId() {
        return id;
    }

    @Override
    public String toString() {
        return new StringBuilder("Acct[").append(uuid).append("-").append(accountName).append("]").toString();
    }

    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public Class<?> getEntityType() {
        return Account.class;
    }
}
