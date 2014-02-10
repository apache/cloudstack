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

import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.serializer.Param;

@EntityReference(value = AutoScaleVmGroup.class)
public class AutoScaleVmGroupResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the autoscale vm group ID")
    private String id;

    @SerializedName(ApiConstants.LBID)
    @Param(description = "the load balancer rule ID")
    private String loadBalancerId;

    @SerializedName(ApiConstants.VMPROFILE_ID)
    @Param(description = "the autoscale profile that contains information about the vms in the vm group.")
    private String profileId;

    @SerializedName(ApiConstants.MIN_MEMBERS)
    @Param(description = "the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number.")
    private int minMembers;

    @SerializedName(ApiConstants.MAX_MEMBERS)
    @Param(description = "the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number.")
    private int maxMembers;

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
    @Param(description = "the account owning the instance group")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id vm profile")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vm profile")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the vm profile")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the vm profile")
    private String domainName;

    public AutoScaleVmGroupResponse() {

    }

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLoadBalancerId(String loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
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
}
