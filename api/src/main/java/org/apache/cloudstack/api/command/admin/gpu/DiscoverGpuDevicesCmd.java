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
package org.apache.cloudstack.api.command.admin.gpu;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;


@APICommand(name = "discoverGpuDevices", description = "Discovers available GPU devices on a host",
            responseObject = GpuDeviceResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
            since = "4.21.0", authorized = {RoleType.Admin})
public class DiscoverGpuDevicesCmd extends BaseListCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostResponse.class, required = true,
               description = "ID of the host")
    private Long id;

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation //////////////////
    /// //////////////////////////////////////////////////

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Discovering GPU Devices on host id: " + getId());
        ListResponse<GpuDeviceResponse> response = gpuService.discoverGpuDevices(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

}
