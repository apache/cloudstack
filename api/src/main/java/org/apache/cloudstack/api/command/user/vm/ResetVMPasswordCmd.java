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
package org.apache.cloudstack.api.command.user.vm;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "resetPasswordForVirtualMachine", responseObject=UserVmResponse.class, description="Resets the password for virtual machine. " +
                    "The virtual machine must be in a \"Stopped\" state and the template must already " +
        "support this feature for this command to take effect. [async]", responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ResetVMPasswordCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ResetVMPasswordCmd.class.getName());

    private static final String s_name = "resetpasswordforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    // unexposed parameter needed for serializing/deserializing the command
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, expose=false)
    protected String password;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_RESETPASSWORD;
    }

    @Override
    public String getEventDescription() {
        return  "resetting password for vm: " + getId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException {
        password = _mgr.generateRandomPassword();
        CallContext.current().setEventDetails("Vm Id: " + getId());
        UserVm result = _userVmService.resetVMPassword(this, password);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reset vm password");
        }
    }
}
