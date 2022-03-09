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
package org.apache.cloudstack.api.command.user.template;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "updateTemplate", description = "Updates attributes of a template.", responseObject = TemplateResponse.class, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateTemplateCmd extends BaseUpdateTemplateOrIsoCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateTemplateCmd.class.getName());
    private static final String s_name = "updatetemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = "templatetype", type = CommandType.STRING, description = "the type of the template")
    private String templateType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public Boolean getBootable() {
        return null;
    }

    public String getTemplateType() {
        return templateType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public TemplateResponse getResponse() {
       return null;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Template;
    }

    @Override
    public void execute() {
        VirtualMachineTemplate result = _templateService.updateTemplate(this);
        if (result != null) {
            TemplateResponse response = _responseGenerator.createTemplateUpdateResponse(getResponseView(), result);
            response.setObjectName("template");
            response.setTemplateType(result.getTemplateType().toString());//Template can be either USER or ROUTING type
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update template");
        }
    }
}
