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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.Encrypt;

@Entity
@Table(name = ("vpn_users"))
public class VpnUserVO implements VpnUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "owner_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "username")
    private String username;

    @Encrypt
    @Column(name = "password")
    private String password;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "uuid")
    private String uuid;

    public VpnUserVO() {
        uuid = UUID.randomUUID().toString();
    }

    public VpnUserVO(long accountId, long domainId, String userName, String password) {
        this.accountId = accountId;
        this.domainId = domainId;
        username = userName;
        this.password = password;
        state = State.Add;
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        username = userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String toString() {
        return new StringBuilder("VpnUser[").append(id).append("-").append(username).append("-").append(accountId).append("]").toString();
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Class<?> getEntityType() {
        return VpnUser.class;
    }
}
