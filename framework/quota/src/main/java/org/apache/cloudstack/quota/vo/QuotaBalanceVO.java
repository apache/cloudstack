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
@Table(name = "quota_balance")
public class QuotaBalanceVO implements InternalIdentity {

    private static final long serialVersionUID = -7112846845287653210L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = "credit_balance")
    private BigDecimal creditBalance;

    @Column(name = "credits_id")
    private Long creditsId;

    @Column(name = "updated_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updatedOn = null;

    public QuotaBalanceVO() {
    }

    public QuotaBalanceVO(final QuotaCreditsVO credit) {
        super();
        this.accountId = credit.getAccountId();
        this.domainId = credit.getDomainId();
        this.creditBalance = credit.getCredit();
        this.updatedOn = credit.getUpdatedOn() == null ? null : new Date(credit.getUpdatedOn().getTime());
        this.creditsId = credit.getId();
    }

    public QuotaBalanceVO(final Long accountId, final Long domainId, final BigDecimal creditBalance, final Date updatedOn) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.creditBalance = creditBalance;
        this.creditsId = 0L;
        this.updatedOn = updatedOn == null ? null : new Date(updatedOn.getTime());
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getCreditsId() {
        return creditsId;
    }

    public void setCreditsId(Long creditsId) {
        this.creditsId = creditsId;
    }

    public boolean isBalanceEntry(){
        return creditsId==0;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }

    public Date getUpdatedOn() {
        return updatedOn == null ? null : new Date(updatedOn.getTime());
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn == null ? null : new Date(updatedOn.getTime());
    }

    @Override
    public String toString() {
        return "QuotaBalanceVO [id=" + id + ", accountId=" + accountId + ", domainId=" + domainId + ", creditBalance=" + creditBalance + ", creditsId=" + creditsId + ", updatedOn="
                + updatedOn + "]";
    }

}
