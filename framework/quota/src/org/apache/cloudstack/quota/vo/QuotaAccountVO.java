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
package org.apache.cloudstack.quota.vo;

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "quota_account")
public class QuotaAccountVO implements InternalIdentity {

    private static final long serialVersionUID = -7112846845287653210L;

    @Id
    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "quota_enforce")
    private Integer quotaEnforce = 0;

    @Column(name = "quota_balance")
    private BigDecimal quotaBalance;

    @Column(name = "quota_balance_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date quotaBalanceDate = null;

    @Column(name = "quota_min_balance")
    private BigDecimal quotaMinBalance;

    @Column(name = "quota_alert_type")
    private Integer quotaAlertType = null;

    @Column(name = "quota_alert_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date quotaAlertDate = null;

    @Column(name = "last_statement_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastStatementDate = null;

    public QuotaAccountVO() {
    }

    public QuotaAccountVO(Long accountId) {
        super();
        this.accountId = accountId;
    }

    @Override
    public long getId() {
        return accountId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getQuotaEnforce() {
        return quotaEnforce == null ? 0 : quotaEnforce;
    }

    public void setQuotaEnforce(Integer quotaEnforce) {
        this.quotaEnforce = quotaEnforce;
    }

    public BigDecimal getQuotaBalance() {
        return quotaBalance;
    }

    public void setQuotaBalance(BigDecimal quotaBalance) {
        this.quotaBalance = quotaBalance;
    }

    public BigDecimal getQuotaMinBalance() {
        return quotaMinBalance == null ? new BigDecimal(0) : quotaMinBalance;
    }

    public void setQuotaMinBalance(BigDecimal quotaMinBalance) {
        this.quotaMinBalance = quotaMinBalance;
    }

    public Integer getQuotaAlertType() {
        return quotaAlertType;
    }

    public void setQuotaAlertType(Integer quotaAlertType) {
        this.quotaAlertType = quotaAlertType;
    }

    public Date getQuotaAlertDate() {
        return quotaAlertDate == null ? null : new Date(quotaAlertDate.getTime());
    }

    public void setQuotaAlertDate(Date quotaAlertDate) {
        this.quotaAlertDate = quotaAlertDate == null ? null : new Date(quotaAlertDate.getTime());
    }

    public Date getQuotaBalanceDate() {
        return quotaBalanceDate  == null ? null : new Date(quotaBalanceDate.getTime());
    }

    public void setQuotaBalanceDate(Date quotaBalanceDate) {
        this.quotaBalanceDate = quotaBalanceDate == null ? null : new Date(quotaBalanceDate.getTime());
    }

    public Date getLastStatementDate() {
        return lastStatementDate  == null ? null : new Date(lastStatementDate.getTime());
    }

    public void setLastStatementDate(Date lastStatementDate) {
        this.lastStatementDate = lastStatementDate  == null ? null : new Date(lastStatementDate.getTime());
    }

    @Override
    public String toString() {
        return "QuotaAccountVO [accountId=" + accountId + ", quotaEnforce=" + quotaEnforce + ", quotaBalance=" + quotaBalance + ", quotaBalanceDate=" + quotaBalanceDate
                + ", quotaMinBalance=" + quotaMinBalance + ", quotaAlertType=" + quotaAlertType + ", quotaAlertDate=" + quotaAlertDate + ", lastStatementDate=" + lastStatementDate
                + "]";
    }

}
