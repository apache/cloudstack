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
package org.apache.cloudstack.api.command.user.affinitygroup;

import org.apache.log4j.Logger;

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

@APICommand(name = "listAffinityGroups", description = "Lists affinity groups", responseObject = AffinityGroupResponse.class)
public class ListAffinityGroupsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListAffinityGroupsCmd.class.getName());

    private static final String s_name = "listaffinitygroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists affinity groups by name")
    private String affinityGroupName;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               description = "lists affinity groups by virtual machine id",
               entityType = UserVmResponse.class)
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "list the affinity group by the id provided", entityType = AffinityGroupResponse.class)
    private Long id;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "lists affinity groups by type")
    private String affinityGroupType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getAffinityGroupName() {
        return affinityGroupName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {

        ListResponse<AffinityGroupResponse> response =
            _queryService.listAffinityGroups(id, affinityGroupName, affinityGroupType, virtualMachineId, this.getAccountName(), this.getDomainId(), this.isRecursive(),
                this.listAll(), this.getStartIndex(), this.getPageSizeVal());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AffinityGroup;
    }
}
