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
package com.cloud.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.SSHKeyPairDao;

@RunWith(MockitoJUnitRunner.class)
public class ManagementServerImplTest {

    @Mock
    RegisterSSHKeyPairCmd regCmd;
    @Mock
    SSHKeyPairVO existingPair;
    @Mock
    Account account;
    @Mock
    SSHKeyPairDao sshKeyPairDao;
    ManagementServerImpl ms = new ManagementServerImpl();
    @Spy
    ManagementServerImpl spy;

    @Test(expected = InvalidParameterValueException.class)
    public void testExistingPairRegistration() {
        String accountName = "account";
        String publicKeyString = "very public";
        // setup owner with domainid
        Mockito.doReturn(account).when(spy).getCaller();
        Mockito.doReturn(account).when(spy).getOwner(regCmd);
        // mock _sshKeyPairDao.findByName to return null
        Mockito.doNothing().when(spy).checkForKeyByName(regCmd, account);
        // mock _sshKeyPairDao.findByPublicKey to return a known object
        Mockito.doReturn(accountName).when(regCmd).getAccountName();
        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn("name").when(regCmd).getName();
        spy._sshKeyPairDao = sshKeyPairDao;
        Mockito.doReturn(1L).when(account).getAccountId();
        Mockito.doReturn(1L).when(account).getDomainId();
        Mockito.doReturn(existingPair).when(sshKeyPairDao).findByPublicKey(1L, 1L, publicKeyString);
        spy.registerSSHKeyPair(regCmd);
    }
}
