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

import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.quota.constant.QuotaConfig;

import javax.inject.Inject;
import java.util.Arrays;

@APICommand(name = "quotaEmailTemplateUpdate", responseObject = SuccessResponse.class, description = "Updates existing email templates for quota alerts", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaEmailTemplateUpdateCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder _quotaResponseBuilder;

    @Parameter(name = "templatetype", type = CommandType.STRING, required=true, description = "Type of the quota email template, allowed types: QUOTA_LOW, QUOTA_EMPTY")
    private String templateName;

    @Parameter(name = "templatesubject", type = CommandType.STRING, required=true, description = "The quota email template subject, max: 77 characters", length = 77)
    private String templateSubject;

    @Parameter(name = "templatebody", type = CommandType.STRING, required=true, description = "The quota email template body, max: 500k characters", length = 512000)
    private String templateBody;

    @Parameter(name = "locale", type = CommandType.STRING, description = "The locale of the email text")
    private String locale;

    @Override
    public void execute() {
        final String templateName = getTemplateName();
        if (templateName == null || getTemplateSubject() == null || getTemplateBody() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Failed to update quota email template due to empty or invalid template name or text");
        }

        boolean isValidTemplateName = false;
        for (QuotaConfig.QuotaEmailTemplateTypes e: QuotaConfig.QuotaEmailTemplateTypes.values()) {
            if (e.toString().equalsIgnoreCase(templateName)) {
                isValidTemplateName = true;
                setTemplateName(e.toString());
                break;
            }
        }
        if (!isValidTemplateName) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid quota email template type, allowed values are: " + Arrays.toString(QuotaConfig.QuotaEmailTemplateTypes.values()));
        }

        if (!_quotaResponseBuilder.updateQuotaEmailTemplate(this)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to update quota email template due to an internal error");
        }
        final SuccessResponse response = new SuccessResponse();
        response.setResponseName(getCommandName());
        response.setSuccess(true);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateSubject() {
        return templateSubject;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public String getLocale() {
        return locale;
    }

    public void setTemplateSubject(String templateSubject) {
        this.templateSubject = templateSubject;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
