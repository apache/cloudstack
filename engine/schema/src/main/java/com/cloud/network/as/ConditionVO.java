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

package com.cloud.network.as;

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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "conditions")
public class ConditionVO implements Condition, Identity, InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "counter_id")
    private long counterId;

    @Column(name = "threshold")
    private long threshold;

    @Column(name = "relational_operator")
    @Enumerated(value = EnumType.STRING)
    private Operator relationalOperator;

    @Column(name = "domain_id")
    protected long domainId;

    @Column(name = "account_id")
    protected long accountId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    public ConditionVO() {
    }

    public ConditionVO(long counterId, long threshold, long accountId, long domainId, Operator relationalOperator) {
        this.counterId = counterId;
        this.threshold = threshold;
        this.relationalOperator = relationalOperator;
        this.accountId = accountId;
        this.domainId = domainId;
        uuid = UUID.randomUUID().toString();
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return new StringBuilder("Condition[").append("id-").append(id).append("]").toString();
    }

    @Override
    public long getCounterId() {
        return counterId;
    }

    @Override
    public long getThreshold() {
        return threshold;
    }

    @Override
    public Operator getRelationalOperator() {
        return relationalOperator;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Class<?> getEntityType() {
        return Condition.class;
    }

    @Override
    public String getName() {
        return null;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setRelationalOperator(Operator relationalOperator) {
        this.relationalOperator = relationalOperator;
    }
}
