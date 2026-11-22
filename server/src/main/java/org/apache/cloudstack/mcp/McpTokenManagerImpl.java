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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.mcp.CreateMcpTokenCmd;
import org.apache.cloudstack.api.command.user.mcp.ListMcpTokensCmd;
import org.apache.cloudstack.api.command.user.mcp.RevokeMcpTokenCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.McpTokenResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.mcp.dao.McpTokenDao;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;

public class McpTokenManagerImpl extends ManagerBase implements McpTokenService, PluggableService, Configurable {
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    private McpTokenDao mcpTokenDao;
    @Inject
    private UserDao userDao;
    @Inject
    private AccountService accountService;

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MCP_TOKEN_CREATE, eventDescription = "creating MCP token")
    public McpTokenResponse createToken(CreateMcpTokenCmd cmd) {
        User user = CallContext.current().getCallingUser();
        if (user.getId() == User.UID_SYSTEM) {
            throw new PermissionDeniedException("MCP tokens cannot be created for the system user.");
        }

        Date endDate = cmd.getEndDate();
        if (endDate != null && !endDate.after(new Date())) {
            throw new InvalidParameterValueException("The MCP token cannot be created already expired, please inform an end date in the future.");
        }

        Account account = CallContext.current().getCallingAccount();
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = TOKEN_PREFIX + Base64.encodeBase64URLSafeString(randomBytes);

        McpTokenVO mcpToken = mcpTokenDao.persist(new McpTokenVO(cmd.getName(), cmd.getDescription(), user.getId(), account.getId(),
                account.getDomainId(), DigestUtils.sha256Hex(token), endDate));

        McpTokenResponse response = createResponse(mcpToken);
        response.setToken(token);
        return response;
    }

    @Override
    public ListResponse<McpTokenResponse> listTokens(ListMcpTokensCmd cmd) {
        List<McpTokenVO> tokens;
        Integer count;

        if (cmd.getId() != null) {
            tokens = List.of(findAccessibleToken(cmd.getId()));
            count = 1;
        } else {
            long userId = CallContext.current().getCallingUserId();
            if (cmd.getUserId() != null) {
                accountService.validateCallingUserHasAccessToDesiredUser(cmd.getUserId());
                userId = cmd.getUserId();
            }
            Pair<List<McpTokenVO>, Integer> result = mcpTokenDao.listByUserId(userId, cmd.getStartIndex(), cmd.getPageSizeVal());
            tokens = result.first();
            count = result.second();
        }

        List<McpTokenResponse> responses = new ArrayList<>();
        for (McpTokenVO token : tokens) {
            responses.add(createResponse(token));
        }
        ListResponse<McpTokenResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, count);
        return listResponse;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MCP_TOKEN_REVOKE, eventDescription = "revoking MCP token")
    public boolean revokeToken(RevokeMcpTokenCmd cmd) {
        return mcpTokenDao.remove(findAccessibleToken(cmd.getId()).getId());
    }

    @Override
    public McpToken findById(long id) {
        return mcpTokenDao.findById(id);
    }

    @Override
    public McpToken authenticate(String token) {
        if (StringUtils.isBlank(token) || !token.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        McpTokenVO mcpToken = mcpTokenDao.findByTokenHash(DigestUtils.sha256Hex(token));
        if (mcpToken == null) {
            return null;
        }
        if (mcpToken.getEndDate() != null && !mcpToken.getEndDate().after(new Date())) {
            logger.debug("MCP token [{}] has expired on [{}].", mcpToken.getUuid(), mcpToken.getEndDate());
            return null;
        }
        return mcpToken;
    }

    private McpTokenVO findAccessibleToken(long id) {
        McpTokenVO token = mcpTokenDao.findById(id);
        if (token == null) {
            throw new InvalidParameterValueException("Unable to find the MCP token with the informed ID.");
        }
        accountService.validateCallingUserHasAccessToDesiredUser(token.getUserId());
        return token;
    }

    private McpTokenResponse createResponse(McpTokenVO token) {
        McpTokenResponse response = new McpTokenResponse();
        response.setId(token.getUuid());
        response.setName(token.getName());
        response.setDescription(token.getDescription());
        response.setEndDate(token.getEndDate());
        response.setCreated(token.getCreated());
        User user = userDao.findByIdIncludingRemoved(token.getUserId());
        if (user != null) {
            response.setUserId(user.getUuid());
            response.setUsername(user.getUsername());
        }
        response.setObjectName("mcptoken");
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateMcpTokenCmd.class);
        cmdList.add(ListMcpTokensCmd.class);
        cmdList.add(RevokeMcpTokenCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return McpTokenService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {McpEnabled, McpAsyncJobWait};
    }
}
