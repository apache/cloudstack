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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.AclRole;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = AclRole.class)
public class AclRoleResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the acl role")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the acl role")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the acl role")
    private String description;

    @SerializedName(ApiConstants.ACL_PARENT_ROLE_ID)
    @Param(description = "parent role id that this acl role is inherited from ")
    private String parentRoleId;

    @SerializedName(ApiConstants.ACL_PARENT_ROLE_NAME)
    @Param(description = "parent role name that this acl role is inherited from ")
    private String parentRoleName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the acl role")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the acl role")
    private String domainName;

    @SerializedName(ApiConstants.ACL_APIS)
    @Param(description = "allowed apis for the acl role ")
    private List<String> apiList;

    public AclRoleResponse() {
        apiList = new ArrayList<String>();
    }

    @Override
    public String getObjectId() {
        return getId();
    }


    public String getId() {
        return id;
     }

    public void setId(String id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParentRoleId(String parentId) {
        parentRoleId = parentId;
    }

    public void setParentRoleName(String parentRoleName) {
        this.parentRoleName = parentRoleName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<String> getApiList() {
        return apiList;
    }

    public void setApiList(List<String> apiList) {
        this.apiList = apiList;
    }

    public void addApi(String api) {
        apiList.add(api);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        AclRoleResponse other = (AclRoleResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }



}
