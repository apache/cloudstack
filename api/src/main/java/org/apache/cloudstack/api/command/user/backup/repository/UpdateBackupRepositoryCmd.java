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

package org.apache.cloudstack.api.command.user.backup.repository;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupRepositoryResponse;
import org.apache.cloudstack.backup.BackupRepository;
import org.apache.cloudstack.backup.BackupRepositoryService;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "updateBackupRepository",
        description = "Update a backup repository",
        responseObject = BackupRepositoryResponse.class, since = "4.22.0",
        authorized = {RoleType.Admin})
public class UpdateBackupRepositoryCmd extends BaseCmd {

    @Inject
    private BackupRepositoryService backupRepositoryService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = BackupRepositoryResponse.class, description = "ID of the backup repository")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the backup repository")
    private String name;

    @Parameter(name = ApiConstants.ADDRESS, type = CommandType.STRING, description = "address of the backup repository")
    private String address;

    @Parameter(name = ApiConstants.MOUNT_OPTIONS, type = CommandType.STRING, description = "shared storage mount options")
    private String mountOptions;

    @Parameter(name = ApiConstants.CROSS_ZONE_INSTANCE_CREATION, type = CommandType.BOOLEAN, description = "backups in this repository can be used to create Instances on all Zones")
    private Boolean crossZoneInstanceCreation;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public BackupRepositoryService getBackupRepositoryService() {
        return backupRepositoryService;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getMountOptions() {
        return mountOptions == null ? "" : mountOptions;
    }

    public Boolean crossZoneInstanceCreationEnabled() {
        return crossZoneInstanceCreation;
    }

    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            BackupRepository result = backupRepositoryService.updateBackupRepository(this);
            if (result != null) {
                BackupRepositoryResponse response = _responseGenerator.createBackupRepositoryResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update the backup repository");
            }
        } catch (Exception ex4) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex4.getMessage());
        }

    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
