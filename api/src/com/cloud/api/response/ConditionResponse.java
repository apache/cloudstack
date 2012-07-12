//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ConditionResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName("id")
    @Param(description = "the id of the Condition")
    private final IdentityProxy id = new IdentityProxy("conditions");

    @SerializedName(value = ApiConstants.THRESHOLD)
    @Param(description = "Threshold Value for the counter.")
    private long threshold;

    @SerializedName(value = ApiConstants.RELATIONAL_OPERATOR)
    @Param(description = "Relational Operator to be used with threshold.")
    private String relationalOperator;

    @SerializedName(value = ApiConstants.COUNTER_ID)
    @Param(description = "Details of the Counter.")
    private CounterResponse counter;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the Condition owner")
    private final IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the owner.")
    private String domain;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of counter")
    private final IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the Condition.")
    private final IdentityProxy projectId = new IdentityProxy("projects");

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the Condition")
    private String projectName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the owner of the Condition.")
    private String accountName;

    // /////////////////////////////////////////////////
    // ///////////////// Setters ///////////////////////
    // ///////////////////////////////////////////////////

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setRelationalOperator(String relationalOperator) {
        this.relationalOperator = relationalOperator;
    }

    public void setCounter(CounterResponse counter) {
        this.counter = counter;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    @Override
    public void setDomainName(String domainName) {
        this.domain = domainName;
    }
}