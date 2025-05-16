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
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "listCustomAction", description = "delete the custom action",
        responseObject = SuccessResponse.class, responseHasSensitiveInfo = false, since = "4.21.0")
public class ListCustomActionCmd extends BaseListCmd {

    @Inject
    ExtensionsManager extensionsManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionCustomActionResponse.class, description = "uuid of the custom action")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the custom action")
    private String name;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionResponse.class, description = "uuid of the extension")
    private Long extensionId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }

    public Long getExtensionId() {
        return extensionId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<ExtensionCustomActionResponse> responses = extensionsManager.listCustomActions(this);
        ListResponse<ExtensionCustomActionResponse> response = new ListResponse<>();
        response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
