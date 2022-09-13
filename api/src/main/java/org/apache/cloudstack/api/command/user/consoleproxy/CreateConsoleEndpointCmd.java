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
package org.apache.cloudstack.api.command.user.consoleproxy;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ConsoleEndpointWebsocketResponse;
import org.apache.cloudstack.api.response.CreateConsoleEndpointResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.consoleproxy.ConsoleAccessManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.utils.consoleproxy.ConsoleAccessUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Map;

@APICommand(name = CreateConsoleEndpointCmd.APINAME, description = "Create a console endpoint to connect to a VM console",
        responseObject = CreateConsoleEndpointResponse.class, since = "4.18.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateConsoleEndpointCmd extends BaseCmd {

    public static final String APINAME = "createConsoleEndpoint";
    public static final Logger s_logger = Logger.getLogger(CreateConsoleEndpointCmd.class.getName());

    @Inject
    private ConsoleAccessManager consoleManager;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            entityType = UserVmResponse.class,
            required = true,
            description = "ID of the VM")
    private Long vmId;

    @Parameter(name = ApiConstants.TOKEN,
            type = CommandType.STRING,
            required = false,
            description = "(optional) extra security token, valid when the extra validation is enabled")
    private String extraSecurityToken;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        String clientAddress = getClientAddress();
        ConsoleEndpoint endpoint = consoleManager.generateConsoleEndpoint(vmId, extraSecurityToken, clientAddress);
        if (endpoint != null) {
            CreateConsoleEndpointResponse response = createResponse(endpoint);
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to generate console endpoint for vm " + vmId);
        }
    }

    private CreateConsoleEndpointResponse createResponse(ConsoleEndpoint endpoint) {
        CreateConsoleEndpointResponse response = new CreateConsoleEndpointResponse();
        response.setResult(endpoint.isResult());
        response.setDetails(endpoint.getDetails());
        response.setUrl(endpoint.getUrl());
        response.setWebsocketResponse(createWebsocketResponse(endpoint));
        response.setResponseName(getCommandName());
        response.setObjectName("consoleendpoint");
        return response;
    }

    private ConsoleEndpointWebsocketResponse createWebsocketResponse(ConsoleEndpoint endpoint) {
        ConsoleEndpointWebsocketResponse wsResponse = new ConsoleEndpointWebsocketResponse();
        wsResponse.setHost(endpoint.getWebsocketHost());
        wsResponse.setPort(endpoint.getWebsocketPort());
        wsResponse.setPath(endpoint.getWebsocketPath());
        wsResponse.setToken(endpoint.getWebsocketToken());
        wsResponse.setExtra(endpoint.getWebsocketExtra());
        wsResponse.setObjectName("websocket");
        return wsResponse;
    }

    private String getParameterBase(String paramKey) {
        Map<String, String> params = getFullUrlParams();
        return MapUtils.isNotEmpty(params) ? params.get(paramKey) : null;
    }

    private String getClientAddress() {
        return getParameterBase(ConsoleAccessUtils.CLIENT_INET_ADDRESS_KEY);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
