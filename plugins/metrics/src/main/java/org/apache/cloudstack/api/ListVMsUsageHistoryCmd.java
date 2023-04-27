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
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.response.VmMetricsStatsResponse;

@APICommand(name = "listVirtualMachinesUsageHistory", description = "Lists VM stats", responseObject = VmMetricsStatsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.17",
        authorized = {RoleType.Admin,  RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListVMsUsageHistoryCmd extends BaseResourceUsageHistoryCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "the ID of the virtual machine.")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType=UserVmResponse.class, description="the IDs of the virtual machines, mutually exclusive with id.")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the virtual machine (a substring match is made against the parameter value returning the data for all matching VMs).")
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
        ListResponse<VmMetricsStatsResponse> response = metricsService.searchForVmMetricsStats(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
