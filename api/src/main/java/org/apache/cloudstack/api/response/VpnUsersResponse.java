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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.VpnUser;
import com.cloud.serializer.Param;

@EntityReference(value = VpnUser.class)
@SuppressWarnings("unused")
public class VpnUsersResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The VPN user ID")
    private String id;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "The username of the VPN user")
    private String userName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account of the remote access VPN")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the Account of the remote access VPN")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain name of the Account of the remote access VPN")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the domain to which the remote access VPN belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the VPN")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the VPN")
    private String projectName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the VPN User, can be 'Add', 'Revoke' or 'Active'.")
    private String state;

    public void setId(String id) {
        this.id = id;
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String name) {
        this.domainName = name;
    }

    @Override
    public void setDomainPath(String path) {
        this.domainPath = path;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
