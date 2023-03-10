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
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.log4j.Logger;

@APICommand(name = DisableVMScheduleCmd.APINAME, description = "Updates a VM Schedule", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,since = "4.19.0",
        entityType = {VMSchedule.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DisableVMScheduleCmd extends BaseAsyncCmd {
    public static final String APINAME = "disableVMSchedule";
    public static final Logger s_logger = Logger.getLogger(DisableVMScheduleCmd.class);

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VM_SCHEDULE_ID,
            type = CommandType.UUID,
            entityType = VMScheduleResponse.class,
            required = true,
            description = "The ID of the VM schedule")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("vmschedule id: " + this._uuidMgr.getUuid(VMSchedule.class, getId()));
        boolean result = vmScheduleManager.disableVMSchedule(getId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to disable vm schedule");
        }

    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VMSCHEDULE_DISABLE;
    }

    @Override
    public String getEventDescription() {
        return "Disable VM schedule";
    }
}
