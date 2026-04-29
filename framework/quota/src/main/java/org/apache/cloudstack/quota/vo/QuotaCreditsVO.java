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
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "quota_credits")
public class QuotaCreditsVO implements InternalIdentity {

    private static final long serialVersionUID = -3576833845287653210L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = "credit")
    private BigDecimal credit;

    @Column(name = "updated_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updatedOn = null;

    public QuotaCreditsVO() {
    }

    public QuotaCreditsVO(long accountId, long domainId, BigDecimal credit, long updatedBy) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.credit = credit;
        this.updatedBy = updatedBy;
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

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public Date getUpdatedOn() {
        return updatedOn == null ? null : new Date(updatedOn.getTime());
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn == null ? null : new Date(updatedOn.getTime());
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // User ID of the creditor
    @Column(name = "updated_by")
    private Long updatedBy = null;

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "accountId", "domainId", "credit");
    }
}
