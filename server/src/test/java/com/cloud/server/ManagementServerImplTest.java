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

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.SSHKeyPairDao;

@RunWith(MockitoJUnitRunner.class)
public class ManagementServerImplTest {

    @Mock
    RegisterSSHKeyPairCmd regCmd;

    @Mock
    SSHKeyPairVO existingPair;

    @Mock
    Account accountMock;

    @Mock
    SSHKeyPairDao sshKeyPairDao;
    ManagementServerImpl ms = new ManagementServerImpl();

    @Mock
    SSHKeyPair sshKeyPair;

    @Spy
    ManagementServerImpl managementServerImplSpy;

    @Mock
    VlanDao vlanDaoMock;

    @Mock
    VlanVO vlanVoMock1, vlanVoMock2;

    @Before
    public void setup() {
        managementServerImplSpy._vlanDao = vlanDaoMock;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDuplicateRegistraitons(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = managementServerImplSpy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(accountMock).when(managementServerImplSpy).getCaller();
        Mockito.lenient().doReturn(accountMock).when(managementServerImplSpy).getOwner(regCmd);

        Mockito.doNothing().when(managementServerImplSpy).checkForKeyByName(regCmd, accountMock);
        Mockito.lenient().doReturn(accountName).when(regCmd).getAccountName();

        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn("name").when(regCmd).getName();

        managementServerImplSpy._sshKeyPairDao = sshKeyPairDao;
        Mockito.doReturn(1L).when(accountMock).getAccountId();
        Mockito.doReturn(1L).when(accountMock).getDomainId();
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        lenient().when(sshKeyPairDao.findByName(1L, 1L, "name")).thenReturn(null).thenReturn(null);
        when(sshKeyPairDao.findByPublicKey(1L, 1L, publicKeyMaterial)).thenReturn(null).thenReturn(existingPair);

        managementServerImplSpy.registerSSHKeyPair(regCmd);
        managementServerImplSpy.registerSSHKeyPair(regCmd);
    }
    @Test
    public void testSuccess(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = managementServerImplSpy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(1L).when(accountMock).getAccountId();
        Mockito.doReturn(1L).when(accountMock).getAccountId();
        managementServerImplSpy._sshKeyPairDao = sshKeyPairDao;


        //Mocking the DAO object functions - NO object found in DB
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByPublicKey(1L, 1L,publicKeyMaterial);
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByName(1L, 1L, accountName);
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        //Mocking the User Params
        Mockito.doReturn(accountName).when(regCmd).getName();
        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn(accountMock).when(managementServerImplSpy).getOwner(regCmd);

        managementServerImplSpy.registerSSHKeyPair(regCmd);
        Mockito.verify(managementServerImplSpy, Mockito.times(3)).getPublicKeyFromKeyKeyMaterial(anyString());
    }

    @Test
    public void validateRetrieveDedicatedVlans() {
        List<VlanVO> expectedResult = Arrays.asList(vlanVoMock1);

        Mockito.doReturn(expectedResult).when(vlanDaoMock).listDedicatedVlans(Mockito.anyLong());

        List<VlanVO> result = managementServerImplSpy.retrieveDedicatedVlans(0, "");

        Assert.assertEquals(expectedResult, result);
        Mockito.verify(vlanDaoMock).listDedicatedVlans(Mockito.anyLong());
    }

    @Test
    public void validateRetrieveZoneWideNonDedicatedVlansAccountWithoutAccessToPublicIps() {
        List<VlanVO> expectedResult = new ArrayList<>();

        Mockito.doReturn(false).when(managementServerImplSpy).getUseSystemPublicIpsValueIn(Mockito.anyLong());

        List<VlanVO> result = managementServerImplSpy.retrieveZoneWideNonDedicatedVlans(0, "", 0);

        Assert.assertEquals(expectedResult, result);
        Mockito.verify(vlanDaoMock, never()).listZoneWideNonDedicatedVlans(Mockito.anyLong());
    }

    @Test
    public void validateRetrieveZoneWideNonDedicatedVlansAccountWithAccessToPublicIps() {
        List<VlanVO> expectedResult = Arrays.asList(vlanVoMock2);

        Mockito.doReturn(true).when(managementServerImplSpy).getUseSystemPublicIpsValueIn(Mockito.anyLong());
        Mockito.doReturn(expectedResult).when(vlanDaoMock).listZoneWideNonDedicatedVlans(Mockito.anyLong());

        List<VlanVO> result = managementServerImplSpy.retrieveZoneWideNonDedicatedVlans(0, "", 0);

        Assert.assertEquals(expectedResult, result);
        Mockito.verify(vlanDaoMock).listZoneWideNonDedicatedVlans(Mockito.anyLong());
    }

    @Test
    public void validateRetrieveAvailableVlanDbIdsForAccountWithNonDedicatedAndDedicatedVlans() {
        List<Long> expectedResult = Arrays.asList(1l, 2l);
        Mockito.doReturn(expectedResult.get(0)).when(vlanVoMock1).getId();
        Mockito.doReturn(expectedResult.get(1)).when(vlanVoMock2).getId();
        Mockito.doReturn(1l).when(accountMock).getId();

        Mockito.doReturn(Arrays.asList(vlanVoMock1)).when(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(Arrays.asList(vlanVoMock2)).when(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());

        List<Long> result = managementServerImplSpy.retrieveAvailableVlanDbIdsForAccount(1l, accountMock);

        Mockito.verify(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());
        assertArrayEquals(expectedResult.toArray(), result.toArray());
    }

    @Test
    public void validateRetrieveAvailableVlanDbIdsForAccountWithNonDedicatedAndWithoutDedicatedVlans() {
        List<Long> expectedResult = Arrays.asList(1l);
        Mockito.doReturn(expectedResult.get(0)).when(vlanVoMock1).getId();
        Mockito.doReturn(1l).when(accountMock).getId();

        Mockito.doReturn(Arrays.asList(vlanVoMock1)).when(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(new ArrayList<>()).when(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());

        List<Long> result = managementServerImplSpy.retrieveAvailableVlanDbIdsForAccount(1l, accountMock);

        Mockito.verify(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());
        assertArrayEquals(expectedResult.toArray(), result.toArray());
    }

    @Test
    public void validateRetrieveAvailableVlanDbIdsForAccountWithoutNonDedicatedAndWithDedicatedVlans() {
        List<Long> expectedResult = Arrays.asList(2l);
        Mockito.doReturn(expectedResult.get(0)).when(vlanVoMock2).getId();
        Mockito.doReturn(1l).when(accountMock).getId();

        Mockito.doReturn(new ArrayList<>()).when(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(Arrays.asList(vlanVoMock2)).when(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());

        List<Long> result = managementServerImplSpy.retrieveAvailableVlanDbIdsForAccount(1l, accountMock);

        Mockito.verify(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());
        assertArrayEquals(expectedResult.toArray(), result.toArray());
    }

    @Test
    public void validateRetrieveAvailableVlanDbIdsForAccountWithoutNonDedicatedAndDedicatedVlans() {
        List<Long> expectedResult = Arrays.asList(-1l);
        Mockito.doReturn(1l).when(accountMock).getId();

        Mockito.doReturn(new ArrayList<>()).when(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(new ArrayList<>()).when(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());

        List<Long> result = managementServerImplSpy.retrieveAvailableVlanDbIdsForAccount(1l, accountMock);

        Mockito.verify(managementServerImplSpy).retrieveZoneWideNonDedicatedVlans(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(managementServerImplSpy).retrieveDedicatedVlans(Mockito.anyLong(), Mockito.anyString());
        assertArrayEquals(expectedResult.toArray(), result.toArray());
    }
}
