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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.uservm.UserVm;

@APICommand(name = "attachIso", description = "Attaches an ISO to a virtual machine.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class AttachIsoCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(AttachIsoCmd.class.getName());

    private static final String s_name = "attachisoresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplateResponse.class,
            required = true, description = "the ID of the ISO file")
    protected Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class,
            required = true, description = "the ID of the virtual machine")
    protected Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
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
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find virtual machine by ID " + getVirtualMachineId());
        }

        return vm.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ISO_ATTACH;
    }

    @Override
    public String getEventDescription() {
        return  "attaching ISO: " + getId() + " to VM: " + getVirtualMachineId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Vm Id: " + getVirtualMachineId() + " ISO ID: " + getId());
        boolean result = _templateService.attachIso(id, virtualMachineId);
        if (result) {
            UserVm userVm = _responseGenerator.findUserVmById(virtualMachineId);
            if (userVm != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", userVm).get(0);
                response.setResponseName(DeployVMCmd.getResultObjectName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to attach ISO");
            }
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to attach ISO");
        }
    }
}
