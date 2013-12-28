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

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.AclPolicy;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = AclPolicy.class)
public class AclPolicyResponse extends BaseResponse implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the acl policy")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the acl policy")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the acl policy")
    private String description;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the acl policy")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the acl policy")
    private String domainName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account owning the policy")
    private String accountName;

    @SerializedName(ApiConstants.ACL_PERMISSIONS)
    @Param(description = "set of permissions for the acl policy")
    private Set<AclPermissionResponse> permissionList;

    public AclPolicyResponse() {
        permissionList = new LinkedHashSet<AclPermissionResponse>();
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

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Set<AclPermissionResponse> getPermissionList() {
        return permissionList;
    }

    public void setPermissionList(Set<AclPermissionResponse> perms) {
        permissionList = perms;
    }

    public void addPermission(AclPermissionResponse perm) {
        permissionList.add(perm);
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProjectName(String projectName) {
        // TODO Auto-generated method stub

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
        AclPolicyResponse other = (AclPolicyResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }



}
