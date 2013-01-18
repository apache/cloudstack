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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.vpc.Vpc;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=Vpc.class)
@SuppressWarnings("unused")
public class VpcResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName("id") @Param(description="the id of the VPC")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the VPC")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="an alternate display text of the VPC.")
    private String displayText;

    @SerializedName(ApiConstants.STATE) @Param(description="state of the VPC. Can be Inactive/Enabled")
    private String state;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="zone id of the vpc")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the name of the zone the VPC belongs to")
    private String zoneName;

    @SerializedName(ApiConstants.SERVICE) @Param(description="the list of supported services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;

    @SerializedName(ApiConstants.CIDR) @Param(description="the cidr the VPC")
    private String cidr;

    @SerializedName(ApiConstants.VPC_OFF_ID) @Param(description="vpc offering id the VPC is created from")
    private String vpcOfferingId;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date this VPC was created")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the owner of the VPC")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the VPC")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the VPC")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the VPC owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the owner")
    private String domain;

    @SerializedName(ApiConstants.NETWORK) @Param(description="the list of networks belongign to the VPC", responseObject = NetworkResponse.class)
    private List<NetworkResponse> networks;

    @SerializedName(ApiConstants.RESTART_REQUIRED) @Param(description="true VPC requires restart")
    private Boolean restartRequired;

    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain of the VPC")
    private String networkDomain;

    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with the project", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public void setState(String state) {
        this.state = state;
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
        this.domain = domainName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public void setVpcOfferingId(String vpcOfferingId) {
        this.vpcOfferingId = vpcOfferingId;
    }

    public List<NetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(List<NetworkResponse> networks) {
        this.networks = networks;
    }

    public void setRestartRequired(Boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}
