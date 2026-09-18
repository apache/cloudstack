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

import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
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

import javax.inject.Inject;

@APICommand(name = "updateBackupSchedule",
        description = "Updates a User-defined Instance backup schedule",
        responseObject = BackupResponse.class, since = "4.14.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateBackupScheduleCmd extends BaseCmd {

    @Inject
    protected BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = BackupScheduleResponse.class,
            required = false,
            description = "ID of the schedule which should be updated. This parameter takes precedence over the virtualmachineid parameter.")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = false,
            description = "ID of the VM for which schedule is to be defined")
    private Long vmId;

    @Parameter(name = ApiConstants.INTERVAL_TYPE,
            type = CommandType.STRING,
            required = false,
            description = "valid values are HOURLY, DAILY, WEEKLY, and MONTHLY")
    protected String intervalType;

    @Parameter(name = ApiConstants.SCHEDULE,
            type = CommandType.STRING,
            required = false,
            description = "custom backup schedule, the format is:"
                    + "for HOURLY MM*, for DAILY MM:HH*, for WEEKLY MM:HH:DD (1-7)*, for MONTHLY MM:HH:DD (1-28)")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE,
            type = CommandType.STRING,
            required = false,
            description = "Specifies a timezone for this command. For more information on the timezone parameter, see TimeZone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_QUIESCEVM,
            type = CommandType.BOOLEAN,
            description = "Whether the VM's file systems should be frozen for the scheduled backups. Currently only supported for the KNIB backup provider.")
    private Boolean quiesceVm;

    @Parameter(name = ApiConstants.MAX_BACKUPS, type = CommandType.INTEGER,
            description = ApiConstants.PARAMETER_DESCRIPTION_MAX_BACKUPS, since = "4.24.0")
    private Integer maxBackups;

    @Parameter(name = ApiConstants.ISOLATED,
            type = CommandType.BOOLEAN,
            description = ApiConstants.PARAMETER_DESCRIPTION_ISOLATED_BACKUPS,
            since = "4.24.0")
    private Boolean isolated;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

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

    public Boolean isQuiesceVm() {
        return quiesceVm;
    }

    public Integer getMaxBackups() {
        return maxBackups;
    }

    public Boolean isIsolated() {
        return isolated;
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
                throw new CloudRuntimeException("Error while updating backup schedule of VM.");
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
