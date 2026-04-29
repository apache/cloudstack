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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import com.cloud.user.Account;

@APICommand(name = "updateCustomAction",
        description = "Update the custom action",
        responseObject = SuccessResponse.class,
        responseHasSensitiveInfo = false, since = "4.21.0")
public class UpdateCustomActionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            required = true,
            entityType = ExtensionCustomActionResponse.class,
            description = "ID of the custom action")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION,
            type = CommandType.STRING,
            description = "The description of the command")
    private String description;

    @Parameter(name = ApiConstants.RESOURCE_TYPE,
            type = CommandType.STRING,
            description = "Type of the resource for actions")
    private String resourceType;

    @Parameter(name = ApiConstants.ALLOWED_ROLE_TYPES,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "List of role types allowed for the action")
    private List<String> allowedRoleTypes;

    @Parameter(name = ApiConstants.ENABLED,
            type = CommandType.BOOLEAN,
            description = "Whether the action is enabled or not")
    private Boolean enabled;

    @Parameter(name = ApiConstants.PARAMETERS, type = CommandType.MAP,
            description = "Parameters mapping for the action using keys - name, type, required. " +
                    "'name' is mandatory. If 'type' is not specified then STRING will be used. " +
                    "If 'required' is not specified then false will be used. "
                    + "Example: parameters[0].name=xxx&parameters[0].type=BOOLEAN&parameters[0].required=true")
    protected Map parameters;

    @Parameter(name = ApiConstants.CLEAN_UP_PARAMETERS,
            type = CommandType.BOOLEAN,
            description = "Optional boolean field, which indicates if parameters should be cleaned up or not " +
                    "(If set to true, parameters will be removed for this action, parameters field ignored; " +
                    "if false or not set, no action)")
    private Boolean cleanupParameters;

    @Parameter(name = ApiConstants.SUCCESS_MESSAGE, type = CommandType.STRING,
            description = "Success message that will be used on successful execution of the action. " +
                    "Name of the action and and extension can be used in the - actionName, extensionName. "
                    + "Example: Successfully complete {{actionName}} for {{extensionName")
    protected String successMessage;

    @Parameter(name = ApiConstants.ERROR_MESSAGE, type = CommandType.STRING,
            description = "Error message that will be used on failure during execution of the action. " +
                    "Name of the action and and extension can be used in the - actionName, extensionName. "
                    + "Example: Failed to complete {{actionName}} for {{extensionName")
    protected String errorMessage;

    @Parameter(name = ApiConstants.TIMEOUT,
            type = CommandType.INTEGER,
            description = "Specifies the timeout in seconds to wait for the action to complete before failing. Default value is 3 seconds")
    private Integer timeout;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.MAP,
            description = "Details in key/value pairs using format details[i].keyname=keyvalue. "
                    + "Example: details[0].vendor=xxx&&details[0].version=2.0")
    protected Map details;

    @Parameter(name = ApiConstants.CLEAN_UP_DETAILS,
            type = CommandType.BOOLEAN,
            description = "Optional boolean field, which indicates if details should be cleaned up or not " +
                    "(If set to true, details removed for this action, details field ignored; " +
                    "if false or not set, no action)")
    private Boolean cleanupDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getId() {
        return id;
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

    public Boolean isCleanupParameters() {
        return cleanupParameters;
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

    public Boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    public Boolean isCleanupDetails() {
        return cleanupDetails;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        ExtensionCustomAction extensionCustomAction = extensionsManager.updateCustomAction(this);
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

    @Override
    public Long getApiResourceId() {
        return getId();
    }
}
