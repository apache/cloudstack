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
package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.context.CallContext;

import java.util.logging.Logger;

@APICommand(name = SyncStoragePoolCmd.APINAME,
        description = "Sync storage pool with management server (currently supported for Datastore Cluster in VMware and syncs the datastores in it)",
        responseObject = StoragePoolResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.15.1",
        authorized = {RoleType.Admin}
        )
public class SyncStoragePoolCmd extends BaseAsyncCmd {

    public static final String APINAME = "syncStoragePool";
    public static final Logger LOGGER = Logger.getLogger(SyncStoragePoolCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, required = true, description = "Storage pool id")
    private Long poolId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPoolId() {
        return poolId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SYNC_STORAGE_POOL;
    }

    @Override
    public String getEventDescription() {
        return "Attempting to synchronise storage pool with management server";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        StoragePool result = _storageService.syncStoragePool(this);
        if (result != null) {
            StoragePoolResponse response = _responseGenerator.createStoragePoolResponse(result);
            response.setResponseName("storagepool");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to synchronise storage pool");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
