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

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.quota.QuotaCreditsVO;

import com.cloud.serializer.Param;

public class QuotaCreditsResponse extends BaseResponse {

    @SerializedName("credits")
    @Param(description = "the credit deposited")
    private Integer credits;

    @SerializedName("balance")
    @Param(description = "the balance credit in account")
    private Integer balance;

    @SerializedName("updated_by")
    @Param(description = "the account name of the admin who updated the credits")
    private String updatedBy;

    @SerializedName("updated_on")
    @Param(description = "the account name of the admin who updated the credits")
    private Timestamp updatedOn;


     public QuotaCreditsResponse() {
         super();
     }


     public QuotaCreditsResponse(QuotaCreditsVO result) {
         super();
         if (result != null){
             this.credits = 100;
             this.balance = 200;
             this.updatedBy = "1";
             this.updatedOn = new Timestamp(System.currentTimeMillis());
         }
     }


    public Integer getCredits() {
        return credits;
    }


    public void setCredits(Integer credits) {
        this.credits = credits;
    }


    public Integer getBalance() {
        return balance;
    }


    public void setBalance(Integer balance) {
        this.balance = balance;
    }


    public String getUpdatedBy() {
        return updatedBy;
    }


    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }


    public Timestamp getUpdatedOn() {
        return updatedOn;
    }


    public void setUpdatedOn(Timestamp updatedOn) {
        this.updatedOn = updatedOn;
    }


}