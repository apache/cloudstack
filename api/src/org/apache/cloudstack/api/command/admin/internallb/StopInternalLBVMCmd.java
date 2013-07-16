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
package org.apache.cloudstack.api.command.admin.internallb;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;

@APICommand(name = "stopInternalLoadBalancerVM", description = "Stops an Internal LB vm.", responseObject = DomainRouterResponse.class)
public class StopInternalLBVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StopInternalLBVMCmd.class.getName());
    private static final String s_name = "stopinternallbvmresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DomainRouterResponse.class,
            required = true, description = "the ID of the internal lb vm")
    private Long id;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false, description = "Force stop the VM. The caller knows the VM is stopped.")
    private Boolean forced;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualRouter vm = _entityMgr.findById(VirtualRouter.class, getId());
        if (vm != null && vm.getRole() == Role.INTERNAL_LB_VM) {
            return vm.getAccountId();
        } else {
            throw new InvalidParameterValueException("Unable to find internal lb vm by id");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_INTERNAL_LB_VM_STOP;
    }

    @Override
    public String getEventDescription() {
        return "stopping internal lb vm: " + getId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.InternalLbVm;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext.current().setEventDetails("Internal lb vm Id: "+getId());
        VirtualRouter result = null;
        VirtualRouter vm = _routerService.findRouter(getId());
        if (vm == null || vm.getRole() != Role.INTERNAL_LB_VM) {
            throw new InvalidParameterValueException("Can't find internal lb vm by id");
        } else {
            result = _internalLbSvc.stopInternalLbVm(getId(), isForced(), CallContext.current().getCallingAccount(), CallContext.current().getCallingUserId());
        } 
        
        if (result != null) {
            DomainRouterResponse response = _responseGenerator.createDomainRouterResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to stop internal lb vm");
        }
    }
}
