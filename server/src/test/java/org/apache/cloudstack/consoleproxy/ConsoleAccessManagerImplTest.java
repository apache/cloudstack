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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.consoleproxy.ConsoleEndpoint;
import org.apache.cloudstack.api.command.user.consoleproxy.ListConsoleSessionsCmd;
import org.apache.cloudstack.api.response.ConsoleSessionResponse;
import org.apache.cloudstack.consoleproxy.ConsoleAccessManagerImpl.ConsoleConnectionDetails;
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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetExternalConsoleAnswer;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.DetailVO;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.servlet.ConsoleProxyClientParam;
import com.cloud.servlet.ConsoleProxyPasswordBasedEncryptor;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.ConsoleSessionVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

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
    @Mock
    ConsoleProxyManager consoleProxyManager;

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

    @Test
    public void getConsoleConnectionDetailsForExternalVmReturnsNullWhenAnswerIsNull() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");

        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(null);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNull(result);
    }

    @Test
    public void getConsoleConnectionDetailsForExternalVmReturnsNullWhenAnswerResultIsFalse() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");
        Answer answer = Mockito.mock(Answer.class);

        Mockito.when(answer.getResult()).thenReturn(false);
        Mockito.when(answer.getDetails()).thenReturn("Error details");
        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(answer);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNull(result);
    }

    @Test
    public void getConsoleConnectionDetailsForExternalVmReturnsNullWhenAnswerIsNotOfTypeGetExternalConsoleAnswer() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");
        Answer answer = Mockito.mock(Answer.class);

        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(answer);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNull(result);
    }

    @Test
    public void getConsoleConnectionDetailsForExternalVmSetsDetailsWhenAnswerIsValid() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");
        GetExternalConsoleAnswer answer = Mockito.mock(GetExternalConsoleAnswer.class);

        String expectedHost = "10.0.0.1";
        int expectedPort = 5900;
        String expectedPassword = "password";

        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.getHost()).thenReturn(expectedHost);
        Mockito.when(answer.getPort()).thenReturn(expectedPort);
        Mockito.when(answer.getPassword()).thenReturn(expectedPassword);
        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(answer);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNotNull(result);
        Assert.assertEquals(ConsoleConnectionDetails.Mode.ConsoleProxy, result.getMode());
        Assert.assertEquals(expectedHost, result.getHost());
        Assert.assertEquals(expectedPort, result.getPort());
        Assert.assertEquals(expectedPassword, result.getSid());
        Assert.assertNull(result.getDirectUrl());
    }

    @Test
    public void getConsoleConnectionDetailsForExternalVmSetsDetailsWhenAnswerIsValidDirect() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");
        GetExternalConsoleAnswer answer = Mockito.mock(GetExternalConsoleAnswer.class);

        String url = "url";

        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.getUrl()).thenReturn(url);
        Mockito.when(answer.getProtocol()).thenReturn(ConsoleConnectionDetails.Mode.Direct.name());
        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(answer);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNotNull(result);
        Assert.assertEquals(ConsoleConnectionDetails.Mode.Direct, result.getMode());
        Assert.assertEquals(url, result.getDirectUrl());
    }

    @Test
    public void getConsoleConnectionDetailsForExternalVmDoesNotSetSidWhenPasswordIsBlank() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("sid", "en", "tag", "displayName");
        GetExternalConsoleAnswer answer = Mockito.mock(GetExternalConsoleAnswer.class);

        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.getHost()).thenReturn("10.0.0.1");
        Mockito.when(answer.getPort()).thenReturn(5900);
        Mockito.when(answer.getPassword()).thenReturn("");
        Mockito.when(managementServer.getExternalVmConsole(vm, host)).thenReturn(answer);

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetailsForExternalVm(details, vm, host);

        Assert.assertNotNull(result);
        Assert.assertEquals("10.0.0.1", result.getHost());
        Assert.assertEquals(5900, result.getPort());
        Assert.assertEquals("sid", result.getSid());
    }

    @Test
    public void getHostAndPortForKVMMaintenanceHostIfNeededReturnsNullForNonKVMHypervisor() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);

        Pair<String, Integer> result = consoleAccessManager.getHostAndPortForKVMMaintenanceHostIfNeeded(host, Map.of());

        Assert.assertNull(result);
    }

    @Test
    public void getHostAndPortForKVMMaintenanceHostIfNeededReturnsNullForNonMaintenanceResourceState() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(host.getResourceState()).thenReturn(ResourceState.Enabled);

        Pair<String, Integer> result = consoleAccessManager.getHostAndPortForKVMMaintenanceHostIfNeeded(host, Map.of());

        Assert.assertNull(result);
    }

    @Test
    public void getHostAndPortForKVMMaintenanceHostIfNeededReturnsHostAndPortForValidKVMInMaintenance() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(host.getResourceState()).thenReturn(ResourceState.ErrorInMaintenance);

        String address = "192.168.1.100";
        int port = 5901;
        Map<String, String> vmDetails = Map.of(
                VmDetailConstants.KVM_VNC_ADDRESS, address,
                VmDetailConstants.KVM_VNC_PORT, String.valueOf(port)
        );

        Pair<String, Integer> result = consoleAccessManager.getHostAndPortForKVMMaintenanceHostIfNeeded(host, vmDetails);

        Assert.assertNotNull(result);
        Assert.assertEquals(address, result.first());
        Assert.assertEquals(port, (int) result.second());
    }

    @Test
    public void getHostAndPortForKVMMaintenanceHostIfNeededReturnsNullWhenVncAddressOrPortIsMissing() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(host.getResourceState()).thenReturn(ResourceState.ErrorInMaintenance);

        Map<String, String> vmDetails = Map.of(VmDetailConstants.KVM_VNC_ADDRESS, "192.168.1.100");

        Pair<String, Integer> result = consoleAccessManager.getHostAndPortForKVMMaintenanceHostIfNeeded(host, vmDetails);

        Assert.assertNull(result);
    }

    @Test
    public void getConsoleConnectionDetailsReturnsDetailsForExternalHypervisor() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = Mockito.mock(ConsoleConnectionDetails.class);

        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getHostName()).thenReturn("vm-hostname");
        Mockito.when(vm.getVncPassword()).thenReturn("vnc-password");
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong(), Mockito.anyList())).thenReturn(Map.of());

        Mockito.doReturn(details).when(consoleAccessManager).getConsoleConnectionDetailsForExternalVm(Mockito.any(), Mockito.eq(vm), Mockito.eq(host));

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetails(vm, host);

        Assert.assertNotNull(result);
        Mockito.verify(consoleAccessManager).getConsoleConnectionDetailsForExternalVm(Mockito.any(), Mockito.eq(vm), Mockito.eq(host));
    }

    @Test
    public void getConsoleConnectionDetailsReturnsDetailsForKVMHypervisor() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        String hostAddress = "192.168.1.100";
        int port = 5900;
        String vmUuid = "vm-uuid";
        String vmHostName = "vm-hostname";
        String vncPassword = "vnc-password";

        Pair<String, Integer> hostPortInfo = new Pair<>(hostAddress, port);

        Mockito.when(vm.getUuid()).thenReturn(vmUuid);
        Mockito.when(vm.getHostName()).thenReturn(vmHostName);
        Mockito.when(vm.getVncPassword()).thenReturn(vncPassword);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong(), Mockito.anyList())).thenReturn(Map.of());
        Mockito.when(managementServer.getVncPort(vm)).thenReturn(hostPortInfo);
        Mockito.doReturn(new Ternary<>(hostAddress, null, null)).when(consoleAccessManager).parseHostInfo(Mockito.anyString());

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetails(vm, host);

        Assert.assertNotNull(result);
        Assert.assertEquals(hostAddress, result.getHost());
        Assert.assertEquals(port, result.getPort());
    }

    @Test
    public void getConsoleConnectionDetailsReturnsDetailsWithRDPForHyperV() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        String hostAddress = "192.168.1.100";
        Pair<String, Integer> hostPortInfo = new Pair<>(hostAddress, -9);

        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getHostName()).thenReturn("vm-hostname");
        Mockito.when(vm.getVncPassword()).thenReturn("vnc-password");
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.Hyperv);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong(), Mockito.anyList())).thenReturn(Map.of());
        Mockito.when(managementServer.getVncPort(vm)).thenReturn(hostPortInfo);
        int port = 3389;
        DetailVO detailVO = Mockito.mock(DetailVO.class);
        Mockito.when(detailVO.getValue()).thenReturn(String.valueOf(port));
        Mockito.when(managementServer.findDetail(Mockito.anyLong(), Mockito.eq("rdp.server.port"))).thenReturn(detailVO);
        Mockito.doReturn(new Ternary<>(hostAddress, null, null)).when(consoleAccessManager).parseHostInfo(Mockito.anyString());

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetails(vm, host);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isUsingRDP());
        Assert.assertEquals(port, result.getPort());
    }

    @Test
    public void getConsoleConnectionDetailsReturnsNullHostInvalidPortWhenVncPortInfoIsMissing() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);

        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getHostName()).thenReturn("vm-hostname");
        Mockito.when(vm.getVncPassword()).thenReturn("vnc-password");
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong(), Mockito.anyList())).thenReturn(Map.of());
        Mockito.when(managementServer.getVncPort(vm)).thenReturn(new Pair<>(null, -1));

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetails(vm, host);

        Assert.assertNull(result.getHost());
        Assert.assertEquals(-1, result.getPort());
    }

    @Test
    public void getConsoleConnectionDetailsSetsLocaleWhenKeyboardDetailIsPresent() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        String hostAddress = "192.168.1.100";
        Pair<String, Integer> hostPortInfo = new Pair<>(hostAddress, 5900);

        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getHostName()).thenReturn("vm-hostname");
        Mockito.when(vm.getVncPassword()).thenReturn("vnc-password");
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong(), Mockito.anyList())).thenReturn(Map.of(VmDetailConstants.KEYBOARD, "en-us"));
        Mockito.when(managementServer.getVncPort(vm)).thenReturn(hostPortInfo);
        Mockito.doReturn(new Ternary<>(hostAddress, null, null)).when(consoleAccessManager).parseHostInfo(Mockito.anyString());

        ConsoleConnectionDetails result = consoleAccessManager.getConsoleConnectionDetails(vm, host);

        Assert.assertNotNull(result);
        Assert.assertEquals("en-us", result.getLocale());
    }

    @Test
    public void generateConsoleProxyClientParamSetsBasicDetailsCorrectly() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        String hostAddress = "192.168.1.100";
        int port = 5902;
        String sid = "sid";
        String tag = "tag";
        String displayName = "displayName";
        String ticket = "ticket";
        String sessionUuid = "sessionUuid";
        String sourceIp = "127.0.0.1";
        ConsoleConnectionDetails details = new ConsoleConnectionDetails(sid, null, tag, displayName);
        details.setHost(hostAddress);
        details.setPort(port);

        ConsoleProxyClientParam param = consoleAccessManager.generateConsoleProxyClientParam(details, ticket, sessionUuid, sourceIp, null, vm, host);

        Assert.assertEquals(hostAddress, param.getClientHostAddress());
        Assert.assertEquals(port, param.getClientHostPort());
        Assert.assertEquals(sid, param.getClientHostPassword());
        Assert.assertEquals(tag, param.getClientTag());
        Assert.assertEquals(displayName, param.getClientDisplayName());
        Assert.assertEquals(ticket, param.getTicket());
        Assert.assertEquals(sessionUuid, param.getSessionUuid());
        Assert.assertEquals(sourceIp, param.getSourceIP());
        Assert.assertNull(param.getLocale());
        Assert.assertNull(param.getExtraSecurityToken());
    }

    @Test
    public void generateConsoleProxyClientParamSetsExtraSecurityTokenWhenProvided() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("password", null, null, null);

        ConsoleProxyClientParam param = consoleAccessManager.generateConsoleProxyClientParam(details, "ticket", "sessionUuid", "127.0.0.1", "extraToken", vm, host);

        Assert.assertEquals("extraToken", param.getExtraSecurityToken());
    }

    @Test
    public void generateConsoleProxyClientParamSetsLocaleWhenProvided() {
        HostVO host = Mockito.mock(HostVO.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails(null, "fr-fr", null, null);

        ConsoleProxyClientParam param = consoleAccessManager.generateConsoleProxyClientParam(details, "ticket", "sessionUuid", "127.0.0.1", null, vm, host);

        Assert.assertEquals("fr-fr", param.getLocale());
    }

    @Test
    public void generateConsoleProxyClientParamSetsRdpDetailsForHyperV() {
        long hostId = 1L;
        String username = "admin";
        String password = "adminPass";
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails(null, null, null, null);
        details.setUsingRDP(true);
        String ip = "10.0.0.1";
        Mockito.when(host.getPrivateIpAddress()).thenReturn(ip);
        Mockito.when(managementServer.findDetail(host.getId(), "username")).thenReturn(new DetailVO(hostId, "username", username));
        Mockito.when(managementServer.findDetail(host.getId(), "password")).thenReturn(new DetailVO(hostId, "password", password));

        ConsoleProxyClientParam param = consoleAccessManager.generateConsoleProxyClientParam(details, "ticket", "sessionUuid", "127.0.0.1", null, vm, host);

        Assert.assertEquals(ip, param.getHypervHost());
        Assert.assertEquals(username, param.getUsername());
        Assert.assertEquals(password, param.getPassword());
    }

    @Test
    public void generateConsoleProxyClientParamSetsTunnelDetailsWhenProvided() {
        HostVO host = Mockito.mock(HostVO.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        ConsoleConnectionDetails details = new ConsoleConnectionDetails(null, null, null, null);
        details.setTunnelUrl("tunnelUrl");
        details.setTunnelSession("tunnelSession");

        ConsoleProxyClientParam param = consoleAccessManager.generateConsoleProxyClientParam(details, "ticket", "sessionUuid", "127.0.0.1", null, vm, host);

        Assert.assertEquals("tunnelUrl", param.getClientTunnelUrl());
        Assert.assertEquals("tunnelSession", param.getClientTunnelSession());
    }

    @Test
    public void returnsNullWhenConsoleConnectionDetailsAreNull() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.doReturn(null).when(consoleAccessManager).getConsoleConnectionDetails(vm, host);

        ConsoleEndpoint result = consoleAccessManager.composeConsoleAccessEndpoint("rootUrl", vm, host, "addr", "sessionUuid", "extraToken");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isResult());
        Assert.assertNull(result.getUrl());
        Assert.assertEquals("Console access to this instance cannot be provided", result.getDetails());
    }

    @Test
    public void composeConsoleAccessEndpointReturnsConsoleEndpointWhenConsoleConnectionDetailsAreValid() {
        String locale = "en";
        String hostStr = "192.168.1.100";
        int port = 5900;
        String sid = "SID";
        String sessionUuid = UUID.randomUUID().toString();
        String ticket = UUID.randomUUID().toString();
        String addr = "addr";
        String extraToken = "extraToken";
        String rootUrl = "rootUrl";
        int vncPort = 443;
        long vmId = 100L;
        long hostId = 1L;
        String url = "url";
        String consoleAddress = "127.0.0.1";
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        String tag = UUID.randomUUID().toString();
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("password", locale, tag, null);
        details.setHost(hostStr);
        details.setPort(port);
        details.setSid(sid);
        Mockito.doReturn(details).when(consoleAccessManager).getConsoleConnectionDetails(vm, host);
        Mockito.when(consoleProxyManager.getVncPort(Mockito.anyLong())).thenReturn(vncPort);
        ConsoleProxyPasswordBasedEncryptor.KeyIVPair keyIvPair = new ConsoleProxyPasswordBasedEncryptor.KeyIVPair("key", "iv");
        Mockito.doReturn(GsonHelper.getGson().toJson(keyIvPair)).when(consoleAccessManager).getEncryptorPassword();
        Mockito.doReturn(ticket).when(consoleAccessManager).genAccessTicket(hostStr, String.valueOf(port), sid, tag, sessionUuid);
        ConsoleProxyClientParam param = Mockito.mock(ConsoleProxyClientParam.class);
        Mockito.when(param.getExtraSecurityToken()).thenReturn(extraToken);
        Mockito.doReturn(param).when(consoleAccessManager).generateConsoleProxyClientParam(details, ticket, sessionUuid, addr, extraToken, vm, host);
        Mockito.doReturn(url).when(consoleAccessManager).generateConsoleAccessUrl(Mockito.eq(rootUrl),
                Mockito.eq(param), Mockito.anyString(), Mockito.eq(vncPort), Mockito.eq(vm), Mockito.eq(host),
                Mockito.eq(locale));
        Mockito.doNothing().when(consoleAccessManager).persistConsoleSession(sessionUuid, vmId, hostId, addr);
        Mockito.when(managementServer.getConsoleAccessAddress(vmId)).thenReturn(consoleAddress);

        ConsoleEndpoint endpoint = consoleAccessManager.composeConsoleAccessEndpoint(rootUrl, vm, host, addr, sessionUuid, extraToken);

        Mockito.verify(consoleAccessManager).persistConsoleSession(sessionUuid, vmId, hostId, addr);
        Mockito.verify(managementServer).setConsoleAccessForVm(vmId, sessionUuid);
        Assert.assertEquals(url, endpoint.getUrl());
        Assert.assertEquals(ConsoleAccessManagerImpl.WEB_SOCKET_PATH, endpoint.getWebsocketPath());
        Assert.assertEquals(extraToken, endpoint.getWebsocketExtra());
        Assert.assertEquals(consoleAddress, endpoint.getWebsocketHost());
    }

    @Test
    public void composeConsoleAccessEndpointReturnsWithoutPersistWhenConsoleConnectionDetailsAreValidDirect() {
        String url = "url";
        long vmId = 100L;
        long hostId = 1L;
        String sessionUuid = UUID.randomUUID().toString();
        String addr = "addr";
        ConsoleConnectionDetails details = new ConsoleConnectionDetails("password", "en", "tag", null);
        details.setDirectUrl(url);
        details.setModeFromExternalProtocol("direct");
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.doReturn(details).when(consoleAccessManager).getConsoleConnectionDetails(vm, host);
        Mockito.doNothing().when(consoleAccessManager).persistConsoleSession(sessionUuid, vmId, hostId, addr);

        ConsoleEndpoint endpoint = consoleAccessManager.composeConsoleAccessEndpoint("rootUrl", vm, host, addr, sessionUuid, "");

        Mockito.verify(consoleAccessManager).persistConsoleSession(sessionUuid, vmId, hostId, addr);
        Mockito.verify(managementServer, Mockito.never()).setConsoleAccessForVm(Mockito.anyLong(), Mockito.anyString());
        Assert.assertEquals(url, endpoint.getUrl());
        Assert.assertNull(endpoint.getWebsocketPath());
        Assert.assertNull(endpoint.getWebsocketExtra());
        Assert.assertNull(endpoint.getWebsocketHost());
    }
}
