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
package org.apache.cloudstack.api.command.user.iso;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "updateIsoPermissions", description = "Updates ISO permissions", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateIsoPermissionsCmd extends BaseUpdateTemplateOrIsoPermissionsCmd {
    @Override
    protected String getResponseName() {
        return "updateisopermissionsresponse";
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
