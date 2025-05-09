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

public class RegisterResponse extends BaseResponse {
    @SerializedName(ApiConstants.API_KEY)
    @Param(description = "the api key of the registered user", isSensitive = true)
    private String apiKey;

    @SerializedName(ApiConstants.SECRET_KEY)
    @Param(description = "the secret key of the registered user", isSensitive = true)
    private String secretKey;

    @SerializedName(ApiConstants.API_KEY_ACCESS)
    @Param(description = "whether api key access is allowed or not", isSensitive = true)
    private Boolean apiKeyAccess;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setApiKeyAccess(Boolean apiKeyAccess) {
        this.apiKeyAccess = apiKeyAccess;
    }
}
