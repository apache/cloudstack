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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.gpu.GpuCard;


@APICommand(name = "createGpuCard", description = "Creates a GPU card definition in the system",
        responseObject = GpuCardResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.21.0")
public class CreateGpuCardCmd extends BaseCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DEVICE_ID, type = CommandType.STRING, required = true,
            description = "the device ID of the GPU card")
    private String deviceId;

    @Parameter(name = ApiConstants.DEVICE_NAME, type = CommandType.STRING, required = true,
            description = "the device name of the GPU card")
    private String deviceName;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "the display name of the GPU card")
    private String name;

    @Parameter(name = ApiConstants.VENDOR_NAME, type = CommandType.STRING, required = true,
            description = "the vendor name of the GPU card")
    private String vendorName;

    @Parameter(name = ApiConstants.VENDOR_ID, type = CommandType.STRING, required = true,
            description = "the vendor ID of the GPU card")
    private String vendorId;

    // Optional parameters for the passthrough vGPU profile display properties
    @Parameter(name = ApiConstants.VIDEORAM, type = CommandType.LONG,
            description = "the video RAM size in MB for the passthrough vGPU profile")
    private Long videoRam;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getName() {
        return name;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorId() {
        return vendorId;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            GpuCard gpuCard = gpuService.createGpuCard(this);
            if (gpuCard != null) {
                GpuCardResponse response = new GpuCardResponse(gpuCard);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create GPU card");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create GPU card: " + e.getMessage());
        }
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
