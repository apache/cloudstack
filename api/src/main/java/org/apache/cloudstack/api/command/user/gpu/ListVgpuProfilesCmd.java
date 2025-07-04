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
import org.apache.cloudstack.api.response.VgpuProfileResponse;

@APICommand(name = "listVgpuProfiles", description = "Lists all available vGPU profiles",
        responseObject = VgpuProfileResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.21.0")
public class ListVgpuProfilesCmd extends BaseListCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VgpuProfileResponse.class,
            description = "ID of the vGPU profile")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the vGPU profile")
    private String name;

    @Parameter(name = ApiConstants.GPU_CARD_ID, type = CommandType.UUID, entityType = GpuCardResponse.class,
            description = "the GPU card ID associated with this GPU device")
    private Long cardId;

    @Parameter(name = ApiConstants.ACTIVE_ONLY, type = CommandType.BOOLEAN,
            description = "If true, only vGPU profiles which have a device will be listed. If false, all vGPU profiles will be listed.")
    private Boolean activeOnly;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getCardId() {
        return cardId;
    }

    public boolean getActiveOnly() {
        return Boolean.TRUE.equals(activeOnly);
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override public void execute() {
        ListResponse<VgpuProfileResponse> response = gpuService.listVgpuProfiles(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
