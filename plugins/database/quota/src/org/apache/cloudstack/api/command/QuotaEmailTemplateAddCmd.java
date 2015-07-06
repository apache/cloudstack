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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.api.response.QuotaEmailTemplateResponse;

@APICommand(name = "quotaEmailTemplateAdd", responseObject = QuotaEmailTemplateResponse.class, description = "Add a new email template", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaEmailTemplateAddCmd extends BaseListCmd {

public static final Logger s_logger = Logger
      .getLogger(QuotaEmailTemplateAddCmd.class.getName());

private static final String s_name = "quotaemailtemplateresponse";

@Inject
private QuotaManager _quotaManager;

@Parameter(name = "templatename", type = CommandType.STRING, description = "The name of email template")
private String templateName;

@Parameter(name = "templatetext", type = CommandType.STRING, description = "The text of the email")
private Long templateText;

@Parameter(name = "locale", type = CommandType.STRING, description = "The locale of the email text")
private Integer locale;



public QuotaEmailTemplateAddCmd() {
  super();
}


public QuotaEmailTemplateAddCmd(final QuotaManager quotaManager) {
  super();
  _quotaManager = quotaManager;
}


@Override
public String getCommandName() {
  return s_name;
}


@Override
public void execute() {

  final QuotaEmailTemplateResponse templResponse = null;

  setResponseObject(templResponse);
}


public String getTemplateName() {
    return templateName;
}


public void setTemplateName(String templateName) {
    this.templateName = templateName;
}


public Long getTemplateText() {
    return templateText;
}


public void setTemplateText(Long templateText) {
    this.templateText = templateText;
}


public Integer getLocale() {
    return locale;
}


public void setLocale(Integer locale) {
    this.locale = locale;
}


}
