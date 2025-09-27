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

package org.apache.cloudstack.framework.extensions.api;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;

@APICommand(name = "deleteExtension",
        description = "Delete the extensions",
        responseObject = ExtensionResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {Extension.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class DeleteExtensionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionResponse.class, description = "ID of the extension")
    private Long id;

    @Parameter(name = ApiConstants.CLEANUP, type = CommandType.BOOLEAN,
            entityType = ExtensionResponse.class,
            description = "Whether to cleanup files for the extension. If true, the extension files will be deleted from all the management servers.")
    private Boolean cleanup;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean isCleanup() {
        return Boolean.TRUE.equals(cleanup);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        boolean result = extensionsManager.deleteExtension(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete extension");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Extension;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }
}
