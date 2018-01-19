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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;

@APICommand(name = "cancelStorageMaintenance", description = "Cancels maintenance for primary storage", responseObject = StoragePoolResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CancelPrimaryStorageMaintenanceCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CancelPrimaryStorageMaintenanceCmd.class.getName());

    private static final String s_name = "cancelprimarystoragemaintenanceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, required = true, description = "the primary storage ID")
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

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE;
    }

    @Override
    public String getEventDescription() {
        return "canceling maintenance for primary storage pool: " + getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        StoragePool result = _storageService.cancelPrimaryStorageForMaintenance(this);
        if (result != null) {
            StoragePoolResponse response = _responseGenerator.createStoragePoolResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to cancel primary storage maintenance");
        }
    }
}
