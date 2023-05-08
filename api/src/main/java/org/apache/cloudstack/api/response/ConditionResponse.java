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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.as.Condition;
import com.cloud.serializer.Param;

@EntityReference(value = Condition.class)
@SuppressWarnings("unused")
public class ConditionResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName("id")
    @Param(description = "the id of the Condition")
    private String id;

    @SerializedName(value = ApiConstants.THRESHOLD)
    @Param(description = "Threshold Value for the counter.")
    private long threshold;

    @SerializedName(value = ApiConstants.RELATIONAL_OPERATOR)
    @Param(description = "Relational Operator to be used with threshold.")
    private String relationalOperator;

    @SerializedName("counterid")
    @Param(description = "the Id of the Counter.")
    private String counterId;

    @SerializedName("countername")
    @Param(description = "the Name of the Counter.")
    private String counterName;

    @SerializedName("counter")
    @Param(description = "Details of the Counter.")
    private CounterResponse counterResponse;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the Condition owner")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the owner.")
    private String domain;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of counter")
    private String zoneId;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the Condition.")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the Condition")
    private String projectName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the owner of the Condition.")
    private String accountName;

    // /////////////////////////////////////////////////
    // ///////////////// Setters ///////////////////////
    // ///////////////////////////////////////////////////

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setRelationalOperator(String relationalOperator) {
        this.relationalOperator = relationalOperator;
    }

    public void setCounterResponse(CounterResponse counterResponse) {
        this.counterResponse = counterResponse;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
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
}
