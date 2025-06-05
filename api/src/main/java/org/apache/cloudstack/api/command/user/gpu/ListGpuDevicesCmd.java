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
package org.apache.cloudstack.api.command.user.gpu;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "listGpuDevices", description = "Lists all available GPU devices",
        responseView = ResponseObject.ResponseView.Restricted,
        responseObject = GpuDeviceResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.21.0", authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListGpuDevicesCmd extends BaseListCmd implements UserCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class,
            description = "the virtual machine ID to which the GPU device is allocated")
    private Long vmId;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation //////////////////
    /// //////////////////////////////////////////////////

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Listing GPU devices");
        ListResponse<GpuDeviceResponse> response = gpuService.listGpuDevices(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
