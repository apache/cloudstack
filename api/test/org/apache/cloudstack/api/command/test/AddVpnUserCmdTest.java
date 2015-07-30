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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vpn.AddVpnUserCmd;

import com.cloud.network.VpnUser;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

public class AddVpnUserCmdTest extends TestCase {

    private AddVpnUserCmd addVpnUserCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
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

    @Test
    public void testCreateSuccess() {

        AccountService accountService = Mockito.mock(AccountService.class);

        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(Matchers.anyLong())).thenReturn(account);

        addVpnUserCmd._accountService = accountService;

        RemoteAccessVpnService ravService = Mockito.mock(RemoteAccessVpnService.class);

        VpnUser vpnUser = Mockito.mock(VpnUser.class);
        Mockito.when(ravService.addVpnUser(Matchers.anyLong(), Matchers.anyString(), Matchers.anyString())).thenReturn(vpnUser);

        addVpnUserCmd._ravService = ravService;

        addVpnUserCmd.create();

    }

    @Test
    public void testCreateFailure() {

        AccountService accountService = Mockito.mock(AccountService.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(accountService.getAccount(Matchers.anyLong())).thenReturn(account);

        addVpnUserCmd._accountService = accountService;

        RemoteAccessVpnService ravService = Mockito.mock(RemoteAccessVpnService.class);
        Mockito.when(ravService.addVpnUser(Matchers.anyLong(), Matchers.anyString(), Matchers.anyString())).thenReturn(null);

        addVpnUserCmd._ravService = ravService;

        try {
            addVpnUserCmd.create();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add vpn user", exception.getDescription());
        }

    }

}
