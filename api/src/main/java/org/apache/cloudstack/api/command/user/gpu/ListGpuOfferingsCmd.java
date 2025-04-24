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

import com.cloud.utils.StringUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GpuOfferingResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.gpu.GpuOffering;
import org.apache.cloudstack.gpu.GpuService;
import org.apache.commons.lang3.EnumUtils;

import javax.inject.Inject;

import static org.apache.cloudstack.gpu.GpuOffering.State.Active;

@APICommand(name = "listGpuOfferings", description = "Lists all available GPU offerings",
        responseObject = GpuOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.21.0")
public class ListGpuOfferingsCmd extends BaseListCmd {

    @Inject
    private GpuService gpuService;

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuOfferingResponse.class,
            description = "ID of the GPU offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the GPU offering")
    private String name;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING,
            description = "Filter by state of the gpu offering. Defaults to 'Active'. If set to 'all' shows both Active & Inactive offerings.")
    private String state;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GpuOffering.State getState() {
        if (StringUtils.isBlank(state)) {
            return Active;
        }
        GpuOffering.State gpuOfferingState = EnumUtils.getEnumIgnoreCase(GpuOffering.State.class, this.state);
        if (!this.state.equalsIgnoreCase("all") && gpuOfferingState == null) {
            throw new IllegalArgumentException("Invalid state value: " + this.state);
        }
        return gpuOfferingState;
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<GpuOfferingResponse> response = gpuService.listGpuOfferings(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
