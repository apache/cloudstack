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

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmwareCbtMigrationManager;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "syncVmwareCbtMigration",
        description = "Run a VMware CBT delta synchronization cycle",
        responseObject = VmwareCbtMigrationResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22.1")
public class SyncVmwareCbtMigrationCmd extends BaseAsyncCmd {

    @Inject
    public VmwareCbtMigrationManager vmwareCbtMigrationManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VmwareCbtMigrationResponse.class,
            required = true, description = "the VMware CBT migration ID")
    private Long id;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING,
            description = "optional username override for the source vCenter. Stored external vCenter credentials are used when available")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING,
            description = "optional password override for the source vCenter. Stored external vCenter credentials are used when available")
    private String password;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VMWARE_CBT_MIGRATION_SYNC;
    }

    @Override
    public String getEventDescription() {
        return String.format("Synchronizing VMware CBT migration: %s", id);
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.migrationSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        VmwareCbtMigrationResponse response = vmwareCbtMigrationManager.syncVmwareCbtMigration(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        return account == null ? Account.ACCOUNT_ID_SYSTEM : account.getId();
    }
}
