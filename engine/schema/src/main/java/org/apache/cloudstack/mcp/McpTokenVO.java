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
package org.apache.cloudstack.mcp;

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

@Entity
@Table(name = "mcp_token")
public class McpTokenVO implements McpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Column(name = "domain_id", nullable = false)
    private long domainId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "end_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(name = "created", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public McpTokenVO() {
    }

    public McpTokenVO(String name, String description, long userId, long accountId, long domainId, String tokenHash, Date endDate) {
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.tokenHash = tokenHash;
        this.endDate = endDate;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Class<?> getEntityType() {
        return McpToken.class;
    }
}
