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
package org.apache.cloudstack.acl.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.AclEntityType;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.iam.api.AclPolicyPermission;

import com.cloud.serializer.Param;

public class AclPermissionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ACL_ACTION)
    @Param(description = "action of this permission")
    private String action;

    @SerializedName(ApiConstants.ENTITY_TYPE)
    @Param(description = "the entity type of this permission")
    private AclEntityType entityType;

    @SerializedName(ApiConstants.ACL_SCOPE)
    @Param(description = "scope of this permission")
    private PermissionScope scope;

    @SerializedName(ApiConstants.ACL_SCOPE_ID)
    @Param(description = "scope id of this permission")
    private Long scopeId;

    @SerializedName(ApiConstants.ACL_ALLOW_DENY)
    @Param(description = "allow or deny of this permission")
    private AclPolicyPermission.Permission permission;

    public AclEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(AclEntityType entityType) {
        this.entityType = entityType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PermissionScope getScope() {
        return scope;
    }

    public void setScope(PermissionScope scope) {
        this.scope = scope;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public AclPolicyPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(AclPolicyPermission.Permission permission) {
        this.permission = permission;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((scopeId == null) ? 0 : scopeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AclPermissionResponse other = (AclPermissionResponse) obj;
        if ((entityType == null && other.entityType != null) || !entityType.equals(other.entityType)) {
            return false;
        } else if ((action == null && other.action != null) || !action.equals(other.action)) {
            return false;
        } else if ((scope == null && other.scope != null) || !scope.equals(other.scope)) {
            return false;
        } else if ((scopeId == null && other.scopeId != null) || !scopeId.equals(other.scopeId)) {
            return false;
        }
        return true;
    }



}
