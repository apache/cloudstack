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
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupPolicyResponse;
import org.apache.cloudstack.api.response.VMBackupResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.backup.VMBackup;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = CreateVMBackupCmd.APINAME,
        description = "Create VM backup",
        responseObject = VMBackupResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateVMBackupCmd extends BaseAsyncCmd {
    public static final String APINAME = "createVMBackup";

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "Name of the VM Backup")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION,
            type = CommandType.STRING,
            description = "The description of the snapshot")
    private String description;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "id of the VM")
    private Long vmId;

    @Parameter(name = ApiConstants.POLICY_ID,
            type = CommandType.UUID,
            entityType = BackupPolicyResponse.class,
            required = true,
            description = "id of the backup policy")
    private Long policyId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            VMBackup backup = backupManager.createBackup(name, description, vmId, policyId);
            if (backup != null) {
                VMBackupResponse response = _responseGenerator.createBackupResponse(backup);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new CloudRuntimeException("Error while creating backup of VM");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_BACKUP_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating backup for VM " + vmId;
    }
}
