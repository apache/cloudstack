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

import com.cloud.user.Account;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.storage.browser.DataStoreObjectResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "listBackupFilesystems",
        description = "List a backup inner file systems",
        responseObject = DataStoreObjectResponse.class, since = "4.24.0.0")
public class ListBackupFilesystemsCmd extends BaseListCmd {

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.BACKUP_ID, type = CommandType.UUID, entityType = BackupResponse.class, required = true,
            description = "ID of the backup to list the file systems.")
    private Long backupId;

    @ACL
    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class,
            description = "ID of the volume. If not informed, we will return every filesystem of every volume.")
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBackupId() {
        return backupId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

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
        List<DataStoreObjectResponse> responseList = backupManager.listBackupFilesystems(getBackupId(), getVolumeId());

        ListResponse<DataStoreObjectResponse> response = new ListResponse<>();
        response.setResponses(responseList);
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }
}
