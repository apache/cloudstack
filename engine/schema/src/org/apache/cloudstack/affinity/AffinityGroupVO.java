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
package org.apache.cloudstack.affinity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.acl.ControlledEntity;

@Entity
@Table(name = ("affinity_group"))
public class AffinityGroupVO implements AffinityGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "acl_type")
    @Enumerated(value = EnumType.STRING)
    ControlledEntity.ACLType aclType;

    public AffinityGroupVO() {
        uuid = UUID.randomUUID().toString();
    }

    public AffinityGroupVO(String name, String type, String description, long domainId, long accountId, ACLType aclType) {
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        uuid = UUID.randomUUID().toString();
        this.type = type;
        this.aclType = aclType;
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
    public String getDescription() {
        return description;
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
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public ControlledEntity.ACLType getAclType() {
        return aclType;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("AffinityGroup[");
        buf.append(uuid).append("]");
        return buf.toString();
    }

    @Override
    public Class<?> getEntityType() {
        return AffinityGroup.class;
    }

}
