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
package org.apache.cloudstack.api.command.test;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vpn.AddVpnUserCmd;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.cloud.network.VpnUser;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

public class AddVpnUserCmdTest extends TestCase {

    private AddVpnUserCmd addVpnUserCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {

        addVpnUserCmd = new AddVpnUserCmd() {

            @Override
            public Long getEntityId() {
                return 2L;
            }

            @Override
            public long getEntityOwnerId() {
                return 2L;
            }

            @Override
            public String getUserName() {
                return "User Name";
            }

            @Override
            public String getPassword() {
                return "password";
            }

        };
    }

    /*
     * @Test public void testExecuteVpnUserNotFound() {
     * 
     * EntityManager entityManager = Mockito.mock(EntityManager.class);
     * 
     * Mockito.when(entityManager.findById(VpnUser.class,
     * Mockito.anyLong())).thenReturn(null);
     * 
     * addVpnUserCmd._entityMgr = entityManager; try { addVpnUserCmd.execute();
     * } catch (Exception e) { }
     * 
     * }
     * 
     * 
     * @Test public void testExecuteVpnUserFound() {
     * 
     * EntityManager entityManager = Mockito.mock(EntityManager.class);
     * addVpnUserCmd._entityMgr = entityManager;
     * 
     * VpnUser vpnUser = Mockito.mock(VpnUser.class);
     * Mockito.when(entityManager.findById(VpnUser.class,
     * Mockito.anyLong())).thenReturn(vpnUser); addVpnUserCmd.execute();
     * 
     * }
     */

    @Test
    public void testCreateSuccess() {

        AccountService accountService = Mockito.mock(AccountService.class);

        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(
                account);

        addVpnUserCmd._accountService = accountService;

        RemoteAccessVpnService ravService = Mockito
                .mock(RemoteAccessVpnService.class);

        VpnUser vpnUser = Mockito.mock(VpnUser.class);
        Mockito.when(
                ravService.addVpnUser(Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(vpnUser);

        addVpnUserCmd._ravService = ravService;

        addVpnUserCmd.create();

    }

    @Test
    public void testCreateFailure() {

        AccountService accountService = Mockito.mock(AccountService.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(
                account);

        addVpnUserCmd._accountService = accountService;

        RemoteAccessVpnService ravService = Mockito
                .mock(RemoteAccessVpnService.class);
        Mockito.when(
                ravService.addVpnUser(Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(null);

        addVpnUserCmd._ravService = ravService;

        try {
            addVpnUserCmd.create();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add vpn user",
                    exception.getDescription());
        }

    }

}
