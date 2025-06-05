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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.gpu.ListGpuDevicesCmd;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;


@APICommand(name = "listGpuDevices", description = "Lists all available GPU devices",
        responseView = ResponseObject.ResponseView.Full,
        responseObject = GpuDeviceResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.21.0")
public class ListGpuDevicesCmdByAdmin extends ListGpuDevicesCmd implements AdminCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuDeviceResponse.class,
            description = "ID of the GPU device")
    private Long id;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "the host ID where the GPU device is attached")
    private Long hostId;

    @Parameter(name = ApiConstants.GPU_CARD_ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
            description = "the GPU card ID associated with the GPU device")
    private Long gpuCardId;

    @Parameter(name = ApiConstants.VGPU_PROFILE_ID, type = CommandType.UUID, entityType = VgpuProfileResponse.class,
            description = "the vGPU profile ID assigned to the GPU device")
    private Long vgpuProfileId;


    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getGpuCardId() {
        return gpuCardId;
    }

    public Long getVgpuProfileId() {
        return vgpuProfileId;
    }

}
