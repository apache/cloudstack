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

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.vm.schedule.VMSchedule;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.log4j.Logger;

@APICommand(name = UpdateVMScheduleCmd.APINAME,
        description = "Update Schedule for a VM",
        responseObject = VMScheduleResponse.class,
        entityType = {VMSchedule.class},
        since = "4.19.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateVMScheduleCmd extends BaseAsyncCmd {
    public static final String APINAME = "updateVMSchedule";
    public static final Logger LOGGER = Logger.getLogger(UpdateVMScheduleCmd.class);

    @Parameter(name = ApiConstants.VM_SCHEDULE_ID, type = CommandType.UUID, entityType = VMScheduleResponse.class, description = "The ID of the VM schedule")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = false, description = "The description of the VM schedule")
    private String description;

    @Parameter(name = ApiConstants.VM_SCHEDULE_ACTION, type = CommandType.STRING, required = false, description = "The action of VM Schedule")
    private String action;

    @Parameter(name = ApiConstants.SCHEDULE, type = CommandType.STRING, required = false, description = "The schedule of VM")
    private String schedule;

    @Parameter(name = ApiConstants.INTERVAL_TYPE, type = CommandType.STRING, required = false, description = "valid values are HOURLY, DAILY, WEEKLY, and MONTHLY")
    private String intervalType;

    @Parameter(name = ApiConstants.VM_SCHEDULE_TAG, type = CommandType.STRING, required = false, description = "The tag of VM Schedule")
    private String tag;

    @Parameter(name = ApiConstants.VM_SCHEDULE_TIMEZONE, type = CommandType.STRING, required = false, description = "The timezone of VM Schedule")
    private String timezone;

    public String getTimezone() {
        return timezone;
    }

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("vmschedule id: " + this._uuidMgr.getUuid(VMSchedule.class, getId()));
        VMSchedule result = vmScheduleManager.updateVMSchedule(this);
        if (result != null) {
            VMScheduleResponse response = _responseGenerator.createVMScheduleResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update schedule");
        }

    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }


    @Override
    public String getEventType() {
        return EventTypes.EVENT_VMSCHEDULE_UPDATE;
    }


    @Override
    public String getEventDescription() {
        return "updating vm schedule";
    }
}
