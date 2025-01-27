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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.Date;
import java.util.UUID;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "user_data")
public class UserDataVO implements UserData {

    public UserDataVO() {
        uuid = UUID.randomUUID().toString();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id = null;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "name")
    private String name;

    @Column(name = "user_data", updatable = true, length = 1048576)
    @Basic(fetch = FetchType.LAZY)
    private String userData;

    @Column(name = "params", length = 4096)
    private String params;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Class<?> getEntityType() {
        return UserDataVO.class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUserData() {
        return userData;
    }

    @Override
    public String getParams() {
        return params;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getRemoved() {
        return removed;
    }
}
