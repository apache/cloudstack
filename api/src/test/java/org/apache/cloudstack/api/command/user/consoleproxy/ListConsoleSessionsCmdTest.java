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
// under the License.import org.apache.cloudstack.context.CallContext;
package org.apache.cloudstack.api.command.user.consoleproxy;

import org.apache.cloudstack.consoleproxy.ConsoleSession;
import com.cloud.user.AccountService;

import com.cloud.user.UserAccount;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.consoleproxy.ConsoleAccessManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListConsoleSessionsCmdTest {
    @Mock
    private AccountService accountServiceMock;

    @Mock
    private ConsoleAccessManager consoleAccessManagerMock;

    @Spy
    @InjectMocks
    private ListConsoleSessionsCmd listConsoleSessionsCmdSpy;

    @Test
    public void executeTestApiExecutionShouldCallServiceLayer() {
        Mockito.when(consoleAccessManagerMock.listConsoleSessions(listConsoleSessionsCmdSpy)).thenReturn(new ListResponse<>());
        listConsoleSessionsCmdSpy.execute();
        Mockito.verify(consoleAccessManagerMock).listConsoleSessions(listConsoleSessionsCmdSpy);
    }

    @Test
    public void getEntityOwnerIdTestReturnConsoleSessionIdIfProvided() {
        ConsoleSession consoleSessionMock = Mockito.mock(ConsoleSession.class);
        long consoleSessionId = 2L;
        long accountId = 2L;

        Mockito.when(listConsoleSessionsCmdSpy.getId()).thenReturn(consoleSessionId);
        Mockito.when(consoleAccessManagerMock.listConsoleSessionById(consoleSessionId)).thenReturn(consoleSessionMock);
        Mockito.when(consoleSessionMock.getAccountId()).thenReturn(accountId);

        Assert.assertEquals(accountId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdTestReturnAccountIdWhenNoConsoleSessionIdIsProvided() {
        long accountId = 2L;

        Mockito.when(listConsoleSessionsCmdSpy.getId()).thenReturn(null);
        Mockito.when(listConsoleSessionsCmdSpy.getAccountId()).thenReturn(accountId);

        Assert.assertEquals(accountId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdTestReturnUserIdWhenNoConsoleSessionIdAndAccountIdAreProvided() {
        UserAccount userAccountMock = Mockito.mock(UserAccount.class);
        long userId = 2L;

        Mockito.when(listConsoleSessionsCmdSpy.getId()).thenReturn(null);
        Mockito.when(listConsoleSessionsCmdSpy.getAccountId()).thenReturn(null);
        Mockito.when(listConsoleSessionsCmdSpy.getUserId()).thenReturn(userId);
        Mockito.when(accountServiceMock.getUserAccountById(userId)).thenReturn(userAccountMock);
        Mockito.when(userAccountMock.getAccountId()).thenReturn(userId);

        Assert.assertEquals(userId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdTestReturnSystemAccountIdWhenNoConsoleSessionIdAndAccountIdAndUserIdAreProvided() {
        long systemAccountId = 1L;

        Mockito.when(listConsoleSessionsCmdSpy.getId()).thenReturn(null);
        Mockito.when(listConsoleSessionsCmdSpy.getAccountId()).thenReturn(null);
        Mockito.when(listConsoleSessionsCmdSpy.getUserId()).thenReturn(null);

        Assert.assertEquals(systemAccountId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdTestReturnSystemAccountIdWhenConsoleSessionDoesNotExist() {
        long consoleSessionId = 2L;
        long systemAccountId = 1L;

        Mockito.when(listConsoleSessionsCmdSpy.getId()).thenReturn(consoleSessionId);
        Mockito.when(consoleAccessManagerMock.listConsoleSessionById(consoleSessionId)).thenReturn(null);

        Assert.assertEquals(systemAccountId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdTestReturnSystemAccountIdWhenUserAccountDoesNotExist() {
        long userId = 2L;
        long systemAccountId = 1L;

        Mockito.when(listConsoleSessionsCmdSpy.getUserId()).thenReturn(userId);
        Mockito.when(accountServiceMock.getUserAccountById(userId)).thenReturn(null);

        Assert.assertEquals(systemAccountId, listConsoleSessionsCmdSpy.getEntityOwnerId());
    }
}
