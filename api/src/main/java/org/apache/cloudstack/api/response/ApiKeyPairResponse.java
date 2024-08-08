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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

@EntityReference(value = ApiKeyPair.class)
public class ApiKeyPairResponse extends BaseResponseWithAnnotations {
    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the Keypair")
    private String name;

    @SerializedName(ApiConstants.API_KEY)
    @Param(description = "The API key of the registered user.", isSensitive = true)
    private String userApiKey;

    @SerializedName(ApiConstants.SECRET_KEY)
    @Param(description = "The secret key of the registered user.", isSensitive = true)
    private String userSecretKey;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "ID of the user that owns the keypair.")
    private String userId;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "User name of the keypair's owner.")
    private String userName;

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the API keypair.", isSensitive = true)
    private String uuid;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Keypair description.")
    private String description;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Keypair start date.")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Keypair expiration date.")
    private Date endDate;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Keypair creation timestamp.")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "Account type (admin, domain-admin, user).")
    private Integer accountType;

    @SerializedName(ApiConstants.ROLE_ID)
    @Param(description = "ID of the role.")
    private Long roleId;

    @SerializedName(ApiConstants.ROLE_TYPE)
    @Param(description = "Type of the role (Admin, ResourceAdmin, DomainAdmin, User).")
    private String roleType;

    @SerializedName(ApiConstants.ROLE_NAME)
    @Param(description = "Name of the role.")
    private String roleName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "ID of the domain which the account belongs to.")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "Name of the domain which the account belongs to.")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the domain which the account belongs to.")
    private String domainPath;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of the keypair.")
    private String state;

    public String getApiKey() {
        return userApiKey;
    }

    public void setApiKey(String apiKey) {
        this.userApiKey = apiKey;
    }

    public String getSecretKey() {
        return userSecretKey;
    }

    public void setSecretKey(String secretKey) {
        this.userSecretKey = secretKey;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
