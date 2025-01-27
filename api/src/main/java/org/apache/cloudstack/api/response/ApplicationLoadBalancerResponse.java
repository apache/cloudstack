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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ApplicationLoadBalancerResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the Load Balancer ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the Load Balancer")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the Load Balancer")
    private String description;

    @SerializedName(ApiConstants.ALGORITHM)
    @Param(description = "the load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "Load Balancer network id")
    private String networkId;

    @SerializedName(ApiConstants.SOURCE_IP)
    @Param(description = "Load Balancer source ip")
    private String sourceIp;

    @SerializedName(ApiConstants.SOURCE_IP_NETWORK_ID)
    @Param(description = "Load Balancer source ip network id")
    private String sourceIpNetworkId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the Load Balancer")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the Load Balancer")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the Load Balancer")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the Load Balancer")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain of the Load Balancer")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the Load Balancer belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName("loadbalancerrule")
    @Param(description = "the list of rules associated with the Load Balancer", responseObject = ApplicationLoadBalancerRuleResponse.class)
    private List<ApplicationLoadBalancerRuleResponse> lbRules;

    @SerializedName("loadbalancerinstance")
    @Param(description = "the list of instances associated with the Load Balancer", responseObject = ApplicationLoadBalancerInstanceResponse.class)
    private List<ApplicationLoadBalancerInstanceResponse> lbInstances;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with the Load Balancer", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is rule for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

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

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public void setSourceIpNetworkId(String sourceIpNetworkId) {
        this.sourceIpNetworkId = sourceIpNetworkId;
    }

    public void setLbRules(List<ApplicationLoadBalancerRuleResponse> lbRules) {
        this.lbRules = lbRules;
    }

    public void setLbInstances(List<ApplicationLoadBalancerInstanceResponse> lbInstances) {
        this.lbInstances = lbInstances;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
