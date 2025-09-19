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
import org.apache.cloudstack.api.response.GpuDeviceResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.gpu.GpuDevice;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;


@APICommand(name = "updateGpuDevice", description = "Updates an existing GPU device",
            responseObject = GpuDeviceResponse.class, since = "4.21.0", requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class UpdateGpuDeviceCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuDeviceResponse.class, required = true,
               description = "ID of the GPU device to update")
    private Long id;

    @Parameter(name = ApiConstants.GPU_CARD_ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
               description = "New GPU card ID")
    private Long gpuCardId;

    @Parameter(name = ApiConstants.VGPU_PROFILE_ID, type = CommandType.UUID, entityType = VgpuProfileResponse.class,
               description = "New vGPU profile ID")
    private Long vgpuProfileId;

    @Parameter(name = "type", type = CommandType.STRING, description = "New type of GPU device (PCI, MDEV, VGPUOnly)")
    private String type;

    @Parameter(name = "parentgpudeviceid", type = CommandType.UUID, entityType = GpuDeviceResponse.class,
               description = "New parent GPU device ID (for virtual GPU devices)")
    private Long parentGpuDeviceId;

    @Parameter(name = ApiConstants.NUMA_NODE, type = CommandType.STRING,
               description = "New NUMA node of the GPU device")
    private String numaNode;

    public Long getId() {
        return id;
    }

    public Long getGpuCardId() {
        return gpuCardId;
    }

    public Long getVgpuProfileId() {
        return vgpuProfileId;
    }

    public GpuDevice.DeviceType getType() {
        GpuDevice.DeviceType deviceType = null;
        if (StringUtils.isNotBlank(type)) {
            deviceType = EnumUtils.getEnumIgnoreCase(GpuDevice.DeviceType.class, type);
            if (deviceType == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid GPU device type: " + type);
            }
        }
        return deviceType;
    }

    public Long getParentGpuDeviceId() {
        return parentGpuDeviceId;
    }

    public String getNumaNode() {
        return numaNode;
    }

    @Override
    public void execute() {
        try {
            GpuDeviceResponse response = gpuService.updateGpuDevice(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
