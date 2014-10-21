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

import com.cloud.network.rules.StickinessPolicy;
import com.cloud.serializer.Param;

@EntityReference(value = StickinessPolicy.class)
public class LBStickinessResponse extends BaseResponse {
    @SerializedName("lbruleid")
    @Param(description = "the LB rule ID")
    private String lbRuleId;

    @SerializedName("name")
    @Param(description = "the name of the Stickiness policy")
    private String name;

    @SerializedName("description")
    @Param(description = "the description of the Stickiness policy")
    private String description;;

    @SerializedName("account")
    @Param(description = "the account of the Stickiness policy")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the Stickiness policy")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "the domain of the Stickiness policy")
    private String domainName;

    @SerializedName("state")
    @Param(description = "the state of the policy")
    private String state;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the id of the zone the Stickiness policy belongs to")
    private String zoneId;

    @SerializedName("stickinesspolicy")
    @Param(description = "the list of stickinesspolicies", responseObject = LBStickinessPolicyResponse.class)
    private List<LBStickinessPolicyResponse> stickinessPolicies;

    public void setlbRuleId(String lbRuleId) {
        this.lbRuleId = lbRuleId;
    }

    public void setRules(List<LBStickinessPolicyResponse> policies) {
        this.stickinessPolicies = policies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LBStickinessPolicyResponse> getStickinessPolicies() {
        return stickinessPolicies;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LBStickinessResponse() {

    }

}
