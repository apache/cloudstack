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
import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private UserVmDetailsDao userVmDetailsDao;
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
}
