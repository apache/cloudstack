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
package org.apache.cloudstack.api.command.user.mcp;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.McpTokenResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mcp.McpTokenService;

@APICommand(name = "listMcpTokens",
        description = "Lists the access tokens for the MCP (Model Context Protocol) server of a user. The tokens themselves are never returned.",
        responseObject = McpTokenResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.User, RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin},
        since = "24.0.0")
public class ListMcpTokensCmd extends BaseListCmd {
    @Inject
    private McpTokenService mcpTokenService;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = McpTokenResponse.class, description = "ID of the token.")
    private Long id;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class,
            description = "ID of the user that owns the tokens. Defaults to the calling user.")
    private Long userId;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() {
        ListResponse<McpTokenResponse> response = mcpTokenService.listTokens(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
