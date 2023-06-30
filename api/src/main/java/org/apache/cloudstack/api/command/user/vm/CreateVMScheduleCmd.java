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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.vm.schedule.VMScheduleManager;

import javax.inject.Inject;
import java.util.Date;

@APICommand(name = "createVMSchedule", description = "Create VM Schedule", responseObject = VMScheduleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateVMScheduleCmd extends BaseCmd {

    @Inject
    VMScheduleManager vmScheduleManager;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "ID of the VM for which schedule is to be defined")
    private Long vmId;

    @Parameter(name = ApiConstants.DESCRIPTION,
            type = CommandType.STRING,
            required = false,
            description = "Description of the schedule")
    private String description;

    @Parameter(name = ApiConstants.SCHEDULE,
            type = CommandType.STRING,
            required = true,
            description = "Schedule for action on VM in cron format. e.g. '0 15 10 * *' for 'at 15:00 on 10th day of every month'")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE,
            type = CommandType.STRING,
            required = true,
            description = "Specifies a timezone for this command. For more information on the timezone parameter, see TimeZone Format.")
    private String timeZone;

    @Parameter(name = ApiConstants.ACTION,
            type = CommandType.STRING,
            required = true,
            description = "Action to take on the VM (start/stop/restart/force_stop/force_reboot).")
    private String action;

    @Parameter(name = ApiConstants.START_DATE,
            type = CommandType.DATE,
            required = false,
            description = "start date from which the schedule becomes active. Defaults to current date plus 1 minute."
                    + "Use format \"yyyy-MM-dd hh:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE,
            type = CommandType.DATE,
            required = false,
            description = "end date after which the schedule becomes inactive"
                    + "Use format \"yyyy-MM-dd hh:mm:ss\")")
    private Date endDate;

    @Parameter(name = ApiConstants.ENABLED,
            type = CommandType.BOOLEAN,
            required = false,
            description = "Enable VM schedule. Defaults to true")
    private Boolean enabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getAction() {
        return action;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Boolean getEnabled() {
        if (enabled == null) {
            enabled = true;
        }
        return enabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        VMScheduleResponse response = vmScheduleManager.createSchedule(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, getVmId());
        if (vm == null) {
            throw new InvalidParameterValueException(String.format("Unable to find VM by id=%d", getVmId()));
        }
        return vm.getAccountId();
    }
}
