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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.ListResponse;

@APICommand(name = "listGpuCards", description = "Lists all available GPU cards",
        responseObject = GpuCardResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.21.0")
public class ListGpuCardsCmd extends BaseListCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
            description = "ID of the GPU card")
    private Long id;

    @Parameter(name = ApiConstants.VENDOR_NAME, type = CommandType.STRING,
            description = "vendor name of the GPU card")
    private String vendorName;

    @Parameter(name = ApiConstants.VENDOR_ID, type = CommandType.STRING,
            description = "vendor ID of the GPU card")
    private String vendorId;

    @Parameter(name = ApiConstants.DEVICE_ID, type = CommandType.STRING,
            description = "device ID of the GPU card")
    private String deviceId;

    @Parameter(name = ApiConstants.DEVICE_NAME, type = CommandType.STRING,
            description = "device name of the GPU card")
    private String deviceName;

    @Parameter(name = ApiConstants.ACTIVE_ONLY, type = CommandType.BOOLEAN,
            description = "If true, only GPU cards which have a device will be listed. If false, all GPU cards will be listed.")
    private Boolean activeOnly;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean getActiveOnly() {
        return Boolean.TRUE.equals(activeOnly);
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override public void execute() {
        ListResponse<GpuCardResponse> response = gpuService.listGpuCards(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
