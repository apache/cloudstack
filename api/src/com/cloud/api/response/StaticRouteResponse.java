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
package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class StaticRouteResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the ID of static route")
    private IdentityProxy id = new IdentityProxy("static_routes");
    
    @SerializedName(ApiConstants.STATE) @Param(description="the state of the static route")
    private String state;

    @SerializedName(ApiConstants.VPC_ID) @Param(description="VPC the static route belongs to")
    private IdentityProxy vpcId = new IdentityProxy("vpc");
    
    @SerializedName(ApiConstants.GATEWAY_ID) @Param(description="VPC gateway the route is created for")
    private IdentityProxy gatewayId = new IdentityProxy("vpc_gateways");
    
    @SerializedName(ApiConstants.CIDR) @Param(description="static route CIDR")
    private String cidr;
    
    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the static route")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the static route")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the static route")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the static route")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the static route")
    private String domainName;
    
    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with static route",
            responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId.setValue(vpcId);
    }

    public void setGatewayId(Long gatewayId) {
        this.gatewayId.setValue(gatewayId);
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
    
    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }
    
    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}
