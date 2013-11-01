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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_group_view"))
public class AclGroupJoinVO extends BaseViewVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "role_id")
    private long roleId;

    @Column(name = "role_uuid")
    private String roleUuid;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private long entityId;

    @Column(name = "entity_uuid")
    private String entityUuid;

    @Column(name = "access_type")
    @Enumerated(value = EnumType.STRING)
    AccessType accessType;

    public AclGroupJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUuid() {
        return uuid;
    }

    public long getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }


    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }

    public long getRoleId() {
        return roleId;
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public String getRoleName() {
        return roleName;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getEntityType() {
        return entityType;
    }

    public long getEntityId() {
        return entityId;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public AccessType getAccessType() {
        return accessType;
    }

}
