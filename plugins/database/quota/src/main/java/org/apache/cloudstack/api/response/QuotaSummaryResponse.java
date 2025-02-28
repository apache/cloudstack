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
import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.cloud.user.Account.State;

public class QuotaSummaryResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "Account's ID")
    private String accountId;

    @SerializedName("account")
    @Param(description = "Account's name")
    private String accountName;

    @SerializedName("domainid")
    @Param(description = "Domain's ID")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "Domain's path")
    private String domainPath;

    @SerializedName("balance")
    @Param(description = "Account's balance")
    private BigDecimal balance;

    @SerializedName("state")
    @Param(description = "Account's state")
    private State state;

    @SerializedName("domainremoved")
    @Param(description = "If the domain is removed or not")
    private boolean domainRemoved;

    @SerializedName("accountremoved")
    @Param(description = "If the account is removed or not")
    private boolean accountRemoved;

    @SerializedName("quota")
    @Param(description = "Quota consumed between the startdate and enddate")
    private BigDecimal quotaUsage;

    @SerializedName("startdate")
    @Param(description = "Start date of the quota consumption")
    private Date startDate;

    @SerializedName("enddate")
    @Param(description = "End date of the quota consumption")
    private Date endDate;

    @SerializedName("currency")
    @Param(description = "Currency")
    private String currency;

    @SerializedName("quotaenabled")
    @Param(description = "if the account has the quota config enabled")
    private boolean quotaEnabled;

    @SerializedName("projectname")
    @Param(description = "Name of the project")
    private String projectName;

    @SerializedName("projectid")
    @Param(description = "Project's id")
    private String projectId;

    @SerializedName("projectremoved")
    @Param(description = "Whether the project is removed or not")
    private Boolean projectRemoved;

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

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setQuotaUsage(BigDecimal quotaUsage) {
        this.quotaUsage = quotaUsage;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setQuotaEnabled(boolean quotaEnabled) {
        this.quotaEnabled = quotaEnabled;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setProjectRemoved(Boolean projectRemoved) {
        this.projectRemoved = projectRemoved;
    }

    public void setDomainRemoved(boolean domainRemoved) {
        this.domainRemoved = domainRemoved;
    }

    public void setAccountRemoved(boolean accountRemoved) {
        this.accountRemoved = accountRemoved;
    }
}
