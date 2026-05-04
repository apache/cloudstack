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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupServiceJobResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

@APICommand(name = "listBackupServiceJobs", description = "List backup service jobs", responseObject = BackupServiceJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin}, since = "4.23.0")
public class ListBackupServiceJobsCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, entityType = BackupServiceJobResponse.class, description = "List only job with given ID.")
    private Long id;

    @Parameter(name = ApiConstants.BACKUP_ID, type = CommandType.UUID, entityType = BackupResponse.class, description = "List jobs for the given backup.")
    private Long backupId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "List jobs in the given host, implies executing.")
    private Long hostId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "List jobs in the given zone.")
    private Long zoneId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "List jobs with the given type. Accepted values are StartCompression, FinalizeCompression and " +
            "BackupValidation.")
    private String type;

    @Parameter(name = ApiConstants.EXECUTING, type = CommandType.BOOLEAN, description = "List executing jobs.")
    private Boolean executing;

    @Parameter(name = ApiConstants.SCHEDULED, type = CommandType.BOOLEAN, description = "List scheduled jobs.")
    private Boolean scheduled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getBackupId() {
        return backupId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getType() {
        return type;
    }

    public boolean getExecuting() {
        return Boolean.TRUE.equals(executing);
    }

    public boolean getScheduled() {
        return Boolean.TRUE.equals(scheduled);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        ListResponse<BackupServiceJobResponse> response = _queryService.listBackupServiceJobs(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
