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
package org.apache.cloudstack.mcp;

import org.apache.cloudstack.api.command.user.mcp.CreateMcpTokenCmd;
import org.apache.cloudstack.api.command.user.mcp.ListMcpTokensCmd;
import org.apache.cloudstack.api.command.user.mcp.RevokeMcpTokenCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.McpTokenResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface McpTokenService {
    String TOKEN_PREFIX = "csmcp_";

    ConfigKey<Boolean> McpEnabled = new ConfigKey<>(Boolean.class, "mcp.enabled", ConfigKey.CATEGORY_ADVANCED, "false",
            "Enables the MCP (Model Context Protocol) server of the management server, available at /client/mcp.",
            true, ConfigKey.Scope.Global, null, "MCP server enabled",
            null, ConfigKey.GROUP_MCP, ConfigKey.SUBGROUP_MCP_SERVER);

    ConfigKey<Integer> McpAsyncJobWait = new ConfigKey<>(Integer.class, "mcp.async.job.wait", ConfigKey.CATEGORY_ADVANCED, "20",
            "Maximum time, in seconds, an MCP tool call waits for an asynchronous job to finish before returning the pending job ID.",
            true, ConfigKey.Scope.Global, null, "MCP async job wait",
            null, ConfigKey.GROUP_MCP, ConfigKey.SUBGROUP_MCP_SERVER);

    /**
     * Creates a token for the calling user. The returned response is the only place where the token itself is available.
     */
    McpTokenResponse createToken(CreateMcpTokenCmd cmd);

    ListResponse<McpTokenResponse> listTokens(ListMcpTokensCmd cmd);

    boolean revokeToken(RevokeMcpTokenCmd cmd);

    McpToken findById(long id);

    /**
     * @return the token entity matching the given token, or null if it does not exist, was revoked or has expired.
     */
    McpToken authenticate(String token);
}
