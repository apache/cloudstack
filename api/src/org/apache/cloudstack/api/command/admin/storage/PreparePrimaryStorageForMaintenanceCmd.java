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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;

@APICommand(name = "enableStorageMaintenance", description="Puts storage pool into maintenance state", responseObject=StoragePoolResponse.class)
public class PreparePrimaryStorageForMaintenanceCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(PreparePrimaryStorageForMaintenanceCmd.class.getName());
    private static final String s_name = "prepareprimarystorageformaintenanceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = StoragePoolResponse.class,
            required=true, description="Primary storage ID")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "primarystorage";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.StoragePool;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MAINTENANCE_PREPARE_PRIMARY_STORAGE;
    }

    @Override
    public String getEventDescription() {
        return  "preparing storage pool: " + getId() + " for maintenance";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException{
        StoragePool result = _storageService.preparePrimaryStorageForMaintenance(getId());
        if (result != null){
            StoragePoolResponse response = _responseGenerator.createStoragePoolResponse(result);
            response.setResponseName("storagepool");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to prepare primary storage for maintenance");
        }
    }
}
