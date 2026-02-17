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

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.backup.IncrementalBackupService;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "finalizeBackup",
        description = "Finalize a VM backup session",
        responseObject = SuccessResponse.class,
        since = "4.22.0",
        authorized = {RoleType.Admin})
public class FinalizeBackupCmd extends BaseCmd implements AdminCmd {

    @Inject
    private IncrementalBackupService incrementalBackupService;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "ID of the VM")
    private Long vmId;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = BackupResponse.class,
            required = true,
            description = "ID of the backup")
    private Long backupId;

    public Long getVmId() {
        return vmId;
    }

    public Long getBackupId() {
        return backupId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public void setBackupId(Long backupId) {
        this.backupId = backupId;
    }

    @Override
    public void execute() {
        boolean result = incrementalBackupService.finalizeBackup(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
