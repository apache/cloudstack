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

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.AclGroup;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = AclGroup.class)
public class AclGroupResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the acl group")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the acl group")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the acl group")
    private String description;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the acl group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the acl role")
    private String domainName;

    @SerializedName(ApiConstants.ACL_ACCOUNT_IDS)
    @Param(description = "account Ids assigned to this acl group ")
    private Set<String> accountIdList;

    @SerializedName(ApiConstants.ACL_ROLES)
    @Param(description = "acl roles granted to this acl group ")
    private Set<AclRoleResponse> roleList;

    public AclGroupResponse() {
        accountIdList = new LinkedHashSet<String>();
        roleList = new LinkedHashSet<AclRoleResponse>();
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

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setAccountIdList(Set<String> acctIdList) {
        accountIdList = acctIdList;
    }

    public void addAccountId(String acctId) {
        accountIdList.add(acctId);
    }

    public void setRoleList(Set<AclRoleResponse> roles) {
        roleList = roles;
    }

    public void addRole(AclRoleResponse role) {
        roleList.add(role);
    }

    public Set<AclRoleResponse> getRoleList() {
        return roleList;
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
        AclGroupResponse other = (AclGroupResponse)obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
