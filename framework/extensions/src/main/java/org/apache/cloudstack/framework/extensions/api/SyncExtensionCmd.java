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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;

@APICommand(name = "syncExtension",
        description = "To sync the extension files from one management server to other management server(s)",
        responseObject = SuccessResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {Extension.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class SyncExtensionCmd extends BaseAsyncCmd {

    @Inject
    ExtensionsManager extensionsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, required = true,
            entityType = ExtensionResponse.class, description = "ID of the extension")
    private Long id;

    @Parameter(name = ApiConstants.SOURCE_MANAGEMENT_SERVER_ID, type = BaseCmd.CommandType.UUID, required = true,
            entityType = ManagementServerResponse.class,
            description = "ID of the management server from which files are to be synced")
    private Long sourceManagementServerId;

    @Parameter(name = ApiConstants.TARGET_MANAGEMENT_SERVER_IDS, type = BaseCmd.CommandType.LIST,
            collectionType = BaseCmd.CommandType.UUID, entityType = ManagementServerResponse.class,
            description="the IDs of the management servers to which the extension files are to be synced. "
                    + "If not specified, the files will be synced to all management servers")
    private List<Long> targetManagementServerIds;

    @Parameter(name = ApiConstants.FILES, type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "List of files to sync. Specify absolute or relative paths. If not provided, all extension " +
                    "files will be synced.")
    private List<String> files;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getSourceManagementServerId() {
        return sourceManagementServerId;
    }

    public List<Long> getTargetManagementServerIds() {
        return targetManagementServerIds;
    }

    public List<String> getFiles() {
        return files;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        boolean result = extensionsManager.syncExtension(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to sync extension");
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

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTENSION_SYNC;
    }

    @Override
    public String getEventDescription() {
        return "Sync extension: " + getId();
    }
}
