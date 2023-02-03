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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaEmailTemplateResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "quotaEmailTemplateList", responseObject = QuotaEmailTemplateResponse.class, description = "Lists all quota email templates", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaEmailTemplateListCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaEmailTemplateListCmd.class);

    @Inject
    QuotaResponseBuilder _quotaResponseBuilder;

    @Parameter(name = "templatetype", type = CommandType.STRING, description = "List by type of the quota email template, allowed types: QUOTA_LOW, QUOTA_EMPTY")
    private String templateName;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public void execute() {
        final ListResponse<QuotaEmailTemplateResponse> response = new ListResponse<QuotaEmailTemplateResponse>();
        response.setResponses(_quotaResponseBuilder.listQuotaEmailTemplates(this));
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    }
