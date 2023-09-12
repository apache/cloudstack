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
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class QuotaCreditsResponse extends BaseResponse {

    @SerializedName("credits")
    @Param(description = "the credit deposited")
    private BigDecimal credits;

    @SerializedName("updated_by")
    @Param(description = "the user name of the admin who updated the credits")
    private String updatedBy;

    @SerializedName("updated_on")
    @Param(description = "the account name of the admin who updated the credits")
    private Date updatedOn;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    public QuotaCreditsResponse() {
        super();
    }

    public QuotaCreditsResponse(QuotaCreditsVO result, String updatedBy) {
        super();
        if (result != null) {
            setCredits(result.getCredit());
            setUpdatedBy(updatedBy);
            setUpdatedOn(new Date());
        }
    }

    public BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(BigDecimal credits) {
        this.credits = credits.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
