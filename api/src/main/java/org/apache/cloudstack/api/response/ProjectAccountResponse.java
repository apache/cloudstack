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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.projects.ProjectAccount;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ProjectAccount.class)
@SuppressWarnings("unused")
public class ProjectAccountResponse extends BaseResponse implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "project id")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "project name")
    private String projectName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the id of the account")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the name of the account")
    private String accountName;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "Name of the user")
    private String username;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "Id of the user")
    private String userId;

    @SerializedName(ApiConstants.PROJECT_ROLE_ID)
    @Param(description = "Id of the project role associated with the account/user")
    private String projectRoleId;

    @SerializedName(ApiConstants.ROLE)
    @Param(description = "account role in the project (regular,owner)")
    private String role;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "id of the Domain the account belongs too")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "name of the Domain the account belongs too")
    private String domainName;

    @SerializedName(ApiConstants.USER)
    @Param(description = "the list of users associated with account", responseObject = UserResponse.class)
    private List<UserResponse> users;

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setAccountId(String id) {
        this.accountId = id;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setUserId(String userId) { this.userId = userId; }

    public void setProjectRoleId(String projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
