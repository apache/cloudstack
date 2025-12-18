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
package com.cloud.api.commands;

import javax.inject.Inject;


import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "stopNetScalerVpx", description = "Stops a NetScalervm.", responseObject = DomainRouterResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class StopNetScalerVMCmd extends BaseAsyncCmd {
    private static final String s_name = "stopNetScalerVmresponse";

    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DomainRouterResponse.class, required = true, description = "the ID of the NetScaler vm")
    private Long id;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false, description = "Force stop the VM (vm is marked as Stopped even when command fails to be send to the backend, otherwise a force poweroff is attempted). To be used if the caller knows the VM is stopped and should be marked as such.")
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
        if (vm != null && vm.getRole() == Role.NETSCALER_VM) {
            return vm.getAccountId();
        } else {
            throw new InvalidParameterValueException("Unable to find NetScaler vm by id");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETSCALER_VM_STOP;
    }

    @Override
    public String getEventDescription() {
        return "stopping Netscaler vm: " + getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DomainRouter;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext.current().setEventDetails("NetScaler vm Id: " + getId());
        VirtualRouter result = null;
        VirtualRouter vm = _routerService.findRouter(getId());
        if (vm == null || vm.getRole() != Role.NETSCALER_VM) {
            throw new InvalidParameterValueException("Can't find NetScaler lb vm by id");
        } else {
            result =   _netsclarLbService.stopNetscalerServiceVm(getId(), isForced(), CallContext.current().getCallingAccount(), CallContext.current().getCallingUserId());
        }

        if (result != null) {
            DomainRouterResponse response = _responseGenerator.createDomainRouterResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to stop Netscaler vm");
        }
    }
}
