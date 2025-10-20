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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;

import com.cloud.serializer.Param;

public class QuotaBalanceResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "account id")
    private Long accountId;

    @SerializedName("account")
    @Param(description = "account name")
    private String accountName;

    @SerializedName("domain")
    @Param(description = "domain id")
    private Long domainId;

    @SerializedName("startquota")
    @Param(description = "quota started with")
    private BigDecimal startQuota;

    @SerializedName("endquota")
    @Param(description = "quota by end of this period")
    private BigDecimal endQuota;

    @SerializedName("credits")
    @Param(description = "list of credits made during this period")
    private List<QuotaCreditsResponse> credits = null;

    @SerializedName("startdate")
    @Param(description = "start date")
    private Date startDate = null;

    @SerializedName("enddate")
    @Param(description = "end date")
    private Date endDate = null;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    public QuotaBalanceResponse() {
        super();
        credits = new ArrayList<QuotaCreditsResponse>();
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public BigDecimal getStartQuota() {
        return startQuota;
    }

    public void setStartQuota(BigDecimal startQuota) {
        this.startQuota = startQuota.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getEndQuota() {
        return endQuota;
    }

    public void setEndQuota(BigDecimal endQuota) {
        this.endQuota = endQuota.setScale(2, RoundingMode.HALF_EVEN);
    }

    public List<QuotaCreditsResponse> getCredits() {
        return credits;
    }

    public void setCredits(List<QuotaCreditsResponse> credits) {
        this.credits = credits;
    }

    public void addCredits(QuotaBalanceVO credit) {
        QuotaCreditsResponse cr = new QuotaCreditsResponse();
        cr.setCredit(credit.getCreditBalance());
        cr.setCreditedOn(credit.getUpdatedOn());
        credits.add(0, cr);
    }

    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
    }

    public Date getEndDate() {
        return endDate == null ? null : new Date(endDate.getTime());
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
