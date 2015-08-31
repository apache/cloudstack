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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import java.math.BigDecimal;
import java.util.Date;

public class QuotaTariffResponse extends BaseResponse {

    @SerializedName("usageType")
    @Param(description = "usageType")
    private int usageType;

    @SerializedName("usageName")
    @Param(description = "usageName")
    private String usageName;

    @SerializedName("usageUnit")
    @Param(description = "usageUnit")
    private String usageUnit;

    @SerializedName("usageDiscriminator")
    @Param(description = "usageDiscriminator")
    private String usageDiscriminator;

    @SerializedName("tariffValue")
    @Param(description = "tariffValue")
    private BigDecimal tariffValue;

    @SerializedName("effectiveDate")
    @Param(description = "the date on/after which this quota value will be effective")
    private Date effectiveOn = null;

    @SerializedName("description")
    @Param(description = "description")
    private String description;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    public QuotaTariffResponse() {
        super();
        this.setObjectName("quotatariff");
    }

    public QuotaTariffResponse(final int usageType) {
        super();
        this.usageType = usageType;
    }

    public String getUsageName() {
        return usageName;
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public int getUsageType() {
        return usageType;
    }

    public void setUsageType(int usageType) {
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

    public BigDecimal getTariffValue() {
        return tariffValue;
    }

    public void setTariffValue(BigDecimal tariffValue) {
        this.tariffValue = tariffValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEffectiveOn() {
        return effectiveOn;
    }

    public void setEffectiveOn(Date effectiveOn) {
        this.effectiveOn = effectiveOn;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
