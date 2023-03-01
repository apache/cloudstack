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
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.schedule.VMSchedule;
import org.apache.cloudstack.acl.RoleType;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

@APICommand(name = CreateVMScheduleCmd.APINAME,
        description = "Creates Schedule for a VM",
        responseObject = VMScheduleResponse.class,
        since = "4.19.0",
        entityType = {VMSchedule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateVMScheduleCmd extends BaseAsyncCreateCmd {
    public static final String APINAME = "createVMSchedule";
    public static final Logger s_logger = Logger.getLogger(CreateVMScheduleCmd.class);

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, required = true, entityType = UserVmResponse.class, description = "The ID of the vm")
    private Long vmId;

    @Parameter(name = ApiConstants.VM_SCHEDULE_DESCRIPTION, type = CommandType.STRING, required = false, description = "The description of the VM schedule")
    private String description;

    @Parameter(name = ApiConstants.VM_SCHEDULE_ACTION, type = CommandType.STRING, required = true, description = "The action of VM Schedule")
    private String action;

    @Parameter(name = ApiConstants.VM_SCHEDULE_PERIOD, type = CommandType.STRING, required = true, description = "The period of VM Schedule")
    private String period;

    @Parameter(name = ApiConstants.VM_SCHEDULE_TAG, type = CommandType.STRING, required = false, description = "The tag of VM Schedule")
    private String tag;

    @Parameter(name = ApiConstants.VM_SCHEDULE_TIMEZONE, type = CommandType.STRING, required = false, description = "The timezone of VM Schedule")
    private String timezone;

    public String getAction() {
        return action;
    }

    public String getPeriod() {
        return period;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void create() throws ResourceAllocationException {

        VMSchedule vmSchedule;
        try {
            vmSchedule = vmScheduleManager.createVMSchedule(getVmId(), getDescription(), getAction(), getPeriod(), getTag(), getTimezone());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm schedule: " + e.getMessage(), e);
        }

        if (vmSchedule != null) {
            setEntityId(vmSchedule.getId());
            setEntityUuid(vmSchedule.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm schedule");
        }
    }

    @Override
    public void execute()  {
        CallContext.current().setEventDetails("VM Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId()));
        VMSchedule result = vmScheduleManager.findVMSchedule(getEntityId());
        if (result != null) {
            VMScheduleResponse response = _responseGenerator.createVMScheduleResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm schedule due to an internal error creating schedule for vm " + getVmId());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VMSCHEDULE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating vm schedule";
    }

}
