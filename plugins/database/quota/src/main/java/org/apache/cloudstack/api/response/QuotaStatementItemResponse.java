//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import java.math.BigDecimal;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaStatementItemResponse extends BaseResponse {

    @SerializedName("type")
    @Param(description = "usage type")
    private int usageType;

    @SerializedName("name")
    @Param(description = "usage type name")
    private String usageName;

    @SerializedName("unit")
    @Param(description = "usage unit")
    private String usageUnit;

    @SerializedName("quota")
    @Param(description = "quota consumed")
    private BigDecimal quotaUsed;

    @SerializedName(ApiConstants.RESOURCES)
    @Param(description = "Item's resources.")
    private List<QuotaStatementItemResourceResponse> resources;

    public QuotaStatementItemResponse(final int usageType) {
        this.usageType = usageType;
    }

    public String getUsageName() {
        return usageName;
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public int getUsageType() {
        return usageType;
    }

    public void setUsageType(int usageType) {
        this.usageType = usageType;
    }

    public String getUsageUnit() {
        return usageUnit;
    }

    public void setUsageUnit(String usageUnit) {
        this.usageUnit = usageUnit;
    }

    public BigDecimal getQuotaUsed() {
        return quotaUsed;
    }

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    public List<QuotaStatementItemResourceResponse> getResources() {
        return resources;
    }

    public void setResources(List<QuotaStatementItemResourceResponse> resources) {
        this.resources = resources;
    }

}
