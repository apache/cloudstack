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

import java.util.Date;

import com.cloud.user.Account;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.user.User;

@EntityReference(value = User.class)
public class UserResponse extends BaseResponse implements SetResourceIconResponse {
    @SerializedName("id")
    @Param(description = "the user ID")
    private String id;

    @SerializedName("username")
    @Param(description = "the user name")
    private String username;

    @SerializedName("firstname")
    @Param(description = "the user firstname")
    private String firstname;

    @SerializedName("lastname")
    @Param(description = "the user lastname")
    private String lastname;

    @SerializedName("email")
    @Param(description = "the user email address")
    private String email;

    @SerializedName("created")
    @Param(description = "the date and time the user account was created")
    private Date created;

    @SerializedName("state")
    @Param(description = "the user state")
    private String state;

    @SerializedName("account")
    @Param(description = "the account name of the user")
    private String accountName;

    @SerializedName("accounttype")
    @Param(description = "the account type of the user")
    private Integer accountType;

    @SerializedName("usersource")
    @Param(description = "the source type of the user in lowercase, such as native, ldap, saml2")
    private String userSource;

    @SerializedName(ApiConstants.ROLE_ID)
    @Param(description = "the ID of the role")
    private String roleId;

    @SerializedName(ApiConstants.ROLE_TYPE)
    @Param(description = "the type of the role")
    private String roleType;

    @SerializedName(ApiConstants.ROLE_NAME)
    @Param(description = "the name of the role")
    private String roleName;

    @SerializedName("domainid")
    @Param(description = "the domain ID of the user")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "the domain name of the user")
    private String domainName;

    @SerializedName("timezone")
    @Param(description = "the timezone user was created in")
    private String timezone;

    @SerializedName("apikey")
    @Param(description = "the api key of the user", isSensitive = true)
    private String apiKey;

    @Deprecated
    @SerializedName("secretkey")
    @Param(description = "the secret key of the user", isSensitive = true)
    private String secretKey;

    @SerializedName("accountid")
    @Param(description = "the account ID of the user")
    private String accountId;

    @SerializedName("iscallerchilddomain")
    @Param(description = "the boolean value representing if the updating target is in caller's child domain")
    private boolean isCallerChildDomain;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "true if user is default, false otherwise", since = "4.2.0")
    private Boolean isDefault;

    @SerializedName(ApiConstants.RESOURCE_ICON)
    @Param(description = "Base64 string representation of the resource icon", since = "4.16.0.0")
    ResourceIconResponse icon;

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Account.Type accountType) {
        this.accountType = accountType.ordinal();
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setRoleType(RoleType roleType) {
        if (roleType != null) {
            this.roleType = roleType.name();
        }
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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean getIsCallerSubdomain() {
        return this.isCallerChildDomain;
    }

    public void setIsCallerChildDomain(boolean isCallerChildDomain) {
        this.isCallerChildDomain = isCallerChildDomain;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getUserSource() {
        return userSource;
    }

    public void setUserSource(User.Source userSource) {
        this.userSource = userSource.toString().toLowerCase();
        if (this.userSource.equals(User.Source.UNKNOWN.toString().toLowerCase())) {
            this.userSource = User.Source.NATIVE.toString().toLowerCase();
        }
    }

    @Override
    public void setResourceIconResponse(ResourceIconResponse icon) {
        this.icon = icon;
    }
}
