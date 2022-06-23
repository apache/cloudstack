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

import com.cloud.network.Network;
import com.cloud.serializer.Param;

@EntityReference(value = Network.class)
@SuppressWarnings("unused")
public class NetworkPermissionsResponse extends BaseResponse {
    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the network ID")
    private String networkId;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain to which the network belongs")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain to which the network belongs")
    private String domainName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account the network is available for")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the ID of account the network is available for")
    private String accountId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project the network is available for")
    private String projectName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the ID of project the network is available for")
    private String projectId;


    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
