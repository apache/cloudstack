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
import com.cloud.user.UserData;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = UserData.class)
public class UserDataResponse extends BaseResponseWithAnnotations implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the ssh keypair")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the userdata")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT_ID) @Param(description="the owner id of the userdata")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the owner of the userdata")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the userdata", since = "4.19.1")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the userdata", since = "4.19.1")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the userdata owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the userdata owner")
    private String domain;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the userdata owner belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.USER_DATA) @Param(description="base64 encoded userdata content")
    private String userData;

    @SerializedName(ApiConstants.PARAMS) @Param(description="list of parameters which contains the list of keys or string parameters that are needed to be passed for any variables declared in userdata")
    private String params;

    public UserDataResponse() {
    }

    public UserDataResponse(String id, String name, String userData, String params) {
        this.id = id;
        this.name = name;
        this.userData = userData;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDomainName() {
        return domain;
    }

    public void setDomainName(String domain) {
        this.domain = domain;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }
}
