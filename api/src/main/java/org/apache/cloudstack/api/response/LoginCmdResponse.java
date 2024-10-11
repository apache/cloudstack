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
import org.apache.cloudstack.api.ApiConstants;

public class LoginCmdResponse extends AuthenticationCmdResponse {

    @SerializedName(value = ApiConstants.USERNAME)
    @Param(description = "Username")
    private String username;

    @SerializedName(value = ApiConstants.USER_ID)
    @Param(description = "User ID")
    private String userId;

    @SerializedName(value = ApiConstants.DOMAIN_ID)
    @Param(description = "Domain ID that the user belongs to")
    private String domainId;

    @SerializedName(value = ApiConstants.TIMEOUT)
    @Param(description = "the time period before the session has expired")
    private Integer timeout;

    @SerializedName(value = ApiConstants.ACCOUNT)
    @Param(description = "the account name the user belongs to")
    private String account;

    @SerializedName(value = ApiConstants.FIRSTNAME)
    @Param(description = "first name of the user")
    private String firstName;

    @SerializedName(value = ApiConstants.LASTNAME)
    @Param(description = "last name of the user")
    private String lastName;

    @SerializedName(value = ApiConstants.TYPE)
    @Param(description = "the account type (admin, domain-admin, read-only-admin, user)")
    private String type;

    @SerializedName(value = ApiConstants.TIMEZONE)
    @Param(description = "user time zone")
    private String timeZone;

    @SerializedName(value = ApiConstants.TIMEZONEOFFSET)
    @Param(description = "user time zoneoffset")
    private String timeZoneOffset;

    @SerializedName(value = ApiConstants.REGISTERED)
    @Param(description = "Is user registered")
    private String registered;

    @SerializedName(value = ApiConstants.SESSIONKEY)
    @Param(description = "Session key that can be passed in subsequent Query command calls", isSensitive = true)
    private String sessionKey;

    @SerializedName(value = ApiConstants.IS_2FA_ENABLED)
    @Param(description = "Is two factor authentication enabled", since = "4.18.0.0")
    private String is2FAenabled;

    @SerializedName(value = ApiConstants.IS_2FA_VERIFIED)
    @Param(description = "Is two factor authentication verified", since = "4.18.0.0")
    private String is2FAverified;

    @SerializedName(value = ApiConstants.PROVIDER_FOR_2FA)
    @Param(description = "Two factor authentication provider", since = "4.18.0.0")
    private String providerFor2FA;

    @SerializedName(value = ApiConstants.ISSUER_FOR_2FA)
    @Param(description = "Two factor authentication issuer", since = "4.18.0.0")
    private String issuerFor2FA;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZoneOffset() {
        return timeZoneOffset;
    }

    public void setTimeZoneOffset(String timeZoneOffset) { this.timeZoneOffset = timeZoneOffset; }

    public String getRegistered() {
        return registered;
    }

    public void setRegistered(String registered) {
        this.registered = registered;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String is2FAenabled() {
        return is2FAenabled;
    }

    public void set2FAenabled(String is2FAenabled) {
        this.is2FAenabled = is2FAenabled;
    }

    public String is2FAverfied() {
        return is2FAverified;
    }

    public void set2FAverfied(String is2FAverified) {
        this.is2FAverified = is2FAverified;
    }

    public String getProviderFor2FA() {
        return providerFor2FA;
    }

    public void setProviderFor2FA(String providerFor2FA) {
        this.providerFor2FA = providerFor2FA;
    }

    public String getIssuerFor2FA() {
        return issuerFor2FA;
    }

    public void setIssuerFor2FA(String issuerFor2FA) {
        this.issuerFor2FA = issuerFor2FA;
    }
}
