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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;

@Entity
@Table(name = ("acl_role_permission"))
public class AclRolePermissionVO implements AclRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "role_id")
    private long aclRoleId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "access_type")
    @Enumerated(value = EnumType.STRING)
    AccessType accessType;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    PermissionScope scope;

    @Column(name = "permission")
    private boolean allowed;

    public AclRolePermissionVO() {

    }

    public AclRolePermissionVO(long roleId, String entityType, AccessType atype) {
        aclRoleId = roleId;
        this.entityType = entityType;
        accessType = atype;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getAclRoleId() {
        return aclRoleId;
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }


    public void setAclRoleId(long aclRoleId) {
        this.aclRoleId = aclRoleId;
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
    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

}
