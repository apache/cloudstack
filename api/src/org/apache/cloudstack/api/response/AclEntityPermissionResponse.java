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
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class AclEntityPermissionResponse extends BaseResponse {

    @SerializedName(ApiConstants.GROUP_ID)
    @Param(description = "the ID of the acl group")
    private String groupId;

    @SerializedName(ApiConstants.ENTITY_TYPE)
    @Param(description = "the entity type of this permission")
    private String entityType;

    @SerializedName(ApiConstants.ENTITY_ID)
    @Param(description = "the uuid of the entity involved in this permission")
    private String entityId;

    @SerializedName(ApiConstants.ACCESS_TYPE)
    @Param(description = "access type involved in this permission")
    private String accessType;



    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        result = prime * result + ((accessType == null) ? 0 : accessType.hashCode());
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
        AclEntityPermissionResponse other = (AclEntityPermissionResponse) obj;
        if (entityType == null) {
            if (other.entityType != null)
                return false;
        } else if (!entityType.equals(other.entityType)) {
            return false;
        } else if ((entityId == null && other.entityId != null) || !entityId.equals(other.entityId)) {
            return false;
        } else if ((accessType == null && other.accessType != null) || !accessType.equals(other.accessType)) {
            return false;
        }
        return true;
    }



}
