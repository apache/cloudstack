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
    @Param(description = "Project ID")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "Project name")
    private String projectName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "The ID of the Account")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The name of the Account")
    private String accountName;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "Name of the user")
    private String username;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "Account type (admin, domain-admin, user)")
    private Integer accountType;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "ID of the user")
    private String userId;

    @SerializedName(ApiConstants.PROJECT_ROLE_ID)
    @Param(description = "ID of the project role associated with the Account/User")
    private String projectRoleId;

    @SerializedName(ApiConstants.ROLE)
    @Param(description = "Account role in the project (regular, owner)")
    private String role;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "ID of the Domain the Account belongs too")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "Name of the Domain the Account belongs too")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the Domain the Account belongs to", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.USER)
    @Param(description = "The list of users associated with Account", responseObject = UserResponse.class)
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

    public void setAccountType(Integer accountType) {
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

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
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
