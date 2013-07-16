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

import org.apache.cloudstack.api.response.TemplatePermissionsResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

public class BaseListTemplateOrIsoPermissionsCmd extends BaseCmd {
    public Logger s_logger = getLogger();
    protected String s_name = "listtemplatepermissionsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = TemplatePermissionsResponse.class,
            required=true, description="the template ID")
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

    protected Logger getLogger() {
        return Logger.getLogger(BaseUpdateTemplateOrIsoPermissionsCmd.class.getName());
    }

    @Override
    public void execute(){
        List<String> accountNames = _templateService.listTemplatePermissions(this);

        Account account = CallContext.current().getCallingAccount();
        boolean isAdmin = (isAdmin(account.getType()));

        TemplatePermissionsResponse response = _responseGenerator.createTemplatePermissionsResponse(accountNames, id, isAdmin);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
