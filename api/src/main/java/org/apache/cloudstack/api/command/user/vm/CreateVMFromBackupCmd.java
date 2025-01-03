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
package org.apache.cloudstack.api.command.user.vm;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.backup.BackupManager;

import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "createVMFromBackup",
        description = "Creates and automatically starts a VM from a backup.",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        since = "4.21.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})

public class CreateVMFromBackupCmd extends DeployVMCmd implements UserCmd {

    @Inject
    BackupManager backupManager;

    @Parameter(name = ApiConstants.BACKUP_ID,
            type = CommandType.UUID,
            entityType = BackupResponse.class,
            required = true,
            description = "backup ID to create the VM from")
    private Long backupId;

    @Parameter(name = ApiConstants.DISK_OFFERING_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            description = "list of disk offering ids to be used by the data volumes of the vm.")
    private List<Long> diskOfferingIds;

    @Parameter(name = ApiConstants.DISK_SIZES,
            type = CommandType.LIST,
            collectionType = CommandType.LONG,
            description = "list of volume sizes to be used by the data volumes of the vm for custom disk offering.")
    private List<Long> diskSizes;

    @Parameter(name = ApiConstants.MIN_IOPS,
            type = CommandType.LIST,
            collectionType = CommandType.LONG,
            description = "list of minIops to be used by the data volumes of the vm for custom disk offering.")
    private List<Long> minIops;

    @Parameter(name = ApiConstants.MAX_IOPS,
            type = CommandType.LIST,
            collectionType = CommandType.LONG,
            description = "list of maxIops to be used by the data volumes of the vm for custom disk offering.")
    private List<Long> maxIops;

    public Long getBackupId() {
        return backupId;
    }

    public List<Long> getDiskOfferingIds() {
        return diskOfferingIds;
    }

    public List<Long> getDiskSizes() {
        return diskSizes;
    }

    public List<Long> getMinIops() {
        return minIops;
    }

    public List<Long> getMaxIops() {
        return maxIops;
    }

    @Override
    public void create() {
        UserVm vm;
        try {
            vm = _userVmService.allocateVMFromBackup(this);
            if (vm != null) {
                setEntityId(vm.getId());
                setEntityUuid(vm.getUuid());
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm due to exception: " + e.getMessage());
        }
    }

    @Override
    public void execute () {
        UserVm vm = null;
        try {
            vm = _userVmService.restoreVMFromBackup(this);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm due to exception: " + e.getMessage());
        }
        if (vm != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", vm).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm uuid:"+getEntityUuid());
        }
    }
}
