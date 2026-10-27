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

import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.gpu.GpuCard;


@APICommand(name = "updateGpuCard", description = "Updates a GPU card definition in the system",
            responseObject = GpuCardResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
            since = "4.21.0", authorized = {RoleType.Admin})
public class UpdateGpuCardCmd extends BaseCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuCardResponse.class, required = true,
               description = "the ID of the GPU card")
    private Long id;

    @Parameter(name = ApiConstants.DEVICE_NAME, type = CommandType.STRING,
               description = "the device name of the GPU card")
    private String deviceName;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the display name of the GPU card")
    private String name;

    @Parameter(name = ApiConstants.VENDOR_NAME, type = CommandType.STRING,
               description = "the vendor name of the GPU card")
    private String vendorName;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
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

    @Override
    public void execute() {
        try {
            GpuCard gpuCard = gpuService.updateGpuCard(this);
            if (gpuCard != null) {
                GpuCardResponse response = new GpuCardResponse(gpuCard);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update GPU card");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update GPU card: " + e.getMessage());
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
