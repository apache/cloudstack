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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "quota_configuration")
public class QuotaConfigurationVO implements InternalIdentity {
    /**
     * enable.quota.service: Enable quota service by default all of this
     * functionality is disabled. quota.period.type : Quota period type: 1 for
     * every x days, 2 for certain day of the month, 3 for yearly on activation
     * day - default usage reporting cycle quota.period.config : The value for
     * the above quota period type quota.activity.generate : Set “Y” to enable a
     * detailed log of the quota usage, rating and billing activity, on daily
     * basis. Valid values (“Y”, “N”); record.outgoingEmail: Boolean, yes means
     * all the emails sent out will be stored in local DB, by default it is no.
     * quota.enable : enable the usage quota enforcement quota.currencySymbol :
     * The symbol for the currency in use to measure usage. quota.criticalLimit:
     * A limit when it is reached user is sent and alert.
     * quota.incrementalLimit: A incremental limit that is added to
     * criticalLimit in this increments, when breached a email is send to the
     * user with details.
     * */

    private static final long serialVersionUID = -7117933766387653203L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "usage_type")
    private String usageType;

    @Column(name = "usage_unit")
    private String usageUnit;

    @Column(name = "usage_discriminator")
    private String usageDiscriminator;

    @Column(name = "currency_value")
    private int currencyValue;

    @Column(name = "include")
    private int include;

    @Column(name = "description")
    private String description;


    public QuotaConfigurationVO() {
    }


    public QuotaConfigurationVO(final String usagetype, final String usageunit, final String usagediscriminator, final int currencyvalue, final int include, final String description) {
        this.usageType = usagetype;
        this.usageUnit = usageunit;
        this.usageDiscriminator = usagediscriminator;
        this.currencyValue = currencyvalue;
        this.include = include;
        this.description = description;
    }


    public String getUsageType() {
        return usageType;
    }


    public void setUsageType(String usageType) {
        this.usageType = usageType;
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


    public int getCurrencyValue() {
        return currencyValue;
    }


    public void setCurrencyValue(int currencyValue) {
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
        return this.id;
    }
}