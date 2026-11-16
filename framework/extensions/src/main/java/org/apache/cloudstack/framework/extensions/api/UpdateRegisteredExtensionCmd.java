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

import com.cloud.user.Account;

@APICommand(name = "updateRegisteredExtension",
        description = "Update details for an extension registered with a resource",
        responseObject = ExtensionResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {Extension.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class UpdateRegisteredExtensionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    @Parameter(name = ApiConstants.EXTENSION_ID, type = CommandType.UUID, required = true,
            entityType = ExtensionResponse.class, description = "ID of the extension")
    private Long extensionId;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, required = true,
            description = "ID of the resource where the extension is registered")
    private String resourceId;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true,
            description = "Type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "Details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].endpoint.url=urlvalue")
    protected Map details;

    @Parameter(name = ApiConstants.CLEAN_UP_DETAILS,
            type = CommandType.BOOLEAN,
            description = "Optional boolean field, which indicates if details should be cleaned up or not " +
                    "(If set to true, details removed for this registration, details field ignored; " +
                    "if false or not set, details can be updated through details map)")
    private Boolean cleanupDetails;

    public Long getExtensionId() {
        return extensionId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    public Boolean isCleanupDetails() {
        return cleanupDetails;
    }

    @Override
    public void execute() throws ServerApiException {
        Extension extension = extensionsManager.updateRegisteredExtensionWithResource(this);
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

    @Override
    public Long getApiResourceId() {
        return getExtensionId();
    }
}
