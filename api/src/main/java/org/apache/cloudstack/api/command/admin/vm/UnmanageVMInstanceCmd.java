//
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
//

package org.apache.cloudstack.api.command.admin.vm;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UnmanageVMInstanceResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.UnmanagedVMsManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = UnmanageVMInstanceCmd.API_NAME,
        description = "Unmanage a guest virtual machine.",
        entityType = {VirtualMachine.class},
        responseObject = UnmanageVMInstanceResponse.class,
        requestHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.15.0")
public class UnmanageVMInstanceCmd extends BaseAsyncCmd {

    public static final Logger LOGGER = Logger.getLogger(UnmanageVMInstanceCmd.class);
    public static final String API_NAME = "unmanageVirtualMachine";

    @Inject
    private UnmanagedVMsManager unmanagedVMsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = UserVmResponse.class, required = true,
            description = "The ID of the virtual machine to unmanage")
    private Long vmId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_UNMANAGE;
    }

    @Override
    public String getEventDescription() {
        return "unmanaging VM. VM ID = " + vmId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UnmanageVMInstanceResponse response = new UnmanageVMInstanceResponse();
        try {
            CallContext.current().setEventDetails("VM ID = " + vmId);
            boolean result = unmanagedVMsManager.unmanageVMInstance(vmId);
            response.setSuccess(result);
            if (result) {
                response.setDetails("VM unmanaged successfully");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(vmId);
        if (vm != null) {
            return vm.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return vmId;
    }

}