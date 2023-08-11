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
public class LoadBalancerResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The Load balancer rule ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the Load balancer")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the Load balancer")
    private String description;

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "The public IP address id")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "The public IP address")
    private String publicIp;

    @SerializedName(ApiConstants.PUBLIC_PORT)
    @Param(description = "The public port")
    private String publicPort;

    @SerializedName(ApiConstants.PRIVATE_PORT)
    @Param(description = "The private port")
    private String privatePort;

    @SerializedName(ApiConstants.ALGORITHM)
    @Param(description = "The Load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "The id of the guest Network the LB rule belongs to")
    private String networkId;

    @SerializedName(ApiConstants.CIDR_LIST)
    @Param(description = "The CIDR list to allow traffic, all other CIDRs will be blocked. Multiple entries must be separated by a single comma character (,).")
    private String cidrList;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account of the Load balancer rule")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project id of the Load balancer")
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

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the rule")
    private String state;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The id of the zone the rule belongs to")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The name of the zone the Load balancer rule belongs to", since = "4.11")
    private String zoneName;

    @SerializedName(ApiConstants.PROTOCOL)
    @Param(description = "The protocol of the Load Balancer rule")
    private String lbProtocol;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "The list of resource tags associated with Load balancer", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "Is rule for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setPublicPort(String publicPort) {
        this.publicPort = publicPort;
    }

    public void setPrivatePort(String privatePort) {
        this.privatePort = privatePort;
    }

    public void setCidrList(String cidrs) {
        this.cidrList = cidrs;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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

    public void setState(String state) {
        this.state = state;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
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

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setLbProtocol(String lbProtocol) {
        this.lbProtocol = lbProtocol;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
