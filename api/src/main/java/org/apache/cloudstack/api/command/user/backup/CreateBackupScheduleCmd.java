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

package org.apache.cloudstack.api.command.user.backup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupSchedule;
import org.apache.cloudstack.context.CallContext;

import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createBackupSchedule",
        description = "Creates a user-defined VM backup schedule",
        responseObject = BackupResponse.class, since = "4.14.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateBackupScheduleCmd extends BaseCmd {

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "ID of the VM for which schedule is to be defined")
    private Long vmId;

    @Parameter(name = ApiConstants.INTERVAL_TYPE,
            type = CommandType.STRING,
            required = true,
            description = "valid values are HOURLY, DAILY, WEEKLY, and MONTHLY")
    private String intervalType;

    @Parameter(name = ApiConstants.SCHEDULE,
            type = CommandType.STRING,
            required = true,
            description = "custom backup schedule, the format is:"
            + "for HOURLY MM*, for DAILY MM:HH*, for WEEKLY MM:HH:DD (1-7)*, for MONTHLY MM:HH:DD (1-28)")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE,
            type = CommandType.STRING,
            required = true,
            description = "Specifies a timezone for this command. For more information on the timezone parameter, see TimeZone Format.")
    private String timezone;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    public DateUtil.IntervalType getIntervalType() {
        return DateUtil.IntervalType.getIntervalType(intervalType);
    }

    public String getSchedule() {
        return schedule;
    }

    public String getTimezone() {
        return timezone;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        try {
            BackupSchedule schedule = backupManager.configureBackupSchedule(this);
            if (schedule != null) {
                BackupScheduleResponse response = _responseGenerator.createBackupScheduleResponse(schedule);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new CloudRuntimeException("Error while creating backup schedule of VM");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
