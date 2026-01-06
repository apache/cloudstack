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

import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.serializer.Param;

@EntityReference(value = GlobalLoadBalancerRule.class)
public class GlobalLoadBalancerResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "Global Load balancer rule ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the global Load balancer rule")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the global Load balancer rule")
    private String description;

    @SerializedName(ApiConstants.GSLB_SERVICE_DOMAIN_NAME)
    @Param(description = "DNS domain name given for the global Load balancer")
    private String gslbDomainName;

    @SerializedName(ApiConstants.GSLB_LB_METHOD)
    @Param(description = "Load balancing method used for the global Load balancer")
    private String algorithm;

    @SerializedName(ApiConstants.GSLB_STICKY_SESSION_METHOD)
    @Param(description = "Session persistence method used for the global Load balancer")
    private String stickyMethod;

    @SerializedName(ApiConstants.GSLB_SERVICE_TYPE)
    @Param(description = "GSLB service type")
    private String serviceType;

    @SerializedName(ApiConstants.REGION_ID)
    @Param(description = "Region ID in which global Load balancer is created")
    private Integer regionId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account of the Load balancer rule")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the Load balancer")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the Load balancer")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the Load balancer rule")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain of the Load balancer rule")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the load balancer rule belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.LOAD_BALANCER_RULE)
    @Param(description = "List of Load balancer rules that are part of GSLB rule", responseObject = LoadBalancerResponse.class)
    private List<LoadBalancerResponse> siteLoadBalancers;

    public void setRegionIdId(Integer regionId) {
        this.regionId = regionId;
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

    public void setStickyMethod(String stickyMethod) {
        this.stickyMethod = stickyMethod;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceDomainName(String domainName) {
        this.gslbDomainName = domainName;
    }

    @Override
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

    public void setSiteLoadBalancers(List<LoadBalancerResponse> siteLoadBalancers) {
        this.siteLoadBalancers = siteLoadBalancers;
    }
}
