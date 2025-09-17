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
package org.apache.cloudstack.consoleproxy;

import com.cloud.agent.AgentManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.ConsoleSessionVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.consoleproxy.ListConsoleSessionsCmd;
import org.apache.cloudstack.api.response.ConsoleSessionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleAccessManagerImplTest {

    @Mock
    private AccountManager accountManager;
    @Mock
    private VirtualMachineManager virtualMachineManager;
    @Mock
    private ManagementServer managementServer;
    @Mock
    private EntityManager entityManager;
    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Mock
    private KeysManager keysManager;
    @Mock
    private AgentManager agentManager;

    @Spy
    @InjectMocks
    ConsoleAccessManagerImpl consoleAccessManager = new ConsoleAccessManagerImpl();

    @Mock
    VirtualMachine virtualMachine;
    @Mock
    Account account;

    @Mock
    private CallContext callContextMock;
    @Mock
    private DomainDao domainDaoMock;
    @Mock
    private DomainVO domainMock;
    @Mock
    private ConsoleSessionVO consoleSessionMock;
    @Mock
    private ConsoleSessionDao consoleSessionDaoMock;
    @Mock
    private ConsoleSessionResponse consoleSessionResponseMock;
    @Mock
    private ListConsoleSessionsCmd listConsoleSessionsCmdMock;
    @Mock
    private ResponseGenerator responseGeneratorMock;

    @Test
    public void testCheckSessionPermissionAdminAccount() {
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);
        Assert.assertTrue(consoleAccessManager.checkSessionPermission(virtualMachine, account));
    }

    @Test
    public void testCheckSessionPermissionUserOwnedVm() {
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        Mockito.when(virtualMachine.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.doNothing().when(accountManager).checkAccess(
                Mockito.eq(account), Mockito.nullable(SecurityChecker.AccessType.class),
                Mockito.eq(true), Mockito.eq(virtualMachine));
        Assert.assertTrue(consoleAccessManager.checkSessionPermission(virtualMachine, account));
    }

    @Test
    public void testCheckSessionPermissionDifferentUserOwnedVm() {
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        Mockito.when(virtualMachine.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkAccess(
                Mockito.eq(account), Mockito.nullable(SecurityChecker.AccessType.class),
                Mockito.eq(true), Mockito.eq(virtualMachine));
        Assert.assertFalse(consoleAccessManager.checkSessionPermission(virtualMachine, account));
    }

    @Test
    public void testCheckSessionPermissionForUsersOnSystemVms() {
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(false);
        List<VirtualMachine.Type> systemVmTypes = Arrays.asList(VirtualMachine.Type.DomainRouter,
                VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        for (VirtualMachine.Type type : systemVmTypes) {
            Mockito.when(virtualMachine.getType()).thenReturn(type);
            Assert.assertFalse(consoleAccessManager.checkSessionPermission(virtualMachine, account));
        }
    }

    @Test
    public void listConsoleSessionsInternalTestNormalUsersShouldOnlyBeAllowedToListTheirOwnConsoleSessions() {
        long callerDomainId = 5L;
        long callerAccountId = 5L;
        long callerUserId = 5L;
        boolean isRecursive = false;

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(listConsoleSessionsCmdMock.getDomainId()).thenReturn(null);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            Mockito.when(account.getDomainId()).thenReturn(callerDomainId);
            Mockito.when(listConsoleSessionsCmdMock.isRecursive()).thenReturn(isRecursive);
            Mockito.when(accountManager.isNormalUser(callerAccountId)).thenReturn(true);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(callerAccountId);
            Mockito.when(callContextMock.getCallingUserId()).thenReturn(callerUserId);

            consoleAccessManager.listConsoleSessionsInternal(listConsoleSessionsCmdMock);
        }

        Mockito.verify(consoleSessionDaoMock).listConsoleSessions(
                Mockito.any(), Mockito.eq(List.of(callerDomainId)), Mockito.eq(callerAccountId),
                Mockito.eq(callerUserId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()
        );
    }

    @Test
    public void listConsoleSessionsInternalTestAdminsShouldBeAllowedToRetrieveOtherAccountsConsoleSessions() {
        long callerDomainId = 5L;
        long callerAccountId = 5L;
        long callerUserId = 5L;
        boolean isRecursive = false;

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(listConsoleSessionsCmdMock.getDomainId()).thenReturn(callerDomainId);
            Mockito.doReturn(callerDomainId).when(consoleAccessManager).getBaseDomainIdToListConsoleSessions(callerDomainId);
            Mockito.when(listConsoleSessionsCmdMock.getAccountId()).thenReturn(callerAccountId);
            Mockito.when(listConsoleSessionsCmdMock.getUserId()).thenReturn(callerUserId);
            Mockito.when(listConsoleSessionsCmdMock.isRecursive()).thenReturn(isRecursive);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(callerAccountId);
            Mockito.when(accountManager.isNormalUser(callerAccountId)).thenReturn(false);

            consoleAccessManager.listConsoleSessionsInternal(listConsoleSessionsCmdMock);
        }

        Mockito.verify(consoleSessionDaoMock).listConsoleSessions(
                Mockito.any(), Mockito.eq(List.of(callerDomainId)), Mockito.eq(callerAccountId),
                Mockito.eq(callerUserId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()
        );
    }


    @Test
    public void listConsoleSessionsInternalTestShouldNotFetchConsoleSessionsRecursivelyWhenIsRecursiveIsFalse() {
        long callerDomainId = 5L;
        long callerAccountId = 5L;
        long callerUserId = 5L;
        boolean isRecursive = false;

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(listConsoleSessionsCmdMock.getDomainId()).thenReturn(callerDomainId);
            Mockito.doReturn(callerDomainId).when(consoleAccessManager).getBaseDomainIdToListConsoleSessions(callerDomainId);
            Mockito.when(listConsoleSessionsCmdMock.getAccountId()).thenReturn(callerAccountId);
            Mockito.when(listConsoleSessionsCmdMock.getUserId()).thenReturn(callerUserId);
            Mockito.when(listConsoleSessionsCmdMock.isRecursive()).thenReturn(isRecursive);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(callerAccountId);
            Mockito.when(accountManager.isNormalUser(callerAccountId)).thenReturn(false);

            consoleAccessManager.listConsoleSessionsInternal(listConsoleSessionsCmdMock);
        }

        Mockito.verify(consoleSessionDaoMock).listConsoleSessions(
                Mockito.any(), Mockito.eq(List.of(callerDomainId)), Mockito.eq(callerAccountId),
                Mockito.eq(callerUserId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()
        );
    }

    @Test
    public void listConsoleSessionsInternalTestShouldFetchConsoleSessionsRecursivelyWhenIsRecursiveIsTrue() {
        long callerDomainId = 5L;
        long callerAccountId = 5L;
        long callerUserId = 5L;
        boolean isRecursive = true;
        List<Long> domainIdsCallerHasAccessTo = List.of(callerDomainId, 6L, 7L);

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(listConsoleSessionsCmdMock.getDomainId()).thenReturn(callerDomainId);
            Mockito.doReturn(callerDomainId).when(consoleAccessManager).getBaseDomainIdToListConsoleSessions(callerDomainId);
            Mockito.when(listConsoleSessionsCmdMock.getAccountId()).thenReturn(callerAccountId);
            Mockito.when(listConsoleSessionsCmdMock.getUserId()).thenReturn(callerUserId);
            Mockito.when(listConsoleSessionsCmdMock.isRecursive()).thenReturn(isRecursive);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(callerAccountId);
            Mockito.when(accountManager.isNormalUser(callerAccountId)).thenReturn(false);
            Mockito.when(domainDaoMock.getDomainAndChildrenIds(callerDomainId)).thenReturn(domainIdsCallerHasAccessTo);

            consoleAccessManager.listConsoleSessionsInternal(listConsoleSessionsCmdMock);
        }

        Mockito.verify(consoleSessionDaoMock).listConsoleSessions(
                Mockito.any(), Mockito.eq(domainIdsCallerHasAccessTo), Mockito.eq(callerAccountId),
                Mockito.eq(callerUserId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any()
        );
    }

    @Test
    public void listConsoleSessionsTestShouldCreateResponsesWithFullViewForRootAdmins() {
        Mockito.doReturn(new Pair<>(List.of(consoleSessionMock), 1))
                .when(consoleAccessManager)
                .listConsoleSessionsInternal(listConsoleSessionsCmdMock);

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(2L);
            Mockito.when(accountManager.isRootAdmin(2L)).thenReturn(true);
            Mockito.when(responseGeneratorMock.createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Full)).thenReturn(consoleSessionResponseMock);

            consoleAccessManager.listConsoleSessions(listConsoleSessionsCmdMock);
        }
        Mockito.verify(responseGeneratorMock).createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Full);
    }

    @Test
    public void listConsoleSessionsTestShouldCreateResponsesWithRestrictedViewForNonRootAdmins() {
        Mockito.doReturn(new Pair<>(List.of(consoleSessionMock), 1))
                .when(consoleAccessManager)
                .listConsoleSessionsInternal(listConsoleSessionsCmdMock);

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccountId()).thenReturn(2L);
            Mockito.when(accountManager.isRootAdmin(2L)).thenReturn(false);
            Mockito.when(responseGeneratorMock.createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Restricted)).thenReturn(consoleSessionResponseMock);

            consoleAccessManager.listConsoleSessions(listConsoleSessionsCmdMock);
        }

        Mockito.verify(responseGeneratorMock).createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Restricted);
    }

    @Test
    public void getBaseDomainIdToListConsoleSessionsTestIfNoDomainIdIsProvidedReturnCallersDomainId() {
        long callerDomainId = 5L;

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            Mockito.when(account.getDomainId()).thenReturn(callerDomainId);
            Assert.assertEquals(callerDomainId, consoleAccessManager.getBaseDomainIdToListConsoleSessions(null));
        }
    }

    @Test
    public void getBaseDomainIdToListConsoleSessionsTestPerformAccessValidationWhenDomainIsProvided() {
        long domainId = 5L;

        try (MockedStatic<CallContext> callContextStaticMock = Mockito.mockStatic(CallContext.class)) {
            callContextStaticMock.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            Mockito.when(domainDaoMock.findById(domainId)).thenReturn(domainMock);
            Assert.assertEquals(domainId, consoleAccessManager.getBaseDomainIdToListConsoleSessions(domainId));
            Mockito.verify(accountManager).checkAccess(account, domainMock);
        }
    }

    @Test
    public void listConsoleSessionByIdTestShouldCallDbLayer() {
        consoleAccessManager.listConsoleSessionById(1L);
        Mockito.verify(consoleSessionDaoMock).findByIdIncludingRemoved(1L);
    }
}
