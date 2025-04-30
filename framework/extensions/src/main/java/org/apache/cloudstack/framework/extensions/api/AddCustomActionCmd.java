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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.extension.ExtensionCustomAction;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@APICommand(name = "addCustomAction", description = "Register the custom action",
        responseObject = SuccessResponse.class, responseHasSensitiveInfo = false, since = "4.21.0")
public class AddCustomActionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    @Parameter(name = ApiConstants.EXTENSION_ID, type = CommandType.UUID, required = true,
            entityType = ExtensionResponse.class, description = "the extension id used to call the custom action")
    private Long extensionId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the command")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the command")
    private String description;

    @Parameter(name = ApiConstants.ROLES_LIST,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "the list of allowed role types")
    private List<String> rolesList;

    @Parameter(name = ApiConstants.CUSTOM_ACTION_PARAMETERS, type = CommandType.MAP, description = "Details in key/value pairs using format parameters[i].keyname=keyvalue. Example: parameters[0].snapshotmemory=Boolean", since = "4.21.0")
    protected Map customActionParameters;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getExtensionId() {
        return extensionId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getRolesList() {
        return rolesList;
    }

    public Map getCustomActionParameters() {
        return convertDetailsToMap(customActionParameters);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ExtensionCustomAction extensionCustomAction = extensionsManager.addCustomAction(this);
        ExtensionCustomActionResponse response = new ExtensionCustomActionResponse(extensionCustomAction.getUuid(), extensionCustomAction.getName(), extensionCustomAction.getDescription(), extensionCustomAction.getRolesList());
        response.setResponseName(getCommandName());
        response.setObjectName(ApiConstants.EXTENSION_CUSTOM_ACTION);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
