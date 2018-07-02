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
package org.apache.cloudstack.api.command.admin.vm;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = ImportVMCmdByAdmin.APINAME,
        description = "Import VM",
        responseObject = SuccessResponse.class,
        since = "4.12.0",
        authorized = {RoleType.Admin})
public class ImportVMCmdByAdmin extends BaseAsyncCmd {
    public static final String APINAME = "importVM";

    @Inject
    BackupManager backupManager;

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "VM Internal name")
    private String vmInternalName;

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "The zone ID", required = true)
    private Long zoneId;

    @Override
    public String getEventType() {
        return "IMPORT.VM";
    }

    @Override
    public String getEventDescription() {
        return "Importing VM";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        long accountId = CallContext.current().getCallingAccountId();
        long userId = CallContext.current().getCallingUserId();
        long domainId = CallContext.current().getCallingAccount().getDomainId();
        boolean result = backupManager.importVM(zoneId, domainId, accountId, userId, vmInternalName,
                Hypervisor.HypervisorType.VMware, null);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to import VM");
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

    public String getVmInternalName() {
        return vmInternalName;
    }

    public Long getZoneId() {
        return zoneId;
    }
}
