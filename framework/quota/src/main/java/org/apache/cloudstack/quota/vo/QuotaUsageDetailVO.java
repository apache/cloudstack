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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Entity
@Table(name = "quota_usage_detail")
public class QuotaUsageDetailVO implements InternalIdentity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "tariff_id")
    private Long tariffId;

    @Column(name = "quota_usage_id")
    private Long quotaUsageId;

    @Column(name = "quota_used")
    private BigDecimal quotaUsed;

    public QuotaUsageDetailVO() {
        quotaUsed = new BigDecimal(0);
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getTariffId() {
        return tariffId;
    }

    public Long getQuotaUsageId() {
        return quotaUsageId;
    }

    public BigDecimal getQuotaUsed() {
        return quotaUsed;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTariffId(Long tariffId) {
        this.tariffId = tariffId;
    }

    public void setQuotaUsageId(Long quotaUsageId) {
        this.quotaUsageId = quotaUsageId;
    }

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).toString();
    }
}
