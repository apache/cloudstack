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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.math.BigDecimal;
import java.util.List;

public class QuotaResourceStatementResponse extends BaseResponse {

    @SerializedName(ApiConstants.USAGE_NAME)
    @Param(description = "Name of the usage type.")
    private String usageName;

    @SerializedName(ApiConstants.UNIT)
    @Param(description = "Unit of the usage type.")
    private String unit;

    @SerializedName(ApiConstants.ITEMS)
    @Param(description = "List of Quota tariff usages.", responseObject = QuotaResourceStatementItemResponse.class)
    private List<QuotaResourceStatementItemResponse> items;

    @SerializedName(ApiConstants.TOTAL_QUOTA)
    @Param(description = "Total amount of quota used.")
    private BigDecimal totalQuotaUsed;

    public QuotaResourceStatementResponse() {
        super("quotaresourcestatement");
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setQuotaUsageDetails(List<QuotaResourceStatementItemResponse> items) {
        this.items = items;
    }

    public void setTotalQuotaUsed(BigDecimal totalQuotaUsed) {
        this.totalQuotaUsed = totalQuotaUsed;
    }

}
