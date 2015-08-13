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
@Table(name = "quota_usage")
public class QuotaUsageVO implements InternalIdentity {

    private static final long serialVersionUID = -7117933845287204781L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "zone_id")
    private Long zoneId = null;

    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "domain_id")
    private Long domainId = null;

    @Column(name = "usage_item_id")
    private Long usageItemId;

    @Column(name = "usage_type")
    private int usageType;

    @Column(name = "quota_used")
    private BigDecimal quotaUsed;

    @Column(name = "start_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startDate = null;

    @Column(name = "end_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endDate = null;

    public QuotaUsageVO() {
        usageType = -1;
        quotaUsed = new BigDecimal(0);
        endDate = new Date();
        startDate = new Date();
    }

    public QuotaUsageVO(Long usageItemId, Long zoneId, Long accountId, Long domainId, int usageType, BigDecimal quotaUsed, Date startDate, Date endDate) {
        super();
        this.usageItemId = usageItemId;
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.usageType = usageType;
        this.quotaUsed = quotaUsed;
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public QuotaUsageVO(QuotaUsageVO toclone) {
        super();
        this.usageItemId = toclone.usageItemId;
        this.zoneId = toclone.zoneId;
        this.accountId = toclone.accountId;
        this.domainId = toclone.domainId;
        this.usageType = toclone.usageType;
        this.quotaUsed = toclone.quotaUsed;
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
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

    @Override
    public long getId() {
        return id;
    }

    public Long getUsageItemId() {
        return usageItemId;
    }

    public void setUsageItemId(Long usageItemId) {
        this.usageItemId = usageItemId;
    }

    public int getUsageType() {
        return usageType;
    }

    public void setUsageType(int usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getQuotaUsed() {
        return quotaUsed;
    }

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed;
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

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "QuotaUsageVO [id=" + id + ", zoneId=" + zoneId + ", accountId=" + accountId + ", domainId=" + domainId + ", usageItemId=" + usageItemId + ", usageType=" + usageType
                + ", quotaUsed=" + quotaUsed + ", startDate=" + startDate + ", endDate=" + endDate + "]";
    }

}
