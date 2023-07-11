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
package org.apache.cloudstack.api.command.admin.vm;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.log4j.Logger;

import java.util.List;

@APICommand(name = "migrateMultipleVMs",
        description = "Attempts Migration of a VM to a different host or Root volume of the vm to a different storage pool",
        responseObject = UserVmResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true)
public class MigrateMultipleVMsCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(MigrateMultipleVMsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = HostResponse.class,
            required = true,
            description = "Destination Host ID to migrate VM to.")
    private List<Long> hostIdList;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "the ID of the virtual machine")
    private List<Long> virtualMachineIdList;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<Long> getHostIdList() {
        return hostIdList;
    }

    public List<Long> getVirtualMachineIdList() {
        return virtualMachineIdList;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        String eventDescription;
        if (getHostIdList() != null) {
            eventDescription = String.format("Attempting to migrate VM id: %s to host Id: %s", getVirtualMachineIdList(), getHostIdList());
        } else {
            eventDescription = String.format("Attempting to migrate VM id: %s", getVirtualMachineIdList());
        }
        return eventDescription;
    }

    @Override
    public void execute() {
        try {
            List<VirtualMachine> migratedVmList = _userVmService.migrateMultipleVms(getVirtualMachineIdList(), getHostIdList());

            List<UserVmResponse> responseList = _responseGenerator.createUserVmResponse(
                    ResponseObject.ResponseView.Full, "virtualmachine", migratedVmList.toArray(new UserVm[0]));

            ListResponse<UserVmResponse> response = new ListResponse<>();
            response.setResponses(responseList);

            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (VirtualMachineMigrationException | ConcurrentOperationException | ManagementServerException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }
}
