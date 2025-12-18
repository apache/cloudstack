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

public class QuotaValidateActivationRuleResponse extends BaseResponse {

    @SerializedName("activationrule")
    @Param(description = "The validated activation rule.")
    private String activationRule;

    @SerializedName("quotatype")
    @Param(description = "The Quota usage type used to validate the activation rule.")
    private String quotaType;

    @SerializedName("isvalid")
    @Param(description = "Whether the activation rule is valid.")
    private Boolean isValid;

    @SerializedName("message")
    @Param(description = "The reason whether the activation rule is valid or not.")
    private String message;

    public QuotaValidateActivationRuleResponse() {
        super("validactivationrule");
    }

    public String getActivationRule() {
        return activationRule;
    }

    public void setActivationRule(String activationRule) {
        this.activationRule = activationRule;
    }

    public Boolean isValid() {
        return isValid;
    }

    public void setValid(Boolean valid) {
        isValid = valid;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
