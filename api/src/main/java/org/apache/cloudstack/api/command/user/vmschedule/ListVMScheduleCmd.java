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
package org.apache.cloudstack.api.command.user.vmschedule;

import com.cloud.vm.schedule.VMSchedule;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListVMScheduleCmd.APINAME,
        description = "Lists Schedules for a VM",
        responseObject = VMScheduleResponse.class,
        since = "4.19.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListVMScheduleCmd extends BaseListCmd {
    public static final String APINAME = "listVMSchedules";
    public static final Logger s_logger = Logger.getLogger(ListVMScheduleCmd.class);

    @Parameter(name = ApiConstants.VM_SCHEDULE_ID, type = CommandType.UUID, entityType = VMScheduleResponse.class, description = "The ID of the VM schedule")
    private Long id;

    @Parameter(name=ApiConstants.VM_SCHEDULE_IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType=VMScheduleResponse.class, description="the IDs of the vm schedule, mutually exclusive with vmscheduleid", since = "4.9")
    private List<Long> ids;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "state of the vm schedule")
    private String state;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "the ID of the vm")
    private Long vmId;

    public String getState() {
        return state;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        final List<VMSchedule> vmSchedules = vmScheduleManager.listVMSchedules(this);
        final List<VMScheduleResponse> responseList = new ArrayList<>();
        for (final VMSchedule vmSchedule : vmSchedules) {
            VMScheduleResponse response = _responseGenerator.createVMScheduleResponse(vmSchedule);
            responseList.add(response);
        }
        final ListResponse<VMScheduleResponse> VMScheduleResponses = new ListResponse<>();
        VMScheduleResponses.setResponses(responseList);
        VMScheduleResponses.setResponseName(getCommandName());
        setResponseObject(VMScheduleResponses);
    }

    public List<Long> getIds() {
        return ids;
    }

}
