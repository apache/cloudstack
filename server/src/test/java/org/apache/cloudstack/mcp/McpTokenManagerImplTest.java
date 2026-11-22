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

import java.util.Date;

import org.apache.cloudstack.api.command.user.mcp.CreateMcpTokenCmd;
import org.apache.cloudstack.api.command.user.mcp.RevokeMcpTokenCmd;
import org.apache.cloudstack.api.response.McpTokenResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mcp.dao.McpTokenDao;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class McpTokenManagerImplTest {
    private static final long USER_ID = 42L;
    private static final long ACCOUNT_ID = 7L;
    private static final long DOMAIN_ID = 3L;
    private static final long DAY_IN_MILLIS = 24L * 60 * 60 * 1000;

    @Mock
    private McpTokenDao mcpTokenDao;
    @Mock
    private UserDao userDao;
    @Mock
    private AccountService accountService;
    @Mock
    private User user;
    @Mock
    private Account account;
    @Mock
    private CreateMcpTokenCmd createCmd;
    @Mock
    private RevokeMcpTokenCmd revokeCmd;

    @InjectMocks
    private McpTokenManagerImpl manager;

    @Before
    public void setUp() {
        Mockito.when(user.getId()).thenReturn(USER_ID);
        Mockito.when(account.getId()).thenReturn(ACCOUNT_ID);
        Mockito.when(account.getDomainId()).thenReturn(DOMAIN_ID);
        CallContext.register(user, account);

        Mockito.when(createCmd.getName()).thenReturn("my-token");
        Mockito.when(mcpTokenDao.persist(Mockito.any(McpTokenVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    private McpTokenVO tokenExpiringAt(Date endDate) {
        return new McpTokenVO("my-token", null, USER_ID, ACCOUNT_ID, DOMAIN_ID, "hash", endDate);
    }

    @Test
    public void createTokenPersistsOnlyTheHashOfTheToken() {
        McpTokenResponse response = manager.createToken(createCmd);

        String token = response.getToken();
        Assert.assertTrue(token.startsWith(McpTokenService.TOKEN_PREFIX));

        ArgumentCaptor<McpTokenVO> captor = ArgumentCaptor.forClass(McpTokenVO.class);
        Mockito.verify(mcpTokenDao).persist(captor.capture());
        McpTokenVO persisted = captor.getValue();
        Assert.assertEquals(DigestUtils.sha256Hex(token), persisted.getTokenHash());
        Assert.assertNotEquals(token, persisted.getTokenHash());
        Assert.assertEquals("my-token", persisted.getName());
        Assert.assertEquals(USER_ID, persisted.getUserId());
        Assert.assertEquals(ACCOUNT_ID, persisted.getAccountId());
        Assert.assertEquals(DOMAIN_ID, persisted.getDomainId());
    }

    @Test
    public void createTokenGeneratesADifferentTokenOnEachCall() {
        Assert.assertNotEquals(manager.createToken(createCmd).getToken(), manager.createToken(createCmd).getToken());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createTokenRejectsAnEndDateInThePast() {
        Mockito.when(createCmd.getEndDate()).thenReturn(new Date(System.currentTimeMillis() - DAY_IN_MILLIS));
        manager.createToken(createCmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void createTokenRejectsTheSystemUser() {
        Mockito.when(user.getId()).thenReturn(User.UID_SYSTEM);
        manager.createToken(createCmd);
    }

    @Test
    public void authenticateReturnsTheTokenMatchingTheHash() {
        String token = McpTokenService.TOKEN_PREFIX + "abc";
        McpTokenVO stored = tokenExpiringAt(new Date(System.currentTimeMillis() + DAY_IN_MILLIS));
        Mockito.when(mcpTokenDao.findByTokenHash(DigestUtils.sha256Hex(token))).thenReturn(stored);

        Assert.assertSame(stored, manager.authenticate(token));
    }

    @Test
    public void authenticateAcceptsATokenWithoutEndDate() {
        String token = McpTokenService.TOKEN_PREFIX + "abc";
        McpTokenVO stored = tokenExpiringAt(null);
        Mockito.when(mcpTokenDao.findByTokenHash(DigestUtils.sha256Hex(token))).thenReturn(stored);

        Assert.assertSame(stored, manager.authenticate(token));
    }

    @Test
    public void authenticateRejectsAnExpiredToken() {
        String token = McpTokenService.TOKEN_PREFIX + "abc";
        Mockito.when(mcpTokenDao.findByTokenHash(DigestUtils.sha256Hex(token)))
                .thenReturn(tokenExpiringAt(new Date(System.currentTimeMillis() - DAY_IN_MILLIS)));

        Assert.assertNull(manager.authenticate(token));
    }

    @Test
    public void authenticateRejectsAnUnknownOrRevokedToken() {
        Assert.assertNull(manager.authenticate(McpTokenService.TOKEN_PREFIX + "unknown"));
    }

    @Test
    public void authenticateRejectsValuesThatAreNotMcpTokensWithoutQueryingTheDatabase() {
        Assert.assertNull(manager.authenticate(null));
        Assert.assertNull(manager.authenticate(""));
        Assert.assertNull(manager.authenticate("some-api-key"));
        Mockito.verifyNoInteractions(mcpTokenDao);
    }

    @Test
    public void revokeTokenRemovesTheToken() {
        McpTokenVO stored = tokenExpiringAt(null);
        Mockito.when(revokeCmd.getId()).thenReturn(5L);
        Mockito.when(mcpTokenDao.findById(5L)).thenReturn(stored);
        Mockito.when(mcpTokenDao.remove(stored.getId())).thenReturn(true);

        Assert.assertTrue(manager.revokeToken(revokeCmd));
        Mockito.verify(accountService).validateCallingUserHasAccessToDesiredUser(USER_ID);
    }

    @Test
    public void revokeTokenOfAnInaccessibleUserIsDenied() {
        Mockito.when(revokeCmd.getId()).thenReturn(5L);
        Mockito.when(mcpTokenDao.findById(5L)).thenReturn(tokenExpiringAt(null));
        Mockito.doThrow(new PermissionDeniedException("denied")).when(accountService).validateCallingUserHasAccessToDesiredUser(USER_ID);

        Assert.assertThrows(PermissionDeniedException.class, () -> manager.revokeToken(revokeCmd));
        Mockito.verify(mcpTokenDao, Mockito.never()).remove(Mockito.anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void revokeTokenThatDoesNotExistFails() {
        Mockito.when(revokeCmd.getId()).thenReturn(5L);
        manager.revokeToken(revokeCmd);
    }
}
