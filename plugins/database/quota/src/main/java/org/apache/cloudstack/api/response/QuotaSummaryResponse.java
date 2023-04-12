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
import java.math.RoundingMode;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.cloud.user.Account.State;

public class QuotaSummaryResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "account id")
    private String accountId;

    @SerializedName("account")
    @Param(description = "account name")
    private String accountName;

    @SerializedName("domainid")
    @Param(description = "domain id")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "domain name")
    private String domainName;

    @SerializedName("balance")
    @Param(description = "account balance")
    private BigDecimal balance;

    @SerializedName("state")
    @Param(description = "account state")
    private State state;

    @SerializedName("quota")
    @Param(description = "quota usage of this period")
    private BigDecimal quotaUsage;

    @SerializedName("startdate")
    @Param(description = "start date")
    private Date startDate = null;

    @SerializedName("enddate")
    @Param(description = "end date")
    private Date endDate = null;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    @SerializedName("quotaenabled")
    @Param(description = "if the account has the quota config enabled")
    private boolean quotaEnabled;

    public QuotaSummaryResponse() {
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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public BigDecimal getQuotaUsage() {
        return quotaUsage;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setQuotaUsage(BigDecimal startQuota) {
        this.quotaUsage = startQuota.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Date getStartDate() {
        return startDate == null ?  null : new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate =  startDate == null ?  null : new Date(startDate.getTime());
    }

    public Date getEndDate() {
        return  endDate == null ?  null : new Date(endDate.getTime());
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate == null ?  null : new Date(endDate.getTime());
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean getQuotaEnabled() {
        return quotaEnabled;
    }

    public void setQuotaEnabled(boolean quotaEnabled) {
        this.quotaEnabled = quotaEnabled;
    }
}
