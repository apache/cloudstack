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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.OvsProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = OvsProvider.class)
@SuppressWarnings("unused")
public class OvsProviderResponse extends BaseResponse implements
        ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the ovs")
    private String id;
    @SerializedName(ApiConstants.NSP_ID)
    @Param(description = "the physical network service provider id of the provider")
    private String nspId;
    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "Enabled/Disabled the service provider")
    private Boolean enabled;
    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the provider")
    private String accountName;
    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the ipaddress")
    private String projectId;
    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the address")
    private String projectName;
    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID associated with the provider")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the provider")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the provider belongs", since = "4.19.2.0")
    private String domainPath;

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setId(String id) {
        this.id = id;
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
    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setNspId(String nspId) {
        this.nspId = nspId;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
