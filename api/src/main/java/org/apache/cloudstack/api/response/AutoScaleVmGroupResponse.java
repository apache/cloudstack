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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = AutoScaleVmGroup.class)
public class AutoScaleVmGroupResponse extends BaseResponseWithAnnotations implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The autoscale Instance group ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the autoscale Instance group ")
    private String name;

    @SerializedName(ApiConstants.LBID)
    @Param(description = "The Load balancer rule ID")
    private String loadBalancerId;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_NAME)
    @Param(description = "The name of the guest Network the LB rule belongs to")
    private String networkName;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_ID)
    @Param(description = "The id of the guest Network the LB rule belongs to")
    private String networkId;

    @SerializedName(ApiConstants.LB_PROVIDER)
    @Param(description = "The LB provider of the guest Network the LB rule belongs to")
    private String lbProvider;

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "The public IP address ID")
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

    @SerializedName(ApiConstants.VMPROFILE_ID)
    @Param(description = "The autoscale profile that contains information about the Instances in the Instance group.")
    private String profileId;

    @SerializedName(ApiConstants.MIN_MEMBERS)
    @Param(description = "The minimum number of members in the Instance Group, the number of Instances in the Instance group will be equal to or more than this number.")
    private int minMembers;

    @SerializedName(ApiConstants.MAX_MEMBERS)
    @Param(description = "The maximum number of members in the Instance Group, The number of Instances in the Instance group will be equal to or less than this number.")
    private int maxMembers;

    @SerializedName(ApiConstants.AVAILABLE_VIRTUAL_MACHINE_COUNT)
    @Param(description = "The number of available Instances (in Running, Starting, Stopping or Migrating state) in the Instance Group", since = "4.18.0")
    private int availableVirtualMachineCount;

    @SerializedName(ApiConstants.INTERVAL)
    @Param(description = "The frequency at which the conditions have to be evaluated")
    private int interval;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The current state of the AutoScale Instance Group")
    private String state;

    @SerializedName(ApiConstants.SCALEUP_POLICIES)
    @Param(description = "List of scaleup autoscale policies")
    private List<AutoScalePolicyResponse> scaleUpPolicies;

    @SerializedName(ApiConstants.SCALEDOWN_POLICIES)
    @Param(description = "List of scaledown autoscale policies")
    private List<AutoScalePolicyResponse> scaleDownPolicies;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account owning the Instance group")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project id of the Instance group")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the Instance group")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The domain ID of the Instance group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain name of the Instance group")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the vm group belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "Is group for display to the regular User", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date when this Instance group was created")
    private Date created;

    public AutoScaleVmGroupResponse() {
        // Empty constructor
    }

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLoadBalancerId(String loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setLbProvider(String lbProvider) {
        this.lbProvider = lbProvider;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
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

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public void setAvailableVirtualMachineCount(int availableVirtualMachineCount) {
        this.availableVirtualMachineCount = availableVirtualMachineCount;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setScaleUpPolicies(List<AutoScalePolicyResponse> scaleUpPolicies) {
        this.scaleUpPolicies = scaleUpPolicies;
    }

    public void setScaleDownPolicies(List<AutoScalePolicyResponse> scaleDownPolicies) {
        this.scaleDownPolicies = scaleDownPolicies;
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

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public int getMinMembers() {
        return minMembers;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public int getAvailableVirtualMachineCount() {
        return availableVirtualMachineCount;
    }

    public int getInterval() {
        return interval;
    }

    public String getState() {
        return state;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getLbProvider() {
        return lbProvider;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }
}
