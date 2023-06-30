/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.user.vm;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.vm.schedule.VMSchedule;
import org.apache.cloudstack.vm.schedule.VMScheduleManager;

import javax.inject.Inject;

@APICommand(name = "listVMSchedule", description = "List VM Schedules.", responseObject = VMScheduleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListVMScheduleCmd extends BaseListCmd {
    @Inject
    VMScheduleManager vmScheduleManager;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "ID of the VM for which schedule is to be defined")
    private Long vmId;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = VMScheduleResponse.class,
            required = false,
            description = "ID of VM schedule")
    private Long id;

    @Parameter(name = ApiConstants.ACTION,
            type = CommandType.STRING,
            required = false,
            description = "Action taken by schedule")
    private String action;

    @Parameter(name = ApiConstants.ENABLED,
            type = CommandType.BOOLEAN,
            required = false,
            description = "ID of VM schedule")
    private Boolean enabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        ListResponse<VMScheduleResponse> response = vmScheduleManager.listSchedule(this);
        response.setResponseName(getCommandName());
        response.setObjectName(VMSchedule.class.getSimpleName().toLowerCase());
        setResponseObject(response);
    }
}
