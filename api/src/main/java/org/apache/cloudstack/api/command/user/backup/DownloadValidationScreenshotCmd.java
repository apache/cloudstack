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

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.NativeBackupService;

import javax.inject.Inject;

@APICommand(name = "downloadValidationScreenshot", description = "Download validation screenshot of given backup.",
        responseObject = ExtractResponse.class, since = "4.20.0.10-scclouds", requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class DownloadValidationScreenshotCmd extends BaseAsyncCmd {

    @Inject
    private NativeBackupService nativeBackupService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.BACKUP_ID, type = CommandType.UUID, entityType = BackupResponse.class, required = true,
            description = "Id of the backup.")
    private Long backupId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBackupId() {
        return backupId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SCREENSHOT_DOWNLOAD;
    }

    @Override
    public String getEventDescription() {
        return "Downloading validation screenshot of backup " + getBackupId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        ExtractResponse response = nativeBackupService.downloadScreenshot(getBackupId());
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Backup backup = _entityMgr.findById(Backup.class, getBackupId());
        if (backup != null) {
            return backup.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }
}
