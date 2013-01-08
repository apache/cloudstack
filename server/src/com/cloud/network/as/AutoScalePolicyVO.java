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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "autoscale_policies")
@Inheritance(strategy = InheritanceType.JOINED)
public class AutoScalePolicyVO implements AutoScalePolicy, InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "duration")
    private int duration;

    @Column(name = "quiet_time", updatable = true, nullable = false)
    private int quietTime;

    @Column(name = "action", updatable = false, nullable = false)
    private String action;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    public AutoScalePolicyVO() {
    }

    public AutoScalePolicyVO(long domainId, long accountId, int duration, int quietTime, String action) {
        this.uuid = UUID.randomUUID().toString();
        this.domainId = domainId;
        this.accountId = accountId;
        this.duration = duration;
        this.quietTime = quietTime;
        this.action = action;
    }

    @Override
    public String toString() {
        return new StringBuilder("AutoScalePolicy[").append("id-").append(id).append("]").toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getQuietTime() {
        return quietTime;
    }

    @Override
    public String getAction() {
        return action;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setQuietTime(Integer quietTime) {
        this.quietTime = quietTime;
    }
}
