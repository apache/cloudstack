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
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "quota_balance")
public class QuotaBalanceVO implements InternalIdentity {

    public QuotaBalanceVO(Long accountId, Long domainId,
            BigDecimal creditBalance, Date updatedOn, Long previousUpdateId,
            Date previousUpdateOn) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.creditBalance = creditBalance;
        this.updatedOn = updatedOn;
        this.previousUpdateId = previousUpdateId;
        this.previousUpdateOn = previousUpdateOn;
    }

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

    @Column(name = "updated_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updatedOn = null;

    @Column(name = "previous_update_id")
    private Long previousUpdateId;

    @Column(name = "previous_update_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date previousUpdateOn = null;

    public QuotaBalanceVO() {
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

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public Long getPreviousUpdateId() {
        return previousUpdateId;
    }

    public void setPreviousUpdateId(Long previousUpdateId) {
        this.previousUpdateId = previousUpdateId;
    }

    public Date getPreviousUpdateOn() {
        return previousUpdateOn;
    }

    public void setPreviousUpdateOn(Date previousUpdateOn) {
        this.previousUpdateOn = previousUpdateOn;
    }

}
