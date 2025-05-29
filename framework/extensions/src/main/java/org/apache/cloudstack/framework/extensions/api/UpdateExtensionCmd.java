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

import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.api.response.ExtensionResponse;
import com.cloud.extension.Extension;

import javax.inject.Inject;

import java.util.Map;

@APICommand(name = "updateExtension",
        description = "Create an extension",
        responseObject = ExtensionResponse.class,
        responseHasSensitiveInfo = false,
        since = "4.21.0")
public class UpdateExtensionCmd extends BaseCmd {

    @Inject
    ExtensionsManager extensionsManager;

    public static final String APINAME = "createExtension";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionResponse.class,
            required = true,
            description = "The ID of the extension")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING,
            description = "Description of the extension")
    private String description;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "Details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].endpoint.url=urlvalue")
    protected Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException {
        Extension extension = extensionsManager.updateExtension(this);
        ExtensionResponse response = extensionsManager.createExtensionResponse(extension);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
