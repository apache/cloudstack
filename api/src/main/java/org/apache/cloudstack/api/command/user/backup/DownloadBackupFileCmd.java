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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupManager;

import javax.inject.Inject;

@APICommand(name = "downloadBackupFile",
        description = "Download a file from a backup",
        responseObject = ExtractResponse.class, since = "4.24.0.0")
public class DownloadBackupFileCmd extends BaseAsyncCmd {

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.BACKUP_ID, type = BaseCmd.CommandType.UUID, entityType = BackupResponse.class, required = true,
            description = "ID of the backup to download the file.")
    private Long backupId;

    @ACL
    @Parameter(name = ApiConstants.VOLUME_ID, type = BaseCmd.CommandType.UUID, entityType = VolumeResponse.class, required = true,
            description = "ID of the volume. If not informed, we will return every filesystem of every volume.")
    private Long volumeId;

    @Parameter(name = ApiConstants.FILESYSTEM, type = BaseCmd.CommandType.STRING, required = true,
            description = "Filesystem to list the files in.", validations = {ApiArgValidator.LimitedSpecialCharacters})
    private String filesystem;

    @Parameter(name = ApiConstants.PATH, type = BaseCmd.CommandType.STRING, required = true,
            description = "Path to the file to be downloaded in the backed-up volume.", validations = {ApiArgValidator.LimitedSpecialCharacters})
    private String path;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBackupId() {
        return backupId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public String getFilesystem() {
        return filesystem.trim();
    }

    public String getPath() {
        return path.trim();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation //////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BACKUP_FILE_DOWNLOAD;
    }

    @Override
    public String getEventDescription() {
        Backup backup = _entityMgr.findById(Backup.class, getBackupId());
        if (backup == null) {
            throw new InvalidParameterValueException(String.format("Unable to find backup with ID [%s].", getBackupId()));
        }
        return "Downloading a file from backup " + backup.getUuid();
    }

    @Override
    public long getEntityOwnerId() {
        Backup backup = _entityMgr.findById(Backup.class, getBackupId());
        if (backup != null) {
            return backup.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        ExtractResponse response = backupManager.downloadBackupFile(getBackupId(), getVolumeId(), getFilesystem(), getPath());

        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }
}
