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

import java.util.List;

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
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "destroyVirtualMachine", description = "Destroys a virtual machine.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class DestroyVMCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(DestroyVMCmd.class.getName());

    private static final String s_name = "destroyvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.EXPUNGE,
               type = CommandType.BOOLEAN,
               description = "If true is passed, the vm is expunged immediately. False by default.",
               since = "4.2.1")
    private Boolean expunge;

    @Parameter( name = ApiConstants.VOLUME_IDS,
                type = CommandType.LIST,
                collectionType = CommandType.UUID,
                entityType = VolumeResponse.class,
                description = "Comma separated list of UUIDs for volumes that will be deleted",
                since = "4.12.0")
    private List<Long> volumeIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean getExpunge() {
        if (expunge == null) {
            return false;
        }
        return expunge;
    }

    public List<Long> getVolumeIds() {
        return volumeIds;
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
        return EventTypes.EVENT_VM_DESTROY;
    }

    @Override
    public String getEventDescription() {
        return  "destroying vm: " + this._uuidMgr.getUuid(VirtualMachine.class, getId());
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
    public void execute() throws ResourceUnavailableException, ConcurrentOperationException {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));
        UserVm result = _userVmService.destroyVm(this);

        UserVmResponse response = new UserVmResponse();
        if (result != null) {
            List<UserVmResponse> responses = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result);
            if (responses != null && !responses.isEmpty()) {
                response = responses.get(0);
            }
            response.setResponseName("virtualmachine");
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to destroy vm");
        }
    }
}
