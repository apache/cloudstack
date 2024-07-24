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

package org.apache.cloudstack.api;

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.response.VolumeMetricsStatsResponse;

@APICommand(name = "listVolumesUsageHistory", description = "Lists volume stats", responseObject = VolumeMetricsStatsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18.0",
        authorized = {RoleType.Admin,  RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListVolumesUsageHistoryCmd extends BaseResourceUsageHistoryCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VolumeResponse.class, description = "the ID of the volume.")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType= SystemVmResponse.class, description="the IDs of the volumes, mutually exclusive with id.")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the volume (a substring match is made against the parameter value returning the data for all matching Volumes).")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getIds() {
        return ids;
    }

    public String getName() {
        return name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<VolumeMetricsStatsResponse> response = metricsService.searchForVolumeMetricsStats(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
