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

import com.cloud.configuration.Resource;
import com.cloud.configuration.ResourceLimit;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ResourceLimit.class)
@SuppressWarnings("unused")
public class ResourceLimitResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the resource limit")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the resource limit")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the resource limit")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the resource limit belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "resource type. Values include 0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11. See the resourceType parameter for more information on these values.")
    private String resourceType;

    @SerializedName(ApiConstants.RESOURCE_TYPE_NAME)
    @Param(description = "resource type name. Values include user_vm, public_ip, volume, snapshot, template, project, network, vpc, cpu, memory, primary_storage, secondary_storage.")
    private String resourceTypeName;

    @SerializedName("max")
    @Param(description = "the maximum number of the resource. A -1 means the resource currently has no limit.")
    private Long max;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the resource limit")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the resource limit")
    private String projectName;

    @SerializedName(ApiConstants.TAG)
    @Param(description = "The tag for the resource limit", since = "4.20.0")
    private String tag;

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
    public void setResourceType(Resource.ResourceType resourceType) {
        this.resourceType = Integer.valueOf(resourceType.getOrdinal()).toString();
        this.resourceTypeName = resourceType.getName();
    }

    public void setMax(Long max) {
        this.max = max;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
