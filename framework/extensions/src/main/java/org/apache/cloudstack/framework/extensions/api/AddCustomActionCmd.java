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
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import com.cloud.user.Account;

@APICommand(name = "addCustomAction",
        description = "Add a custom action for an extension",
        responseObject = ExtensionCustomActionResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {ExtensionCustomAction.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class AddCustomActionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    @Parameter(name = ApiConstants.EXTENSION_ID, type = CommandType.UUID, required = true,
            entityType = ExtensionResponse.class, description = "The ID of the extension to associate the action with")
    private Long extensionId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the action")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Description of the action")
    private String description;

    @Parameter(name = ApiConstants.RESOURCE_TYPE,
            type = CommandType.STRING,
            description = "Resource type for which the action is available")
    private String resourceType;

    @Parameter(name = ApiConstants.ALLOWED_ROLE_TYPES,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "List of role types allowed for the action")
    private List<String> allowedRoleTypes;

    @Parameter(name = ApiConstants.PARAMETERS, type = CommandType.MAP,
            description = "Parameters mapping for the action using keys - name, type, required. " +
                    "'name' is mandatory. If 'type' is not specified then STRING will be used. " +
                    "If 'required' is not specified then false will be used. "
                    + "Example: parameters[0].name=xxx&parameters[0].type=BOOLEAN&parameters[0].required=true")
    protected Map parameters;

    @Parameter(name = ApiConstants.SUCCESS_MESSAGE, type = CommandType.STRING,
            description = "Success message that will be used on successful execution of the action. " +
                    "Name of the action, extension, resource can be used as - actionName, extensionName, resourceName. "
                    + "Example: Successfully complete {{actionName}} for {{resourceName}} with {{extensionName}}")
    protected String successMessage;

    @Parameter(name = ApiConstants.ERROR_MESSAGE, type = CommandType.STRING,
            description = "Error message that will be used on failure during execution of the action. " +
                    "Name of the action, extension, resource can be used as - actionName, extensionName, resourceName. "
                    + "Example: Failed to complete {{actionName}} for {{resourceName}} with {{extensionName}}")
    protected String errorMessage;

    @Parameter(name = ApiConstants.TIMEOUT,
            type = CommandType.INTEGER,
            description = "Specifies the timeout in seconds to wait for the action to complete before failing. Default value is 5 seconds")
    private Integer timeout;

    @Parameter(name = ApiConstants.ENABLED,
            type = CommandType.BOOLEAN,
            description = "Whether the action is enabled or not. Default is disabled.")
    private Boolean enabled;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.MAP,
            description = "Details in key/value pairs using format details[i].keyname=keyvalue. "
                    + "Example: details[0].vendor=xxx&&details[0].version=2.0")
    protected Map details;

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

    public String getResourceType() {
        return resourceType;
    }

    public List<String> getAllowedRoleTypes() {
        return allowedRoleTypes;
    }

    public Map getParametersMap() {
        return parameters;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ExtensionCustomAction extensionCustomAction = extensionsManager.addCustomAction(this);
        ExtensionCustomActionResponse response = extensionsManager.createCustomActionResponse(extensionCustomAction);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.ExtensionCustomAction;
    }
}
