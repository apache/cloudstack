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
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupRepository;
import org.apache.cloudstack.backup.BackupRepositoryService;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "addBackupRepository",
        description = "Adds a backup repository to store NAS backups",
        responseObject = BackupRepositoryResponse.class, since = "4.20.0",
        authorized = {RoleType.Admin})
public class AddBackupRepositoryCmd extends BaseCmd {

    @Inject
    private BackupRepositoryService backupRepositoryService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the backup repository")
    private String name;

    @Parameter(name = ApiConstants.ADDRESS, type = CommandType.STRING, required = true, description = "address of the backup repository")
    private String address;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "type of the backup repository storage. Supported values: nfs, cephfs, cifs")
    private String type;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "backup repository provider")
    private String provider;

    @Parameter(name = ApiConstants.MOUNT_OPTIONS, type = CommandType.STRING, description = "shared storage mount options")
    private String mountOptions;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "ID of the zone where the backup repository is to be added")
    private Long zoneId;

    @Parameter(name = ApiConstants.CAPACITY_BYTES, type = CommandType.LONG, description = "capacity of this backup repository")
    private Long capacityBytes;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public BackupRepositoryService getBackupRepositoryService() {
        return backupRepositoryService;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        if ("cephfs".equalsIgnoreCase(type)) {
            return "ceph";
        }
        return type.toLowerCase();
    }

    public String getAddress() {
        return address;
    }

    public String getProvider() {
        return provider;
    }

    public String getMountOptions() {
        return mountOptions == null ? "" : mountOptions;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            BackupRepository result = backupRepositoryService.addBackupRepository(this);
            if (result != null) {
                BackupRepositoryResponse response = _responseGenerator.createBackupRepositoryResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add backup repository");
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
