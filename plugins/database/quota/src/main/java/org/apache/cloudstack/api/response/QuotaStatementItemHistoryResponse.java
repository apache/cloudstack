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
import java.util.Date;

public class QuotaStatementItemHistoryResponse extends BaseResponse {

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Start date of the item.")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "End date of the item.")
    private Date endDate;

    @SerializedName(ApiConstants.QUOTA_CONSUMED)
    @Param(description = "Amount of quota consumed.")
    private BigDecimal quotaConsumed = BigDecimal.ZERO;

    public QuotaStatementItemHistoryResponse() {
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getQuotaConsumed() {
        return quotaConsumed;
    }

    public void setQuotaConsumed(BigDecimal quotaConsumed) {
        this.quotaConsumed = quotaConsumed;
    }

}
