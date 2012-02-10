/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ProjectAccountResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "project id")
    private IdentityProxy projectId = new IdentityProxy("projects");

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "project name")
    private String projectName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the id of the account")
    private IdentityProxy id = new IdentityProxy("account");

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the name of the account")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.ROLE)
    @Param(description = "account role in the project (regular,owner)")
    private String role;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "id of the Domain the account belongs too")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "name of the Domain the account belongs too")
    private String domainName;

    @SerializedName(ApiConstants.USER)
    @Param(description = "the list of users associated with account", responseObject = UserResponse.class)
    private List<UserResponse> users;

    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
