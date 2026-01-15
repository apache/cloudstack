//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.api.command.admin.backup;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.backup.IncrementalBackupService;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "listImageTransfers",
        description = "List image transfers for a backup",
        responseObject = ImageTransferResponse.class,
        since = "4.22.0",
        authorized = {RoleType.Admin})
public class ListImageTransfersCmd extends BaseListCmd implements AdminCmd {

    @Inject
    private IncrementalBackupService incrementalBackupService;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = ImageTransferResponse.class,
            description = "ID of the Image Transfer")
    private Long id;

    @Parameter(name = ApiConstants.BACKUP_ID,
            type = CommandType.UUID,
            entityType = BackupResponse.class,
            description = "ID of the backup")
    private Long backupId;

    public Long getId() {
        return id;
    }

    public Long getBackupId() {
        return backupId;
    }

    @Override
    public void execute() {
        List<ImageTransferResponse> responses = incrementalBackupService.listImageTransfers(this);
        ListResponse<ImageTransferResponse> response = new ListResponse<>();
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
