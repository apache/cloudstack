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
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.api.response.ExtensionResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListExtensionsCmd.APINAME, description = "list of extensions",
        responseObject = ExtensionResponse.class, responseHasSensitiveInfo = false, since = "4.21.0")
public class ListExtensionsCmd extends BaseListCmd {
    public static final String APINAME = "listExtensions";

    @Inject
    ExtensionsManager extensionsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the extension")
    private String name;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionResponse.class, description = "uuid of the extension")
    private Long extensionId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public Long getExtensionId() {
        return extensionId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<ExtensionResponse> responses = extensionsManager.listExtensions(this);

        ListResponse<ExtensionResponse> response = new ListResponse<>();
        response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
