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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeForImportResponse;
import org.apache.cloudstack.storage.volume.VolumeImportUnmanageService;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;

import javax.inject.Inject;

@APICommand(name = "listVolumesForImport",
        description = "Lists unmanaged volumes on a storage pool",
        responseObject = VolumeForImportResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        entityType = {VolumeOnStorageTO.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.19.1")
public class ListVolumesForImportCmd extends BaseListCmd {

    @Inject
    public VolumeImportUnmanageService volumeImportService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.STORAGE_ID,
            type = BaseCmd.CommandType.UUID,
            required = true,
            entityType = StoragePoolResponse.class,
            description = "the ID of the storage pool")
    private Long storageId;

    @Parameter(name = ApiConstants.PATH,
            type = BaseCmd.CommandType.STRING,
            description = "the path of the volume on the storage pool")
    private String path;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getStorageId() {
        return storageId;
    }

    public String getPath() {
        return path;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ListResponse<VolumeForImportResponse> response = volumeImportService.listVolumesForImport(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
