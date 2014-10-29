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
package com.cloud.vm;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "instance_group")
@SecondaryTable(name = "account", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "account_id", referencedColumnName = "id")})
public class InstanceGroupVO implements InstanceGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    String name;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id", table = "account", insertable = false, updatable = false)
    private long domainId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "type", table = "account", insertable = false, updatable = false)
    private short accountType;

    public InstanceGroupVO(String name, long accountId) {
        this.name = name;
        this.accountId = accountId;
        uuid = UUID.randomUUID().toString();
    }

    protected InstanceGroupVO() {
        super();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Short getAccountType() {
        return accountType;
    }

    @Override
    public Class<?> getEntityType() {
        return InstanceGroup.class;
    }
}
