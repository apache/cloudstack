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

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserData;
import com.cloud.user.UserDataVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.userdata.DeleteUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.ListUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.RegisterUserDataCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.utils.db.SearchCriteria;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
public class ManagementServerImplTest {

    @Mock
    SearchCriteria<IPAddressVO> sc;

    @Mock
    RegisterSSHKeyPairCmd regCmd;

    @Mock
    SSHKeyPairVO existingPair;

    @Mock
    Account account;

    @Mock
    SSHKeyPairDao sshKeyPairDao;

    @Mock
    SSHKeyPair sshKeyPair;

    @Mock
    IpAddressManagerImpl ipAddressManagerImpl;

    @Mock
    AccountManager _accountMgr;

    @Mock
    UserDataDao _userDataDao;

    @Mock
    VMTemplateDao _templateDao;

    @Mock
    AnnotationDao annotationDao;

    @Mock
    UserVmDao _userVmDao;

    @Spy
    ManagementServerImpl spy = new ManagementServerImpl();

    ConfigKey mockConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        mockConfig = Mockito.mock(ConfigKey.class);
        Whitebox.setInternalState(ipAddressManagerImpl.getClass(), "SystemVmPublicIpReservationModeStrictness", mockConfig);
        spy._accountMgr = _accountMgr;
        spy.userDataDao = _userDataDao;
        spy.templateDao = _templateDao;
        spy._userVmDao = _userVmDao;
        spy.annotationDao = annotationDao;
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDuplicateRegistraitons(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = spy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(account).when(spy).getCaller();
        Mockito.lenient().doReturn(account).when(spy).getOwner(regCmd);

        Mockito.doNothing().when(spy).checkForKeyByName(regCmd, account);
        Mockito.lenient().doReturn(accountName).when(regCmd).getAccountName();

        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn("name").when(regCmd).getName();

        spy._sshKeyPairDao = sshKeyPairDao;
        Mockito.doReturn(1L).when(account).getAccountId();
        Mockito.doReturn(1L).when(account).getDomainId();
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        lenient().when(sshKeyPairDao.findByName(1L, 1L, "name")).thenReturn(null).thenReturn(null);
        when(sshKeyPairDao.findByPublicKey(1L, 1L, publicKeyMaterial)).thenReturn(null).thenReturn(existingPair);

        spy.registerSSHKeyPair(regCmd);
        spy.registerSSHKeyPair(regCmd);
    }
    @Test
    public void testSuccess(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = spy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(1L).when(account).getAccountId();
        Mockito.doReturn(1L).when(account).getAccountId();
        spy._sshKeyPairDao = sshKeyPairDao;


        //Mocking the DAO object functions - NO object found in DB
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByPublicKey(1L, 1L,publicKeyMaterial);
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByName(1L, 1L, accountName);
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        //Mocking the User Params
        Mockito.doReturn(accountName).when(regCmd).getName();
        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn(account).when(spy).getOwner(regCmd);

        spy.registerSSHKeyPair(regCmd);
        Mockito.verify(spy, Mockito.times(3)).getPublicKeyFromKeyKeyMaterial(anyString());
    }

