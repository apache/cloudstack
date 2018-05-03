/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.framework.backuprecovery.api;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.backuprecovery.BackupRecoveryManager;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderResponse;

import javax.inject.Inject;

@APICommand(name = AssignBackupPolicyCmd.APINAME,
        description = "Assigns a VM to an existing backup policy",
        responseObject = SuccessResponse.class, since = "4.12.0")
public class AssignBackupPolicyCmd extends BaseCmd {

    public static final String APINAME = "assignBackupPolicy";

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "id of the VM to be moved")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.BACKUP_POLICY_ID,
            type = CommandType.STRING,
            entityType = BackupRecoveryProviderResponse.class,
            required = true,
            description = "Backup Recovery Provider ID")
    private String policyId;

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getPolicyId() {
        return policyId;
    }

    @Inject
    BackupRecoveryManager backupRecoveryManager;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            boolean result = backupRecoveryManager.assignVMToBackupPolicy(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign VM to backup policy");
            }
        } catch (Exception e) {

        }
    }

    @Override
    public String getCommandName() {
        return AssignBackupPolicyCmd.APINAME + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
