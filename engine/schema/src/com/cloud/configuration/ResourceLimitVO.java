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
@Table(name = "resource_limit")
public class ResourceLimitVO implements ResourceLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id = null;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ResourceCount.ResourceType type;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "max")
    private Long max;

    public ResourceLimitVO() {
    }

    public ResourceLimitVO(ResourceCount.ResourceType type, Long max, long ownerId, ResourceOwnerType ownerType) {
        this.type = type;
        this.max = max;

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

    public void setType(ResourceCount.ResourceType type) {
        this.type = type;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getMax() {
        return max;
    }

    @Override
    public void setMax(Long max) {
        this.max = max;
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
