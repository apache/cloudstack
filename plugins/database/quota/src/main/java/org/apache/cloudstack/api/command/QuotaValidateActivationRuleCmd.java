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
package org.apache.cloudstack.api.command;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaValidateActivationRuleResponse;
import org.apache.cloudstack.quota.constant.QuotaTypes;

import javax.inject.Inject;

@APICommand(name = "quotaValidateActivationRule", responseObject = QuotaValidateActivationRuleResponse.class, description = "Validates if the given activation rule is valid for the informed usage type.", since = "4.20.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaValidateActivationRuleCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder responseBuilder;

    @Parameter(name = ApiConstants.ACTIVATION_RULE, type = CommandType.STRING, required = true, description = "Quota tariff's activation rule to validate. The activation rule is valid if it has no syntax errors and all " +
            "variables are compatible with the given usage type.", length = 65535)
    private String activationRule;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = true, description = "The Quota usage type used to validate the activation rule.")
    private Integer quotaType;

    @Override
    public void execute() {
        QuotaValidateActivationRuleResponse response = responseBuilder.validateActivationRule(this);

        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getActivationRule() {
        return activationRule;
    }

    public QuotaTypes getQuotaType() {
        QuotaTypes quotaTypes = QuotaTypes.getQuotaType(quotaType);

        if (quotaTypes == null) {
            throw new InvalidParameterValueException(String.format("Usage type not found for value [%s].", quotaType));
        }

        return quotaTypes;
    }
}
