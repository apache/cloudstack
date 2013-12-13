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

import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_group_view"))
public class AclGroupJoinVO extends BaseViewVO implements ControlledViewEntity {
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

    @Column(name = "policy_id")
    private long policyId;

    @Column(name = "policy_uuid")
    private String policyUuid;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_type")
    private short accountType;

    @Column(name = "member_account_id")
    private long memberAccountId;

    @Column(name = "member_account_uuid")
    private String memberAccountUuid;

    @Column(name = "member_account_name")
    private String memberAccountName;

    @Column(name = "permission_action")
    private String permissionAction;

    @Column(name = "permission_entity_type")
    private String permissionEntityType;

    @Column(name = "permission_scope_id")
    private long permissionScopeId;

    @Column(name = "permission_scope_type")
    @Enumerated(value = EnumType.STRING)
    PermissionScope permissionScope;

    @Column(name = "permission_access_type")
    @Enumerated(value = EnumType.STRING)
    AccessType permissionAccessType;

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

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }


    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }

    public long getPolicyId() {
        return policyId;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public String getPolicyName() {
        return policyName;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public short getAccountType() {
        return accountType;
    }

    @Override
    public String getProjectUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProjectName() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getMemberAccountId() {
        return memberAccountId;
    }

    public String getMemberAccountUuid() {
        return memberAccountUuid;
    }

    public String getMemberAccountName() {
        return memberAccountName;
    }

    public String getPermissionAction() {
        return permissionAction;
    }

    public String getPermissionEntityType() {
        return permissionEntityType;
    }

    public long getPermissionScopeId() {
        return permissionScopeId;
    }

    public PermissionScope getPermissionScope() {
        return permissionScope;
    }

    public AccessType getPermissionAccessType() {
        return permissionAccessType;
    }

    @Override
    public AclEntityType getEntityType() {
        return AclEntityType.AclGroup;
    }

}
