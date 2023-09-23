// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

/**
 * Login Response object
 *
 */
public class LoginResponse extends BaseResponse {

    @SerializedName("timeout")
    @Param(description = "session timeout period")
    private String timeout;

    @SerializedName("sessionkey")
    @Param(description = "login session key")
    private String sessionkey;

    @SerializedName("username")
    @Param(description = "login username")
    private String username;

    @SerializedName("userid")
    @Param(description = "login user internal uuid")
    private String userid;

    @SerializedName("firstname")
    @Param(description = "login user firstname")
    private String firstname;

    @SerializedName("lastname")
    @Param(description = "login user lastname")
    private String lastname;

    @SerializedName("account")
    @Param(description = "login user account type")
    private String account;

    @SerializedName("domainid")
    @Param(description = "login user domain id")
    private String domainid;

    @SerializedName("type")
    @Param(description = "login user type")
    private int type;

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getSessionkey() {
        return sessionkey;
    }

    public void setSessionkey(String sessionkey) {
        this.sessionkey = sessionkey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getDomainid() {
        return domainid;
    }

    public void setDomainid(String domainid) {
        this.domainid = domainid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
