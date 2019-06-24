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
package com.cloud.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "resource_count")
public class ResourceCountVO implements ResourceCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id = null;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "count")
    private long count;

    public ResourceCountVO() {
    }

    public ResourceCountVO(ResourceType type, long count, long ownerId, ResourceOwnerType ownerType) {
        this.type = type;
        this.count = count;

        if (ownerType == ResourceOwnerType.Account) {
            this.accountId = ownerId;
        } else if (ownerType == ResourceOwnerType.Domain) {
            this.domainId = ownerId;
        }
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public void setCount(long count) {
        this.count = count;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return new StringBuilder("REsourceCount[").append("-")
            .append(id)
            .append("-")
            .append(type)
            .append("-")
            .append(accountId)
            .append("-")
            .append(domainId)
            .append("]")
            .toString();
    }

    @Override
    public long getOwnerId() {
        if (accountId != null) {
            return accountId;
        }

        return domainId;
    }

    @Override
    public ResourceOwnerType getResourceOwnerType() {
        if (accountId != null) {
            return ResourceOwnerType.Account;
        } else {
            return ResourceOwnerType.Domain;
        }
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
