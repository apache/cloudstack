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
package org.apache.cloudstack.api;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.ResponseViewProvider;
import org.apache.cloudstack.api.response.TemplatePermissionsResponse;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

public abstract class BaseListTemplateOrIsoPermissionsCmd extends BaseCmd implements ResponseViewProvider {
    public Logger logger = getLogger();
    protected static final String s_name = "listtemplatepermissionsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplatePermissionsResponse.class, required = true, description = "the template ID")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    protected Logger getLogger() {
        return Logger.getLogger(BaseListTemplateOrIsoPermissionsCmd.class);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    protected boolean templateIsCorrectType(VirtualMachineTemplate template) {
        return true;
    }

    public String getMediaType() {
        return "templateOrIso";
    }

    @Override
    public void execute() {
        executeWithView(getResponseView());
    }

    protected void executeWithView(ResponseView view) {
        List<String> accountNames = _templateService.listTemplatePermissions(this);

        TemplatePermissionsResponse response = _responseGenerator.createTemplatePermissionsResponse(view, accountNames, id);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }


}