    @Test
    public void setParametersTestWhenStateIsFreeAndSystemVmPublicIsTrue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.TRUE);

        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(IpAddress.State.Free.name());
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", "Free");
        Mockito.verify(sc, Mockito.times(1)).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsFreeAndSystemVmPublicIsFalse() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.FALSE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(IpAddress.State.Free.name());
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", "Free");
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsFalse() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.FALSE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsTrue() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.TRUE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    @Test
    public void testSuccessfulRegisterUserdata() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        RegisterUserDataCmd cmd = Mockito.mock(RegisterUserDataCmd.class);
        when(cmd.getUserData()).thenReturn("testUserdata");
        when(cmd.getName()).thenReturn("testName");
        when(cmd.getHttpMethod()).thenReturn(BaseCmd.HTTPMethod.GET);

        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);
        when(_userDataDao.findByUserData(account.getAccountId(), account.getDomainId(), "testUserdata")).thenReturn(null);

        UserData userData = spy.registerUserData(cmd);
        Assert.assertEquals("testName", userData.getName());
        Assert.assertEquals("testUserdata", userData.getUserData());
        Assert.assertEquals(1L, userData.getAccountId());
        Assert.assertEquals(2L, userData.getDomainId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRegisterExistingUserdata() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        RegisterUserDataCmd cmd = Mockito.mock(RegisterUserDataCmd.class);
        when(cmd.getUserData()).thenReturn("testUserdata");
        when(cmd.getName()).thenReturn("testName");

        UserDataVO userData = Mockito.mock(UserDataVO.class);
        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);
        when(_userDataDao.findByUserData(account.getAccountId(), account.getDomainId(), "testUserdata")).thenReturn(userData);

        spy.registerUserData(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRegisterExistingName() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        PowerMockito.when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        RegisterUserDataCmd cmd = Mockito.mock(RegisterUserDataCmd.class);
        when(cmd.getUserData()).thenReturn("testUserdata");
        when(cmd.getName()).thenReturn("testName");

        UserDataVO userData = Mockito.mock(UserDataVO.class);
        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(userData);

        spy.registerUserData(cmd);
    }

    @Test
    public void testSuccessfulDeleteUserdata() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        DeleteUserDataCmd cmd = Mockito.mock(DeleteUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getId()).thenReturn(1L);
        UserDataVO userData = Mockito.mock(UserDataVO.class);

        Mockito.when(userData.getId()).thenReturn(1L);
        when(_userDataDao.findById(1L)).thenReturn(userData);
        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);
        when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());
        when(_userVmDao.findByUserDataId(1L)).thenReturn(new ArrayList<UserVmVO>());
        when(_userDataDao.remove(1L)).thenReturn(true);

        boolean result = spy.deleteUserData(cmd);
        Assert.assertEquals(true, result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteUserdataLinkedToTemplate() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        DeleteUserDataCmd cmd = Mockito.mock(DeleteUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getId()).thenReturn(1L);

        UserDataVO userData = Mockito.mock(UserDataVO.class);
        Mockito.when(userData.getId()).thenReturn(1L);
        when(_userDataDao.findById(1L)).thenReturn(userData);
        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);

        VMTemplateVO vmTemplateVO = Mockito.mock(VMTemplateVO.class);
        List<VMTemplateVO> linkedTemplates = new ArrayList<>();
        linkedTemplates.add(vmTemplateVO);
        when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(linkedTemplates);

        spy.deleteUserData(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteUserdataUsedByVM() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        DeleteUserDataCmd cmd = Mockito.mock(DeleteUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getId()).thenReturn(1L);

        UserDataVO userData = Mockito.mock(UserDataVO.class);
        Mockito.when(userData.getId()).thenReturn(1L);
        when(_userDataDao.findById(1L)).thenReturn(userData);
        when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);

        when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());

        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        List<UserVmVO> vms = new ArrayList<>();
        vms.add(userVmVO);
        when(_userVmDao.findByUserDataId(1L)).thenReturn(vms);

        spy.deleteUserData(cmd);
    }

    @Test
    public void testListUserDataById() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        ListUserDataCmd cmd = Mockito.mock(ListUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getId()).thenReturn(1L);
        when(cmd.isRecursive()).thenReturn(false);
        UserDataVO userData = Mockito.mock(UserDataVO.class);

        SearchBuilder<UserDataVO> sb = Mockito.mock(SearchBuilder.class);
        when(_userDataDao.createSearchBuilder()).thenReturn(sb);
        when(sb.entity()).thenReturn(userData);

        SearchCriteria<UserDataVO> sc = Mockito.mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);

        List<UserDataVO> userDataList = new ArrayList<UserDataVO>();
        userDataList.add(userData);
        Pair<List<UserDataVO>, Integer> result = new Pair(userDataList, 1);
        when(_userDataDao.searchAndCount(nullable(SearchCriteria.class), nullable(Filter.class))).thenReturn(result);

        Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd);

        Assert.assertEquals(userdataResultList.first().get(0), userDataList.get(0));
    }

    @Test
    public void testListUserDataByName() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        ListUserDataCmd cmd = Mockito.mock(ListUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getName()).thenReturn("testSearchUserdataName");
        when(cmd.isRecursive()).thenReturn(false);
        UserDataVO userData = Mockito.mock(UserDataVO.class);

        SearchBuilder<UserDataVO> sb = Mockito.mock(SearchBuilder.class);
        when(_userDataDao.createSearchBuilder()).thenReturn(sb);
        when(sb.entity()).thenReturn(userData);

        SearchCriteria<UserDataVO> sc = Mockito.mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);

        List<UserDataVO> userDataList = new ArrayList<UserDataVO>();
        userDataList.add(userData);
        Pair<List<UserDataVO>, Integer> result = new Pair(userDataList, 1);
        when(_userDataDao.searchAndCount(nullable(SearchCriteria.class), nullable(Filter.class))).thenReturn(result);

        Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd);

        Assert.assertEquals(userdataResultList.first().get(0), userDataList.get(0));
    }

    @Test
    public void testListUserDataByKeyword() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        when(CallContext.current()).thenReturn(callContextMock);
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(2L);
        when(callContextMock.getCallingAccount()).thenReturn(account);
        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        ListUserDataCmd cmd = Mockito.mock(ListUserDataCmd.class);
        when(cmd.getAccountName()).thenReturn("testAccountName");
        when(cmd.getDomainId()).thenReturn(1L);
        when(cmd.getProjectId()).thenReturn(2L);
        when(cmd.getKeyword()).thenReturn("testSearchUserdataKeyword");
        when(cmd.isRecursive()).thenReturn(false);
        UserDataVO userData = Mockito.mock(UserDataVO.class);

        SearchBuilder<UserDataVO> sb = Mockito.mock(SearchBuilder.class);
        when(_userDataDao.createSearchBuilder()).thenReturn(sb);
        when(sb.entity()).thenReturn(userData);

        SearchCriteria<UserDataVO> sc = Mockito.mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);

        List<UserDataVO> userDataList = new ArrayList<UserDataVO>();
        userDataList.add(userData);
        Pair<List<UserDataVO>, Integer> result = new Pair(userDataList, 1);
        when(_userDataDao.searchAndCount(nullable(SearchCriteria.class), nullable(Filter.class))).thenReturn(result);

        Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd);

        Assert.assertEquals(userdataResultList.first().get(0), userDataList.get(0));
    }

}
