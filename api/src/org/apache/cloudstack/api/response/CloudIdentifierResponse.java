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

public class CloudIdentifierResponse extends BaseResponse {

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "the user ID for the cloud identifier")
    private String userId;

    @SerializedName("cloudidentifier")
    @Param(description = "the cloud identifier")
    private String cloudIdentifier;

    @SerializedName("signature")
    @Param(description = "the signed response for the cloud identifier")
    private String signature;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCloudIdentifier() {
        return cloudIdentifier;
    }

    public void setCloudIdentifier(String cloudIdentifier) {
        this.cloudIdentifier = cloudIdentifier;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

}
