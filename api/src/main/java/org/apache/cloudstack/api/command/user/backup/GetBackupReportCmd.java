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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.BackupReportResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupReportService;

import javax.inject.Inject;
import java.util.Date;

@APICommand(name = "getBackupReport",
        description = "Get the backup report for the given period",
        responseObject = BackupReportResponse.class, since = "4.24.0.0", authorized = {RoleType.Admin})
public class GetBackupReportCmd extends BaseCmd {

    @Inject
    private BackupReportService backupReportService;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "Get backup report by zone ID.")
    private Long zoneId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "Get backup report by domain ID.")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT_ID,
            type = CommandType.UUID,
            entityType = AccountResponse.class,
            description = "Get backup report by account ID.")
    private Long accountId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "Get backup report by project ID.")
    private Long projectId;

    @Parameter(name = ApiConstants.START_DATE,
            type = CommandType.DATE,
            description = "Start date of the report.",
            required = true)
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE,
            type = CommandType.DATE,
            description = "End date of the report.",
            required = true)
    private Date endDate;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
        if (domainId != null && (accountId != null || projectId != null)) {
            throw new InvalidParameterValueException("domainid and accountid or projectid cannot be informed at the same time.");
        }
        return domainId;
    }

    public Long getAccountId() {
        if (projectId != null && accountId != null) {
            throw new InvalidParameterValueException("accountid and projectid cannot be informed at the same time.");
        }
        return accountId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("End date must be after start date.");
        }
        return endDate;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        BackupReportResponse response = backupReportService.getBackupReport(getStartDate(), getEndDate(), getZoneId(), getDomainId(), getAccountId(), getProjectId());

        response.setResponseName(getCommandName());
        response.setObjectName("backupreport");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
