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
import java.util.List;

public class QuotaStatementResponse  extends BaseResponse {

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "ID of the Account.")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "Name of the Account.")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "ID of the Domain.")
    private String domainId;

    @SerializedName(ApiConstants.QUOTA_USAGE)
    @Param(description = "List of Quota usage under various types.", responseObject = QuotaStatementItemResponse.class)
    private List<QuotaStatementItemResponse> lineItem;

    @SerializedName(ApiConstants.TOTAL_QUOTA)
    @Param(description = "Total Quota consumed during this period.")
    private BigDecimal totalQuota;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Start date of the Quota statement.")
    private Date startDate = null;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "End date of the Quota statement.")
    private Date endDate = null;

    @SerializedName(ApiConstants.CURRENCY)
    @Param(description = "Currency of the Quota statement.")
    private String currency;

    public QuotaStatementResponse() {
        super();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setLineItem(List<QuotaStatementItemResponse> lineItem) {
        this.lineItem = lineItem;
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

    public void setTotalQuota(BigDecimal totalQuota) {
        this.totalQuota = totalQuota;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
