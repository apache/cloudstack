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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GpuOfferingResponse;
import org.apache.cloudstack.api.response.VgpuProfileResponse;
import org.apache.cloudstack.gpu.GpuService;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "createGpuOffering", description = "Creates a GPU offering",
        responseObject = GpuOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.21.0",
        authorized = {RoleType.Admin})
public class CreateGpuOfferingCmd extends BaseCmd {

    @Inject
    private GpuService gpuService;

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "the name of the GPU offering")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING,
            description = "the description of the GPU offering")
    private String description;

    @Parameter(name = ApiConstants.VGPU_PROFILE_IDS, type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = VgpuProfileResponse.class,
            description = "the list of vGPU profile IDs to include in the offering")
    private List<Long> vgpuProfileIds;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Long> getVgpuProfileIds() {
        return vgpuProfileIds;
    }

    @Override
    public void execute() {
        try {
            GpuOfferingResponse response = gpuService.createGpuOffering(this);
            if (response != null) {
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        "Failed to create GPU offering");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to create GPU offering: " + e.getMessage());
        }
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GpuOffering;
    }
}
