// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.dao;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * The ownership row for one deployment (SPEC.md section 9).
 *
 * Deliberately minimal: uuid, owner, zone and the engine's identifier.
 * The engine stays the source of truth for runtime state — replicas,
 * health, rollouts — and duplicating it here would create two answers to
 * the same question. What this buys is everything CloudStack does
 * natively with an owned row: ownership checks, list filtering by
 * account and domain, and cleanup when either is deleted.
 */
@Entity
@Table(name = "ai_deployment")
public class AiDeploymentVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "engine_id")
    private String engineId;

    @Column(name = "state")
    private String state;

    @Column(name = "created")
    private Date created;

    @Column(name = "removed")
    private Date removed;

    protected AiDeploymentVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public AiDeploymentVO(
            String name, long accountId, long domainId, long zoneId, String engineId) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.accountId = accountId;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.engineId = engineId;
        this.state = "Allocated";
        this.created = new Date();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
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

    public String getEngineId() {
        return engineId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return "AiDeployment{uuid=" + uuid + ", name=" + name
                + ", account=" + accountId + ", zone=" + zoneId + "}";
    }
}
