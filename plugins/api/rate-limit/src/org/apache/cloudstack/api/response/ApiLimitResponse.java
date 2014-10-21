// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class ApiLimitResponse extends BaseResponse {
    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account uuid of the api remaining count")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name of the api remaining count")
    private String accountName;

    @SerializedName("apiIssued")
    @Param(description = "number of api already issued")
    private int apiIssued;

    @SerializedName("apiAllowed")
    @Param(description = "currently allowed number of apis")
    private int apiAllowed;

    @SerializedName("expireAfter")
    @Param(description = "seconds left to reset counters")
    private long expireAfter;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setApiIssued(int apiIssued) {
        this.apiIssued = apiIssued;
    }

    public void setApiAllowed(int apiAllowed) {
        this.apiAllowed = apiAllowed;
    }

    public void setExpireAfter(long duration) {
        this.expireAfter = duration;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public int getApiIssued() {
        return apiIssued;
    }

    public int getApiAllowed() {
        return apiAllowed;
    }

    public long getExpireAfter() {
        return expireAfter;
    }

}
