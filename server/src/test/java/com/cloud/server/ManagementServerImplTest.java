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

import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserData;
import com.cloud.user.UserDataVO;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.userdata.DeleteUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.ListUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.RegisterUserDataCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.userdata.UserDataManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    UserDataManager userDataManager;

    @Spy
    ManagementServerImpl spy = new ManagementServerImpl();

    @Mock
    UserVmDetailsDao userVmDetailsDao;

    @Mock
    HostDetailsDao hostDetailsDao;
    private AutoCloseable closeable;

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException {
        closeable = MockitoAnnotations.openMocks(this);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        spy._accountMgr = _accountMgr;
        spy.userDataDao = _userDataDao;
        spy.templateDao = _templateDao;
        spy._userVmDao = _userVmDao;
        spy.annotationDao = annotationDao;
        spy._UserVmDetailsDao = userVmDetailsDao;
        spy._detailsDao = hostDetailsDao;
        spy.userDataManager = userDataManager;
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        closeable.close();
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
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
        overrideDefaultConfigValue(ipAddressManagerImpl.SystemVmPublicIpReservationModeStrictness, "_defaultValue", "true");

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
    public void setParametersTestWhenStateIsFreeAndSystemVmPublicIsFalse() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ipAddressManagerImpl.SystemVmPublicIpReservationModeStrictness, "_defaultValue", "false");
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
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsFalse() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ipAddressManagerImpl.SystemVmPublicIpReservationModeStrictness, "_defaultValue", "false");
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
        Mockito.verify(sc, Mockito.times(1)).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsTrue() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ipAddressManagerImpl.SystemVmPublicIpReservationModeStrictness, "_defaultValue", "true");
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
        Mockito.verify(sc, Mockito.times(1)).setParameters("forsystemvms", false);
    }

    @Test
    public void testSuccessfulRegisterUserdata() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
            when(account.getAccountId()).thenReturn(1L);
            when(account.getDomainId()).thenReturn(2L);
            when(callContextMock.getCallingAccount()).thenReturn(account);
            when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

            String testUserData = "testUserdata";
            RegisterUserDataCmd cmd = Mockito.mock(RegisterUserDataCmd.class);
            when(cmd.getUserData()).thenReturn(testUserData);
            when(cmd.getName()).thenReturn("testName");
            when(cmd.getHttpMethod()).thenReturn(BaseCmd.HTTPMethod.GET);

            when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(null);
            when(_userDataDao.findByUserData(account.getAccountId(), account.getDomainId(), testUserData)).thenReturn(null);
            when(userDataManager.validateUserData(testUserData, BaseCmd.HTTPMethod.GET)).thenReturn(testUserData);

            UserData userData = spy.registerUserData(cmd);
            Assert.assertEquals("testName", userData.getName());
            Assert.assertEquals("testUserdata", userData.getUserData());
            Assert.assertEquals(1L, userData.getAccountId());
            Assert.assertEquals(2L, userData.getDomainId());
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRegisterExistingUserdata() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
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
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRegisterExistingName() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
            when(account.getAccountId()).thenReturn(1L);
            when(account.getDomainId()).thenReturn(2L);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

            RegisterUserDataCmd cmd = Mockito.mock(RegisterUserDataCmd.class);
            when(cmd.getName()).thenReturn("testName");

            UserDataVO userData = Mockito.mock(UserDataVO.class);
            when(_userDataDao.findByName(account.getAccountId(), account.getDomainId(), "testName")).thenReturn(userData);

            spy.registerUserData(cmd);
        }
    }

    @Test
    public void testSuccessfulDeleteUserdata() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
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
            when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());
            when(_userVmDao.findByUserDataId(1L)).thenReturn(new ArrayList<UserVmVO>());
            when(_userDataDao.remove(1L)).thenReturn(true);

            boolean result = spy.deleteUserData(cmd);
            Assert.assertEquals(true, result);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteUserdataLinkedToTemplate() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
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

            VMTemplateVO vmTemplateVO = Mockito.mock(VMTemplateVO.class);
            List<VMTemplateVO> linkedTemplates = new ArrayList<>();
            linkedTemplates.add(vmTemplateVO);
            when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(linkedTemplates);

            spy.deleteUserData(cmd);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteUserdataUsedByVM() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
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

            when(_templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());

            UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
            List<UserVmVO> vms = new ArrayList<>();
            vms.add(userVmVO);
            when(_userVmDao.findByUserDataId(1L)).thenReturn(vms);

            spy.deleteUserData(cmd);
        }
    }

    @Test
    public void testListUserDataById() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);;

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
    }

    @Test
    public void testListUserDataByName() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
            when(callContextMock.getCallingAccount()).thenReturn(account);

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
    }

    @Test
    public void testListUserDataByKeyword() {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            when(CallContext.current()).thenReturn(callContextMock);
            when(callContextMock.getCallingAccount()).thenReturn(account);

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

    private UserVmVO mockFilterUefiHostsTestVm(String uefiValue) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        if (uefiValue == null) {
            Mockito.when(userVmDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(null);
        } else {
            UserVmDetailVO detail = new UserVmDetailVO(vm.getId(), ApiConstants.BootType.UEFI.toString(), uefiValue, true);
            Mockito.when(userVmDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(detail);
            Mockito.when(hostDetailsDao.findByName(Host.HOST_UEFI_ENABLE)).thenReturn(new ArrayList<>(List.of(new DetailVO(1l, Host.HOST_UEFI_ENABLE, "true"), new DetailVO(2l, Host.HOST_UEFI_ENABLE, "false"))));
        }
        return vm;
    }

    private List<HostVO> mockHostsList(Long filtered) {
        List<HostVO> hosts = new ArrayList<>();
        HostVO h1 = new HostVO("guid-1");
        ReflectionTestUtils.setField(h1, "id", 1L);
        hosts.add(h1);
        HostVO h2 = new HostVO("guid-2");
        ReflectionTestUtils.setField(h2, "id", 2L);
        hosts.add(h2);
        HostVO h3 = new HostVO("guid-3");
        ReflectionTestUtils.setField(h3, "id", 3L);
        hosts.add(h3);
        if (filtered != null) {
            hosts.removeIf(h -> h.getId() == filtered);
        }
        return hosts;
    }

    @Test
    public void testFilterUefiHostsForMigrationBiosVm() {
        UserVmVO vm = mockFilterUefiHostsTestVm(null);
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), new ArrayList<>(), vm);
        Assert.assertTrue(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationBiosVmFiltered() {
        List<HostVO> filteredHosts = mockHostsList(2L);
        UserVmVO vm = mockFilterUefiHostsTestVm(null);
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), filteredHosts, vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(2, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiInvalidValueVm() {
        UserVmVO vm = mockFilterUefiHostsTestVm("");
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), new ArrayList<>(), vm);
        Assert.assertTrue(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsUnavailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        List<HostVO> filteredHosts = new ArrayList<>();
        Mockito.when(hostDetailsDao.findByName(Host.HOST_UEFI_ENABLE)).thenReturn(new ArrayList<>());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), filteredHosts, vm);
        Assert.assertFalse(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsAvailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.LEGACY.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), null, vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(1, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationNoHostsAvailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), null, vm);
        Assert.assertFalse(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsAvailableFiltered() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.LEGACY.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), mockHostsList(3L), vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(1, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMNoHostsAvailableFiltered() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), mockHostsList(1L), vm);
        Assert.assertFalse(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(0, result.second().size());
    }

    @Test
    public void testZoneWideVolumeRequiresStorageMotionNonManaged() {
        PrimaryDataStore dataStore = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(dataStore.isManaged()).thenReturn(false);
        Assert.assertFalse(spy.zoneWideVolumeRequiresStorageMotion(dataStore,
                Mockito.mock(Host.class), Mockito.mock(Host.class)));
    }

    @Test
    public void testZoneWideVolumeRequiresStorageMotionSameClusterHost() {
        PrimaryDataStore dataStore = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(dataStore.isManaged()).thenReturn(true);
        Host host1 = Mockito.mock(Host.class);
        Mockito.when(host1.getClusterId()).thenReturn(1L);
        Host host2 = Mockito.mock(Host.class);
        Mockito.when(host2.getClusterId()).thenReturn(1L);
        Assert.assertFalse(spy.zoneWideVolumeRequiresStorageMotion(dataStore, host1, host2));
    }

    @Test
    public void testZoneWideVolumeRequiresStorageMotionDriverDependent() {
        PrimaryDataStore dataStore = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(dataStore.isManaged()).thenReturn(true);
        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(dataStore.getDriver()).thenReturn(driver);
        Host host1 = Mockito.mock(Host.class);
        Mockito.when(host1.getClusterId()).thenReturn(1L);
        Host host2 = Mockito.mock(Host.class);
        Mockito.when(host2.getClusterId()).thenReturn(2L);

        Mockito.when(driver.zoneWideVolumesAvailableWithoutClusterMotion()).thenReturn(true);
        Assert.assertFalse(spy.zoneWideVolumeRequiresStorageMotion(dataStore, host1, host2));

        Mockito.when(driver.zoneWideVolumesAvailableWithoutClusterMotion()).thenReturn(false);
        Assert.assertTrue(spy.zoneWideVolumeRequiresStorageMotion(dataStore, host1, host2));
    }
}
