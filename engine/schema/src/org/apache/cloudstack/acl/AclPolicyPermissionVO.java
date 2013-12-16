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
package org.apache.cloudstack.acl;

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
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_policy_permission"))
public class AclPolicyPermissionVO implements AclPolicyPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "policy_id")
    private long aclPolicyId;

    @Column(name = "action")
    private String action;

    @Column(name = "resource_type")
    private String entityType;

    @Column(name = "access_type")
    @Enumerated(value = EnumType.STRING)
    private AccessType accessType;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private PermissionScope scope;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "permission")
    @Enumerated(value = EnumType.STRING)
    private Permission permission;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public AclPolicyPermissionVO() {

    }

    public AclPolicyPermissionVO(long aclPolicyId, String action, String entityType, AccessType accessType,
            PermissionScope scope,
            Long scopeId, Permission permission) {
        this.aclPolicyId = aclPolicyId;
        this.action = action;
        this.entityType = entityType;
        this.accessType = accessType;
        this.scope = scope;
        this.scopeId = scopeId;
        this.permission = permission;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getAclPolicyId() {
        return aclPolicyId;
    }


    public void setAclPolicyId(long aclPolicyId) {
        this.aclPolicyId = aclPolicyId;
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }


    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    @Override
    public PermissionScope getScope() {
        return scope;
    }

    public void setScope(PermissionScope scope) {
        this.scope = scope;
    }


    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Long getScopeId() {
        // handle special -1 scopeId, current caller domain, account
        if ( scopeId < 0 ){
            Account caller = CallContext.current().getCallingAccount();
            if ( scope == PermissionScope.DOMAIN){
                return caller.getDomainId();
            } else if (scope == PermissionScope.ACCOUNT) {
                return caller.getAccountId();
            }
        }
        return scopeId;
    }

    @Override
    public Permission getPermission() {
        return permission;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
