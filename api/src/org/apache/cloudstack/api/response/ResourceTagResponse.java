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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ResourceTagResponse extends BaseResponse implements ControlledViewEntityResponse{
    @SerializedName(ApiConstants.KEY) @Param(description="tag key name")
    private String key;

    @SerializedName(ApiConstants.VALUE) @Param(description="tag value")
    private String value;

    @SerializedName(ApiConstants.RESOURCE_TYPE) @Param(description="resource type")
    private String resourceType;

    @SerializedName(ApiConstants.RESOURCE_ID) @Param(description="id of the resource")
    private String resourceId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the tag")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id the tag belongs to")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name where tag belongs to")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the tag")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the tag")
    private String domainName;

    @SerializedName(ApiConstants.CUSTOMER) @Param(description="customer associated with the tag")
    private String customer;

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setResourceId(String id) {
        this.resourceId = id;
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
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}
