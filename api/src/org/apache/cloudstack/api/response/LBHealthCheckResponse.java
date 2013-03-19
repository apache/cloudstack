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

import com.cloud.network.rules.HealthCheckPolicy;
import org.apache.cloudstack.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.List;
import java.util.UUID;

@EntityReference(value=HealthCheckPolicy.class)
public class LBHealthCheckResponse extends BaseResponse {
@SerializedName("lbruleid")
    @Param(description = "the LB rule ID")
    private String lbRuleId;


    @SerializedName("account")
    @Param(description = "the account of the HealthCheck policy")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the HealthCheck policy")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "the domain of the HealthCheck policy")
    private String domainName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the id of the zone the HealthCheck policy belongs to")
    private String zoneId;

    @SerializedName("healthcheckpolicy")
    @Param(description = "the list of healthcheckpolicies", responseObject = LBHealthCheckPolicyResponse.class)
    private List<LBHealthCheckPolicyResponse> healthCheckPolicies;

    public void setlbRuleId(String lbRuleId) {
        this.lbRuleId = lbRuleId;
    }

    public void setRules(List<LBHealthCheckPolicyResponse> policies) {
        this.healthCheckPolicies = policies;
    }

    public List<LBHealthCheckPolicyResponse> getHealthCheckPolicies() {
        return healthCheckPolicies;
    }

    public void setHealthCheckPolicies(List<LBHealthCheckPolicyResponse> healthCheckPolicies) {
        this.healthCheckPolicies = healthCheckPolicies;
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

    public LBHealthCheckResponse() {
    }

    public LBHealthCheckResponse(HealthCheckPolicy healthcheckpolicy) {
        setObjectName("healthcheckpolicy");
    }
}
