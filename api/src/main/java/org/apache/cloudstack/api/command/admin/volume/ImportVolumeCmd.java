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

package org.apache.cloudstack.api.command.admin.volume;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.volume.VolumeImportUnmanageService;

import javax.inject.Inject;

@APICommand(name = "importVolume",
        description = "Import an unmanaged volume from a storage pool on a host into CloudStack",
        responseObject = VolumeResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.19.1")
public class ImportVolumeCmd extends BaseAsyncCmd {

    @Inject
    public VolumeImportUnmanageService volumeImportService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @Parameter(name = ApiConstants.PATH,
            type = BaseCmd.CommandType.STRING,
            required = true,
            description = "the path of the volume")
    private String path;

    @Parameter(name = ApiConstants.NAME,
            type = BaseCmd.CommandType.STRING,
            description = "the name of the volume. If not set, it will be set to the path of the volume.")
    private String name;

    @Parameter(name = ApiConstants.STORAGE_ID,
            type = BaseCmd.CommandType.UUID,
            required = true,
            entityType = StoragePoolResponse.class,
            description = "the ID of the storage pool")
    private Long storageId;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            description = "the ID of the disk offering linked to the volume")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = BaseCmd.CommandType.STRING,
            description = "an optional account for the volume. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = DomainResponse.class,
            description = "import volume to the domain specified")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "import volume for the project")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public Long getStorageId() {
        return storageId;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_IMPORT;
    }

    @Override
    public String getEventDescription() {
        return String.format("Importing unmanaged Volume with path: %s", path);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        VolumeResponse response = volumeImportService.importVolume(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }
}
