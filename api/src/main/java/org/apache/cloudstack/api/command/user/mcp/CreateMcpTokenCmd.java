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

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.McpTokenResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mcp.McpTokenService;

@APICommand(name = "createMcpToken",
        description = "Creates an access token for the MCP (Model Context Protocol) server, owned by the calling user. " +
                "The token is returned only by this call and cannot be retrieved later.",
        responseObject = McpTokenResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.User, RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin},
        since = "24.0.0")
public class CreateMcpTokenCmd extends BaseCmd {
    @Inject
    private McpTokenService mcpTokenService;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the token.")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, length = 1024, description = "Description of the token.")
    private String description;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "Expiration date of the token. If not informed, the token does not expire. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() {
        McpTokenResponse response = mcpTokenService.createToken(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
