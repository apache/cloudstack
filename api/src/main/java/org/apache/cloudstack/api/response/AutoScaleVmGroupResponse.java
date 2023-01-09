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
    @Param(description = "the autoscale vm group ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the autoscale vm group ")
    private String name;

    @SerializedName(ApiConstants.LBID)
    @Param(description = "the load balancer rule ID")
    private String loadBalancerId;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_NAME)
    @Param(description = "the name of the guest network the lb rule belongs to")
    private String networkName;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_ID)
    @Param(description = "the id of the guest network the lb rule belongs to")
    private String networkId;

    @SerializedName(ApiConstants.LB_PROVIDER)
    @Param(description = "the lb provider of the guest network the lb rule belongs to")
    private String lbProvider;

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "the public ip address id")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "the public ip address")
    private String publicIp;

    @SerializedName(ApiConstants.PUBLIC_PORT)
    @Param(description = "the public port")
    private String publicPort;

    @SerializedName(ApiConstants.PRIVATE_PORT)
    @Param(description = "the private port")
    private String privatePort;

    @SerializedName(ApiConstants.VMPROFILE_ID)
    @Param(description = "the autoscale profile that contains information about the vms in the vm group.")
    private String profileId;

    @SerializedName(ApiConstants.MIN_MEMBERS)
    @Param(description = "the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number.")
    private int minMembers;

    @SerializedName(ApiConstants.MAX_MEMBERS)
    @Param(description = "the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number.")
    private int maxMembers;

    @SerializedName(ApiConstants.AVAILABLE_VIRTUAL_MACHINE_COUNT)
    @Param(description = "the number of available virtual machines (in Running, Starting, Stopping or Migrating state) in the vmgroup", since = "4.18.0")
    private int availableVirtualMachineCount;

    @SerializedName(ApiConstants.INTERVAL)
    @Param(description = "the frequency at which the conditions have to be evaluated")
    private int interval;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the current state of the AutoScale Vm Group")
    private String state;

    @SerializedName(ApiConstants.SCALEUP_POLICIES)
    @Param(description = "list of scaleup autoscale policies")
    private List<AutoScalePolicyResponse> scaleUpPolicies;

    @SerializedName(ApiConstants.SCALEDOWN_POLICIES)
    @Param(description = "list of scaledown autoscale policies")
    private List<AutoScalePolicyResponse> scaleDownPolicies;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account owning the vm group")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the vm group")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vm group")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the vm group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the vm group")
    private String domainName;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is group for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date when this vm group was created")
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
