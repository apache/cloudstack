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

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.QuotaConfigureEmailResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;

import javax.inject.Inject;

@APICommand(name = QuotaConfigureEmailCmd.API_NAME, responseObject = QuotaConfigureEmailResponse.class, description = "Configure a quota email template", since = "4.16.0.11",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaConfigureEmailCmd extends QuotaBaseCmd {

    public static final String API_NAME = "quotaConfigureEmail";

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, required = true,
            description = "Account ID for which to configure quota template email or min balance")
    private long accountId;

    @Parameter(name = ApiConstants.TEMPLATE_NAME, type = CommandType.STRING, description = "Quota email template name which should be configured")
    private String templateName;

    @Parameter(name = ApiConstants.ENABLE, type = CommandType.BOOLEAN, description = "If the quota email template should be enabled")
    private Boolean enable;

    @Parameter(name = "minbalance", type = CommandType.DOUBLE, description = "New quota account min balance")
    private Double minBalance;

    @Inject
    private QuotaResponseBuilder responseBuilder;

    @Override
    public void execute() {
        Pair<QuotaEmailConfigurationVO, Double> result = responseBuilder.configureQuotaEmail(this);
        QuotaConfigureEmailResponse quotaConfigureEmailResponse = responseBuilder.createQuotaConfigureEmailResponse(result.first(), result.second(), accountId);
        quotaConfigureEmailResponse.setResponseName(getCommandName());
        this.setResponseObject(quotaConfigureEmailResponse);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return accountId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Boolean getEnable() {
        return enable;
    }

    public Double getMinBalance() {
        return minBalance;
    }
}
