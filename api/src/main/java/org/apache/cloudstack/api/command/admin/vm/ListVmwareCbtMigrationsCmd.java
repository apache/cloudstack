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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.vm.VmwareCbtMigrationManager;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "listVmwareCbtMigrations",
        description = "List VMware CBT based migration sessions",
        responseObject = VmwareCbtMigrationResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22.1")
public class ListVmwareCbtMigrationsCmd extends BaseListCmd {

    @Inject
    public VmwareCbtMigrationManager vmwareCbtMigrationManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VmwareCbtMigrationResponse.class,
            description = "the VMware CBT migration ID")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "the destination zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            description = "the account ID")
    private Long accountId;

    @Parameter(name = ApiConstants.VCENTER, type = CommandType.STRING,
            description = "the source VMware vCenter")
    private String vcenter;

    @Parameter(name = ApiConstants.SOURCE_VM_NAME, type = CommandType.STRING,
            description = "the source VMware VM name")
    private String sourceVmName;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING,
            description = "the migration state")
    private String state;

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public String getState() {
        return state;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ListResponse<VmwareCbtMigrationResponse> response = vmwareCbtMigrationManager.listVmwareCbtMigrations(this);
        response.setResponseName(getCommandName());
        response.setObjectName("vmwarecbtmigration");
        setResponseObject(response);
    }
}
