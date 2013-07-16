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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.ExecutionException;

@APICommand(name = "startVirtualMachine", responseObject = UserVmResponse.class, description = "Starts a virtual machine.")
public class StartVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StartVMCmd.class.getName());

    private static final String s_name = "startvirtualmachineresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType=UserVmResponse.class,
            required = true, description = "The ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.UUID, entityType=HostResponse.class,
            description="destination Host ID to deploy the VM to - parameter available for root admin only", since="3.0.1")
    private Long hostId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getHostId() {
        return hostId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_START;
    }

    @Override
    public String getEventDescription() {
        return "starting user vm: " + getId();
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException {
        try {
            CallContext.current().setEventDetails("Vm Id: " + getId());

            UserVm result ;
            result = _userVmService.startVirtualMachine(this);

            if (result != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", result).get(0);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start a vm");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (StorageUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ExecutionException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientCapacityException ex) {
            StringBuilder message = new StringBuilder(ex.getMessage());
            if (ex instanceof InsufficientServerCapacityException) {
                if (((InsufficientServerCapacityException) ex).isAffinityApplied()) {
                    message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                }
            }
            s_logger.info(ex);
            s_logger.info(message.toString(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
        }
    }

}
