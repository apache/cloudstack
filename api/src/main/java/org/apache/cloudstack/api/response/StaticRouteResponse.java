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

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.vpc.StaticRoute;
import com.cloud.serializer.Param;

@EntityReference(value = StaticRoute.class)
@SuppressWarnings("unused")
public class StaticRouteResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of static route")
    private String id;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the static route")
    private String state;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC the static route belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.GATEWAY_ID)
    @Param(description = "VPC gateway the route is created for")
    private String gatewayId;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "static route CIDR")
    private String cidr;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the static route")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the static route")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the static route")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the static route")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the static route")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "the domain path associated with the static route", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with static route", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
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

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}
