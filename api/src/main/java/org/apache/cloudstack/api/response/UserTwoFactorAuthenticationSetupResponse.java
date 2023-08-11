/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class UserTwoFactorAuthenticationSetupResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "The user ID")
    private String id;

    @SerializedName("username")
    @Param(description = "The user name")
    private String username;

    @SerializedName("accountid")
    @Param(description = "The Account ID of the user")
    private String accountId;

    @SerializedName(ApiConstants.SECRET_CODE)
    @Param(description = "Secret code that needs to be registered with authenticator")
    private String secretCode;

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setSecretCode(String secretCode) {
        this.secretCode = secretCode;
    }

    public String getSecretCode() {
        return secretCode;
    }
}
