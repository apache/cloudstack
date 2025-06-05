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
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.gpu.GpuDevice;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;


@APICommand(name = "createGpuDevice", description = "Creates a GPU device manually on a host",
            responseObject = GpuDeviceResponse.class, since = "4.21.0", requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class CreateGpuDeviceCmd extends BaseCmd {

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, required = true,
               description = "ID of the host where the GPU device is located")
    private Long hostId;

    @Parameter(name = ApiConstants.BUS_ADDRESS, type = CommandType.STRING, required = true,
               description = "PCI bus address of the GPU device (e.g., 0000:01:00.0) or UUID for MDEV devices.")
    private String busAddress;

    @Parameter(name = ApiConstants.GPU_CARD_ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
               required = true, description = "ID of the GPU card type")
    private Long gpuCardId;

    @Parameter(name = ApiConstants.VGPU_PROFILE_ID, type = CommandType.UUID, entityType = VgpuProfileResponse.class,
               required = true, description = "ID of the vGPU profile")
    private Long vgpuProfileId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING,
               description = "Type of GPU device (PCI, MDEV, VGPUOnly). Defaults to PCI.")
    private String type;

    @Parameter(name = ApiConstants.PARENT_GPU_DEVICE_ID, type = CommandType.UUID, entityType = GpuDeviceResponse.class,
               description = "ID of the parent GPU device (for virtual GPU devices)")
    private Long parentGpuDeviceId;

    @Parameter(name = ApiConstants.NUMA_NODE, type = CommandType.STRING,
            description = "NUMA node of the GPU device (e.g., 0, 1, etc.). This is optional and can be used to "
                          + "specify the NUMA node for the GPU device which is used during allocation. Defaults to -1")
    private String numaNode;

    public Long getHostId() {
        return hostId;
    }

    public String getBusAddress() {
        return busAddress;
    }

    public Long getGpuCardId() {
        return gpuCardId;
    }

    public Long getVgpuProfileId() {
        return vgpuProfileId;
    }

    public GpuDevice.DeviceType getType() {
        GpuDevice.DeviceType deviceType = GpuDevice.DeviceType.PCI;
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
        if (StringUtils.isBlank(numaNode)) {
            return "-1"; // Default value for NUMA node
        }
        return numaNode;
    }

    @Override
    public void execute() {
        try {
            GpuDeviceResponse response = gpuService.createGpuDevice(this);
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
