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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;


@APICommand(name = "deployVirtualMachine", description = "Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.", responseObject = UserVmResponse.class, responseView = ResponseView.Full, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class DeployVMCmdByAdmin extends DeployVMCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmdByAdmin.class.getName());


    @Override
    public void execute(){
        UserVm result;

        if (getStartVm()) {
            try {
                CallContext.current().setEventDetails("Vm Id: "+getEntityId());
                result = _userVmService.startVirtualMachine(this);
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
            } catch (ConcurrentOperationException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
            } catch (InsufficientCapacityException ex) {
                StringBuilder message = new StringBuilder(ex.getMessage());
                if (ex instanceof InsufficientServerCapacityException) {
                    if(((InsufficientServerCapacityException)ex).isAffinityApplied()){
                        message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                    }
                }
                s_logger.info(ex);
                s_logger.info(message.toString(), ex);
                throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
            }
        } else {
            result = _userVmService.getUserVm(getEntityId());
        }

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm");
        }
    }

}
