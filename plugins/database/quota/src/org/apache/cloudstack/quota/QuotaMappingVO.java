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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "quota_mapping")
public class QuotaMappingVO implements InternalIdentity {
    private static final long serialVersionUID = -7117933766387653203L;

    @Id
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

    @Column(name = "include")
    private int include;

    @Column(name = "description")
    private String description;

    public QuotaMappingVO() {
    }

    public QuotaMappingVO(final int usagetype, final String usagename, final String usageunit, final String usagediscriminator, final BigDecimal currencyvalue, final int include, final String description) {
        this.usageType = usagetype;
        this.usageName = usagename;
        this.usageUnit = usageunit;
        this.usageDiscriminator = usagediscriminator;
        this.currencyValue = currencyvalue;
        this.include = include;
        this.description = description;
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

    public int getInclude() {
        return include;
    }

    public void setInclude(int include) {
        this.include = include;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return this.usageType;
    }
}