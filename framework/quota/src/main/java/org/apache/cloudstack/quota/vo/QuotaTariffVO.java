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
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "quota_tariff")
public class QuotaTariffVO implements InternalIdentity {
    private static final long serialVersionUID = -7117933766387653203L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "usage_type")
    private int usageType;

    @Column(name = "usage_name")
    private String usageName;

    @Column(name = "usage_unit")
    private String usageUnit;

    @Column(name = "usage_discriminator")
    private String usageDiscriminator;

    @Column(name = "currency_value")
    private BigDecimal currencyValue;

    @Column(name = "effective_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date effectiveOn = null;

    @Column(name = "updated_on")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updatedOn = null;

    @Column(name = "updated_by")
    private Long updatedBy = null;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, length = 65535)
    protected String name = null;

    @Column(name = "description", length = 65535)
    protected String description;

    @Column(name = "activation_rule", length = 65535)
    protected String activationRule;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    @Column(name = "end_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endDate;

    public QuotaTariffVO() {
    }

    public QuotaTariffVO(final int usagetype) {
        this.usageType = usagetype;
    }

    public QuotaTariffVO(final int usagetype, final String usagename, final String usageunit, final String usagediscriminator, final BigDecimal currencyvalue,
            final Date effectiveOn, final Date updatedOn, final long updatedBy) {
        this.usageType = usagetype;
        this.usageName = usagename;
        this.usageUnit = usageunit;
        this.usageDiscriminator = usagediscriminator;
        this.currencyValue = currencyvalue;
        this.effectiveOn = effectiveOn;
        this.updatedOn = updatedOn == null ? null : new Date(updatedOn.getTime());
        this.updatedBy = updatedBy;
    }


    public QuotaTariffVO(QuotaTariffVO that) {
        this(that.getUsageType(), that.getUsageName(), that.getUsageUnit(), that.getUsageDiscriminator(), that.getCurrencyValue(), that.getEffectiveOn(), that.getUpdatedOn(),
                that.getUpdatedBy());
        this.setName(that.getName());
        this.setDescription(that.getDescription());
        this.setActivationRule(that.getActivationRule());
        this.setEndDate(that.getEndDate());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getEffectiveOn() {
        return effectiveOn == null ? null : new Date(effectiveOn.getTime());
    }

    public void setEffectiveOn(Date effectiveOn) {
        this.effectiveOn = effectiveOn == null ? null : new Date(effectiveOn.getTime());
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

    public int getUsageType() {
        return usageType;
    }

    public void setUsageType(int usageType) {
        this.usageType = usageType;
    }

    public String getUsageName() {
        return usageName;
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public String getUsageUnit() {
        return usageUnit;
    }

    public void setUsageUnit(String usageUnit) {
        this.usageUnit = usageUnit;
    }

    public String getUsageDiscriminator() {
        return usageDiscriminator;
    }

    public void setUsageDiscriminator(String usageDiscriminator) {
        this.usageDiscriminator = usageDiscriminator;
    }

    public BigDecimal getCurrencyValue() {
        return currencyValue;
    }

    public void setCurrencyValue(BigDecimal currencyValue) {
        this.currencyValue = currencyValue;
    }

    public String getUsageTypeDescription() {
        return QuotaTypes.getDescription(usageType);
    }

    public Long getIdObj(){
        return id;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivationRule() {
        return activationRule;
    }

    public void setActivationRule(String activationRule) {
        this.activationRule = activationRule;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getUuid() {
        return uuid;
    }

    public boolean setUsageTypeData(int usageType) {
        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(usageType);

        if (quotaType == null) {
            return false;
        }

        this.setUsageType(usageType);
        this.setUsageName(quotaType.getQuotaName());
        this.setUsageUnit(quotaType.getQuotaUnit());
        this.setUsageDiscriminator(quotaType.getDiscriminator());

        return true;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "uuid", "name", "effectiveOn", "endDate");
    };
}
