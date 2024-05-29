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

import org.apache.cloudstack.api.ApiConstants;
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
    @Param(description = "the start date of the quota tariff")
    private Date effectiveOn = null;

    @SerializedName("usageTypeDescription")
    @Param(description = "usage type description")
    private String usageTypeDescription;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    @SerializedName("endDate")
    @Param(description = "the end date of the quota tariff")
    private Date endDate;

    @SerializedName("activationRule")
    @Param(description = "activation rule of the quota tariff")
    private String activationRule;

    @SerializedName("name")
    @Param(description = "name")
    private String name;

    @SerializedName("description")
    @Param(description = "description")
    private String description;

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the tariff")
    private String id;

    @SerializedName("removed")
    @Param(description = "when the quota tariff was removed")
    private Date removed;

    @SerializedName("position")
    @Param(description = "position in the execution sequence for tariffs of the same type")
    private Integer position;


    public QuotaTariffResponse() {
        super();
        this.setObjectName("quotatariff");
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

    public void setUsageUnit(String usageUnit) {
        this.usageUnit = usageUnit;
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

    public void setUsageTypeDescription(String usageTypeDescription) {
        this.usageTypeDescription = usageTypeDescription;
    }

    public void setEffectiveOn(Date effectiveOn) {
        this.effectiveOn = effectiveOn;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getActivationRule() {
        return activationRule;
    }

    public void setActivationRule(String activationRule) {
        this.activationRule = activationRule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

}
