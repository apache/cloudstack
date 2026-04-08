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

import java.util.EnumSet;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

@APICommand(name = "createExtension",
        description = "Create an extension",
        responseObject = ExtensionResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {Extension.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class CreateExtensionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Name of the extension")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING,
            description = "Description of the extension")
    private String description;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true,
            description = "Type of the extension")
    private String type;

    @Parameter(name = ApiConstants.PATH, type = CommandType.STRING,
            description = "Relative path for the extension")
    private String path;

    @Parameter(name = ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM,
            type = CommandType.BOOLEAN,
            description = "Only honored when type is Orchestrator. Whether prepare VM is needed or not")
    private Boolean orchestratorRequiresPrepareVm;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING,
            description = "State of the extension")
    private String state;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "Details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].endpoint.url=urlvalue")
    protected Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Boolean isOrchestratorRequiresPrepareVm() {
        return orchestratorRequiresPrepareVm;
    }

    public String getState() {
        return state;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Extension extension = extensionsManager.createExtension(this);
        ExtensionResponse response = extensionsManager.createExtensionResponse(extension,
                EnumSet.of(ApiConstants.ExtensionDetails.all));
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Extension;
    }
}
