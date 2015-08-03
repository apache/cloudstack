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

public class SamlUserAccountResponse extends AuthenticationCmdResponse {
    @SerializedName("userId")
    @Param(description = "The User Id")
    private String userId;

    @SerializedName("domainId")
    @Param(description = "The Domain Id")
    private String domainId;

    @SerializedName("userName")
    @Param(description = "The User Name")
    private String userName;

    @SerializedName("accountName")
    @Param(description = "The Account Name")
    private String accountName;

    @SerializedName("domainName")
    @Param(description = "The Domain Name")
    private String domainName;

    @SerializedName("idpId")
    @Param(description = "The IDP ID")
    private String idpId;

    public SamlUserAccountResponse() {
        super();
        setObjectName("samluseraccount");
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getIdpId() {
        return idpId;
    }

    public void setIdpId(String idpId) {
        this.idpId = idpId;
    }
}
