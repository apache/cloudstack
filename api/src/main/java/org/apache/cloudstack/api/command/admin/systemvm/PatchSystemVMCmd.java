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
package org.apache.cloudstack.api.command.admin.systemvm;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

@APICommand(name = "patchSystemVm", description = "Attempts to live patch systemVMs - CPVM, SSVM ",
        responseObject = SuccessResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, authorized = { RoleType.Admin }, since = "4.17.0")
public class PatchSystemVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(PatchSystemVMCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = SystemVmResponse.class,
            description = "patches systemVM - CPVM/SSVM with the specified ID")
    private Long id;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN,
            description = "If true, initiates copy of scripts and restart of the agent, even if the scripts version matches." +
                    "To be used with ID parameter only")
    private Boolean force;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }

    public boolean isForced() {
        return force != null && force;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LIVE_PATCH_SYSTEMVM;
    }

    @Override
    public String getEventDescription() {
        return String.format("Attempting to live patch System VM with Id: %s ", this._uuidMgr.getUuid(VirtualMachine.class, getId()));
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Pair<Boolean, String> patched = _mgr.patchSystemVM(this);
        if (patched.first()) {
            final SuccessResponse response = new SuccessResponse(getCommandName());
            response.setDisplayText(patched.second());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, patched.second());
        }
    }
}
