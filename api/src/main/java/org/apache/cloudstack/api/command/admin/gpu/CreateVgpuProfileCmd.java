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
import org.apache.cloudstack.api.response.VgpuProfileResponse;


@APICommand(name = "createVgpuProfile", description = "Creates a vGPU profile in the system",
            responseObject = VgpuProfileResponse.class, requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false, since = "4.21.0", authorized = {RoleType.Admin})
public class CreateVgpuProfileCmd extends BaseCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
               description = "the name of the vGPU profile")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING,
               description = "the description of the vGPU profile")
    private String description;

    @Parameter(name = ApiConstants.GPU_CARD_ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
               required = true, description = "the GPU card ID associated with this GPU device")
    private Long cardId;

    @Parameter(name = ApiConstants.MAX_VGPU_PER_PHYSICAL_GPU, type = CommandType.LONG,
               description = "Max vGPU per physical GPU. This is used to calculate capacity.")
    private Long maxVgpuPerPgpu;

    @Parameter(name = ApiConstants.VIDEORAM, type = CommandType.LONG,
               description = "the video RAM size in MB")
    private Long videoRam;

    @Parameter(name = ApiConstants.MAXHEADS, type = CommandType.LONG,
               description = "the maximum number of display heads")
    private Long maxHeads;

    @Parameter(name = ApiConstants.MAXRESOLUTIONX, type = CommandType.LONG,
               description = "the maximum X resolution")
    private Long maxResolutionX;

    @Parameter(name = ApiConstants.MAXRESOLUTIONY, type = CommandType.LONG,
               description = "the maximum Y resolution")
    private Long maxResolutionY;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCardId() {
        return cardId;
    }

    public Long getMaxVgpuPerPgpu() {
        return maxVgpuPerPgpu;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public Long getMaxHeads() {
        return maxHeads;
    }

    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    @Override
    public void execute() {
        try {
            VgpuProfileResponse response = gpuService.createVgpuProfile(this);
            if (response != null) {
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vGPU profile");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to create vGPU profile: " + e.getMessage());
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
