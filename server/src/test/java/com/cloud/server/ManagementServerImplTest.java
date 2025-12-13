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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.api.command.admin.guest.AddGuestOsCategoryCmd;
import org.apache.cloudstack.api.command.admin.guest.DeleteGuestOsCategoryCmd;
import org.apache.cloudstack.api.command.admin.guest.UpdateGuestOsCategoryCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.guest.ListGuestOsCategoriesCmd;
import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.api.command.user.userdata.DeleteUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.ListUserDataCmd;
import org.apache.cloudstack.api.command.user.userdata.RegisterUserDataCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.userdata.UserDataManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.cpu.CPU;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

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
    VMTemplateDao templateDao;

    @Mock
    AnnotationDao annotationDao;

    @Mock
    UserVmDao _userVmDao;

    @Mock
    UserDataManager userDataManager;

    @Mock
    VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    HostDetailsDao hostDetailsDao;

    @Mock
    ConfigurationDao configDao;

    @Mock
    ConfigDepot configDepot;

    @Mock
    DomainDao domainDao;

    @Mock
    GuestOSCategoryDao _guestOSCategoryDao;

    @Mock
    GuestOSDao _guestOSDao;

    @Mock
    ExtensionsManager extensionManager;

    @Spy
    @InjectMocks
    ManagementServerImpl spy = new ManagementServerImpl();

    private AutoCloseable closeable;

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException {
        closeable = MockitoAnnotations.openMocks(this);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
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
        Mockito.when(cmd.getTags()).thenReturn(null);
        List<IpAddress.State> states = Collections.singletonList(IpAddress.State.Free);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE, states);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", states.toArray());
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
        Mockito.when(cmd.getTags()).thenReturn(null);
        List<IpAddress.State> states = Collections.singletonList(IpAddress.State.Free);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE, states);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", states.toArray());
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
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE, Collections.emptyList());

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", IpAddress.State.Allocated);
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
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE, Collections.emptyList());

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", IpAddress.State.Allocated);
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
            when(templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());
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
            when(templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(linkedTemplates);

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

            when(templateDao.findTemplatesLinkedToUserdata(1L)).thenReturn(new ArrayList<VMTemplateVO>());

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

            Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd, false);

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

            Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd, false);

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

            Pair<List<? extends UserData>, Integer> userdataResultList = spy.listUserDatas(cmd, false);

            Assert.assertEquals(userdataResultList.first().get(0), userDataList.get(0));
        }
    }

    private UserVmVO mockFilterUefiHostsTestVm(String uefiValue) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        if (uefiValue == null) {
            Mockito.when(vmInstanceDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(null);
        } else {
            VMInstanceDetailVO detail = new VMInstanceDetailVO(vm.getId(), ApiConstants.BootType.UEFI.toString(), uefiValue, true);
            Mockito.when(vmInstanceDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(detail);
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

    @Test(expected = InvalidParameterValueException.class)
    public void testSearchForConfigurationsMultipleIds() {
        ListCfgsByCmd cmd = Mockito.mock(ListCfgsByCmd.class);
        Mockito.when(cmd.getConfigName()).thenReturn("pool.storage.capacity.disablethreshold");
        Mockito.when(cmd.getZoneId()).thenReturn(1L);
        Mockito.when(cmd.getStoragepoolId()).thenReturn(2L);
        spy.searchForConfigurations(cmd);
    }

    @Test
    public void testSearchForConfigurations() {
        Long poolId = 1L;
        ListCfgsByCmd cmd = Mockito.mock(ListCfgsByCmd.class);
        Mockito.when(cmd.getConfigName()).thenReturn("pool.storage.capacity.disablethreshold");
        Mockito.when(cmd.getStoragepoolId()).thenReturn(poolId);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getAccountId()).thenReturn(null);
        Mockito.when(cmd.getDomainId()).thenReturn(null);
        Mockito.when(cmd.getImageStoreId()).thenReturn(null);

        SearchCriteria<ConfigurationVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(configDao.createSearchCriteria()).thenReturn(sc);
        ConfigurationVO cfg = new ConfigurationVO("Advanced", "DEFAULT", "test", "pool.storage.capacity.disablethreshold", null, "description");
        Mockito.when(configDao.searchAndCount(any(), any())).thenReturn(new Pair<>(List.of(cfg), 1));
        Mockito.when(configDao.findByName("pool.storage.capacity.disablethreshold")).thenReturn(cfg);

        ConfigKey storageDisableThreshold = new ConfigKey<>(ConfigKey.CATEGORY_ALERT, Double.class, "pool.storage.capacity.disablethreshold", "0.85",
                "Percentage (as a value between 0 and 1) of storage utilization above which allocators will disable using the pool for low storage available.",
                true, List.of(ConfigKey.Scope.StoragePool, ConfigKey.Scope.Zone));
        when(configDepot.get("pool.storage.capacity.disablethreshold")).thenReturn(storageDisableThreshold);

        Pair<List<? extends Configuration>, Integer> result = spy.searchForConfigurations(cmd);

        Assert.assertEquals("0.85", result.first().get(0).getValue());
    }
    @Test
    public void testAddGuestOsCategory() {
        AddGuestOsCategoryCmd addCmd = Mockito.mock(AddGuestOsCategoryCmd.class);
        String name = "Ubuntu";
        boolean featured = true;
        Mockito.when(addCmd.getName()).thenReturn(name);
        Mockito.when(addCmd.isFeatured()).thenReturn(featured);
        Mockito.doAnswer((Answer<GuestOSCategoryVO>) invocation -> (GuestOSCategoryVO)invocation.getArguments()[0]).when(_guestOSCategoryDao).persist(Mockito.any(GuestOSCategoryVO.class));
        GuestOsCategory result = spy.addGuestOsCategory(addCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(featured, result.isFeatured());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).persist(any(GuestOSCategoryVO.class));
    }

    @Test
    public void testUpdateGuestOsCategory() {
        UpdateGuestOsCategoryCmd updateCmd = Mockito.mock(UpdateGuestOsCategoryCmd.class);
        GuestOSCategoryVO guestOSCategory = new GuestOSCategoryVO("Old name", false);
        long id = 1L;
        String name = "Updated Name";
        Boolean featured = true;
        Integer sortKey = 10;
        Mockito.when(updateCmd.getId()).thenReturn(id);
        Mockito.when(updateCmd.getName()).thenReturn(name);
        Mockito.when(updateCmd.isFeatured()).thenReturn(featured);
        Mockito.when(updateCmd.getSortKey()).thenReturn(sortKey);
        Mockito.when(_guestOSCategoryDao.findById(id)).thenReturn(guestOSCategory);
        Mockito.when(_guestOSCategoryDao.update(Mockito.eq(id), any(GuestOSCategoryVO.class))).thenReturn(true);
        GuestOsCategory result = spy.updateGuestOsCategory(updateCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(featured, result.isFeatured());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).findById(id);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).update(Mockito.eq(id), any(GuestOSCategoryVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateGuestOsCategory_ThrowsExceptionWhenCategoryNotFound() {
        UpdateGuestOsCategoryCmd updateCmd = Mockito.mock(UpdateGuestOsCategoryCmd.class);
        long id = 1L;
        when(updateCmd.getId()).thenReturn(id);
        when(_guestOSCategoryDao.findById(id)).thenReturn(null);
        spy.updateGuestOsCategory(updateCmd);
    }

    @Test
    public void testUpdateGuestOsCategory_NoChanges() {
        UpdateGuestOsCategoryCmd updateCmd = Mockito.mock(UpdateGuestOsCategoryCmd.class);
        GuestOSCategoryVO guestOSCategory = new GuestOSCategoryVO("Old name", false);
        long id = 1L;
        when(updateCmd.getId()).thenReturn(id);
        when(updateCmd.getName()).thenReturn(null);
        when(updateCmd.isFeatured()).thenReturn(null);
        when(updateCmd.getSortKey()).thenReturn(null);
        when(_guestOSCategoryDao.findById(id)).thenReturn(guestOSCategory);
        GuestOsCategory result = spy.updateGuestOsCategory(updateCmd);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getName());
        Assert.assertFalse(result.isFeatured());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).findById(id);
        Mockito.verify(_guestOSCategoryDao, Mockito.never()).update(Mockito.eq(id), any(GuestOSCategoryVO.class));
    }

    @Test
    public void testUpdateGuestOsCategory_UpdateNameOnly() {
        UpdateGuestOsCategoryCmd updateCmd = Mockito.mock(UpdateGuestOsCategoryCmd.class);
        GuestOSCategoryVO guestOSCategory = new GuestOSCategoryVO("Old name", false);
        long id = 1L;
        String name = "Updated Name";
        Mockito.when(updateCmd.getId()).thenReturn(id);
        Mockito.when(updateCmd.getName()).thenReturn(name);
        Mockito.when(updateCmd.isFeatured()).thenReturn(null);
        Mockito.when(updateCmd.getSortKey()).thenReturn(null);
        Mockito.when(_guestOSCategoryDao.findById(id)).thenReturn(guestOSCategory);
        Mockito.when(_guestOSCategoryDao.update(Mockito.eq(id), any(GuestOSCategoryVO.class))).thenReturn(true);
        GuestOsCategory result = spy.updateGuestOsCategory(updateCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(name, result.getName());
        Assert.assertFalse(result.isFeatured());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).findById(id);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).update(Mockito.eq(id), any(GuestOSCategoryVO.class));
    }

    @Test
    public void testDeleteGuestOsCategory_Successful() {
        DeleteGuestOsCategoryCmd deleteCmd = Mockito.mock(DeleteGuestOsCategoryCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        long id = 1L;
        Mockito.when(deleteCmd.getId()).thenReturn(id);
        Mockito.when(_guestOSCategoryDao.findById(id)).thenReturn(guestOSCategory);
        Mockito.when(_guestOSDao.listIdsByCategoryId(id)).thenReturn(Arrays.asList());
        Mockito.when(_guestOSCategoryDao.remove(id)).thenReturn(true);
        boolean result = spy.deleteGuestOsCategory(deleteCmd);
        Assert.assertTrue(result);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).findById(id);
        Mockito.verify(_guestOSDao, Mockito.times(1)).listIdsByCategoryId(id);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).remove(id);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteGuestOsCategory_ThrowsExceptionWhenCategoryNotFound() {
        DeleteGuestOsCategoryCmd deleteCmd = Mockito.mock(DeleteGuestOsCategoryCmd.class);
        long id = 1L;
        Mockito.when(deleteCmd.getId()).thenReturn(id);
        Mockito.when(_guestOSCategoryDao.findById(id)).thenReturn(null);
        spy.deleteGuestOsCategory(deleteCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteGuestOsCategory_ThrowsExceptionWhenGuestOsExists() {
        DeleteGuestOsCategoryCmd deleteCmd = Mockito.mock(DeleteGuestOsCategoryCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        long id = 1L;
        Mockito.when(deleteCmd.getId()).thenReturn(id);
        Mockito.when(_guestOSCategoryDao.findById(id)).thenReturn(guestOSCategory);
        Mockito.when(_guestOSDao.listIdsByCategoryId(id)).thenReturn(Arrays.asList(1L));
        spy.deleteGuestOsCategory(deleteCmd);
    }

    private void mockGuestOsJoin() {
        GuestOSVO vo = mock(GuestOSVO.class);
        SearchBuilder<GuestOSVO> sb = mock(SearchBuilder.class);
        when(sb.entity()).thenReturn(vo);
        when(_guestOSDao.createSearchBuilder()).thenReturn(sb);
    }

    @Test
    public void testListGuestOSCategoriesByCriteria_Success() {
        ListGuestOsCategoriesCmd listCmd = Mockito.mock(ListGuestOsCategoriesCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        Filter filter = Mockito.mock(Filter.class);
        Long id = 1L;
        String name = "Ubuntu";
        String keyword = "Linux";
        Boolean featured = true;
        Long zoneId = 1L;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = true;
        Boolean isVnf = false;
        Mockito.when(listCmd.getId()).thenReturn(id);
        Mockito.when(listCmd.getName()).thenReturn(name);
        Mockito.when(listCmd.getKeyword()).thenReturn(keyword);
        Mockito.when(listCmd.isFeatured()).thenReturn(featured);
        Mockito.when(listCmd.getZoneId()).thenReturn(zoneId);
        Mockito.when(listCmd.getArch()).thenReturn(arch);
        Mockito.when(listCmd.isIso()).thenReturn(isIso);
        Mockito.when(listCmd.isVnf()).thenReturn(isVnf);
        SearchBuilder<GuestOSCategoryVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        Mockito.when(searchBuilder.entity()).thenReturn(guestOSCategory);
        SearchCriteria<GuestOSCategoryVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        Mockito.when(_guestOSCategoryDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(templateDao.listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf)).thenReturn(Arrays.asList(1L, 2L));
        Pair<List<GuestOSCategoryVO>, Integer> mockResult = new Pair<>(Arrays.asList(guestOSCategory), 1);
        mockGuestOsJoin();
        Mockito.when(_guestOSCategoryDao.searchAndCount(Mockito.eq(searchCriteria), Mockito.any())).thenReturn(mockResult);
        Pair<List<? extends GuestOsCategory>, Integer> result = spy.listGuestOSCategoriesByCriteria(listCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.second().intValue());
        Assert.assertEquals(1, result.first().size());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).createSearchBuilder();
        Mockito.verify(templateDao, Mockito.times(1)).listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).searchAndCount(Mockito.eq(searchCriteria), Mockito.any());
    }

    @Test
    public void testListGuestOSCategoriesByCriteria_NoResults() {
        ListGuestOsCategoriesCmd listCmd = Mockito.mock(ListGuestOsCategoriesCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        Long id = 1L;
        String name = "CentOS";
        String keyword = "Linux";
        Boolean featured = false;
        Long zoneId = 1L;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = false;
        Boolean isVnf = false;
        Mockito.when(listCmd.getId()).thenReturn(id);
        Mockito.when(listCmd.getName()).thenReturn(name);
        Mockito.when(listCmd.getKeyword()).thenReturn(keyword);
        Mockito.when(listCmd.isFeatured()).thenReturn(featured);
        Mockito.when(listCmd.getZoneId()).thenReturn(zoneId);
        Mockito.when(listCmd.getArch()).thenReturn(arch);
        Mockito.when(listCmd.isIso()).thenReturn(isIso);
        Mockito.when(listCmd.isVnf()).thenReturn(isVnf);
        SearchBuilder<GuestOSCategoryVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        Mockito.when(searchBuilder.entity()).thenReturn(guestOSCategory);
        SearchCriteria<GuestOSCategoryVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        Mockito.when(_guestOSCategoryDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(templateDao.listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf)).thenReturn(Arrays.asList(1L, 2L));
        Pair<List<GuestOSCategoryVO>, Integer> mockResult = new Pair<>(Arrays.asList(), 0);
        Mockito.when(_guestOSCategoryDao.searchAndCount(Mockito.eq(searchCriteria), Mockito.any())).thenReturn(mockResult);
        mockGuestOsJoin();
        Pair<List<? extends GuestOsCategory>, Integer> result = spy.listGuestOSCategoriesByCriteria(listCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.second().intValue());
        Assert.assertEquals(0, result.first().size());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).createSearchBuilder();
        Mockito.verify(templateDao, Mockito.times(1)).listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).searchAndCount(Mockito.eq(searchCriteria), Mockito.any());
    }

    @Test
    public void testListGuestOSCategoriesByCriteria_NoGuestOsIdsFound() {
        ListGuestOsCategoriesCmd listCmd = Mockito.mock(ListGuestOsCategoriesCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        Long id = 1L;
        String name = "Ubuntu";
        String keyword = "Linux";
        Boolean featured = true;
        Long zoneId = 1L;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = true;
        Boolean isVnf = false;
        Mockito.when(listCmd.getId()).thenReturn(id);
        Mockito.when(listCmd.getName()).thenReturn(name);
        Mockito.when(listCmd.getKeyword()).thenReturn(keyword);
        Mockito.when(listCmd.isFeatured()).thenReturn(featured);
        Mockito.when(listCmd.getZoneId()).thenReturn(zoneId);
        Mockito.when(listCmd.getArch()).thenReturn(arch);
        Mockito.when(listCmd.isIso()).thenReturn(isIso);
        Mockito.when(listCmd.isVnf()).thenReturn(isVnf);
        SearchBuilder<GuestOSCategoryVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        Mockito.when(searchBuilder.entity()).thenReturn(guestOSCategory);
        SearchCriteria<GuestOSCategoryVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        Mockito.when(_guestOSCategoryDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(templateDao.listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf)).thenReturn(Arrays.asList(1L, 2L));
        Pair<List<GuestOSCategoryVO>, Integer> mockResult = new Pair<>(Arrays.asList(), 0);
        when(_guestOSCategoryDao.searchAndCount(Mockito.eq(searchCriteria), Mockito.any())).thenReturn(mockResult);
        mockGuestOsJoin();
        Pair<List<? extends GuestOsCategory>, Integer> result = spy.listGuestOSCategoriesByCriteria(listCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.second().intValue());
        Assert.assertEquals(0, result.first().size());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).createSearchBuilder();
        Mockito.verify(templateDao, Mockito.times(1)).listTemplateIsoByArchVnfAndZone(zoneId, arch, isIso, isVnf);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).searchAndCount(Mockito.eq(searchCriteria), Mockito.any());
    }

    @Test
    public void testListGuestOSCategoriesByCriteria_FilterById() {
        ListGuestOsCategoriesCmd listCmd = Mockito.mock(ListGuestOsCategoriesCmd.class);
        GuestOSCategoryVO guestOSCategory = Mockito.mock(GuestOSCategoryVO.class);
        Long id = 1L;
        Mockito.when(listCmd.getId()).thenReturn(id);
        Mockito.when(listCmd.getZoneId()).thenReturn(null);
        Mockito.when(listCmd.isIso()).thenReturn(null);
        Mockito.when(listCmd.isVnf()).thenReturn(null);
        SearchBuilder<GuestOSCategoryVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        Mockito.when(searchBuilder.entity()).thenReturn(guestOSCategory);
        SearchCriteria<GuestOSCategoryVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        Mockito.when(_guestOSCategoryDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(searchBuilder.create()).thenReturn(searchCriteria);
        Pair<List<GuestOSCategoryVO>, Integer> mockResult = new Pair<>(Arrays.asList(guestOSCategory), 1);
        Mockito.when(_guestOSCategoryDao.searchAndCount(Mockito.eq(searchCriteria), Mockito.any())).thenReturn(mockResult);
        mockGuestOsJoin();
        Pair<List<? extends GuestOsCategory>, Integer> result = spy.listGuestOSCategoriesByCriteria(listCmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.second().intValue());
        Assert.assertEquals(1, result.first().size());
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).createSearchBuilder();
        Mockito.verify(searchCriteria, Mockito.times(1)).setParameters("id", id);
        Mockito.verify(_guestOSCategoryDao, Mockito.times(1)).searchAndCount(Mockito.eq(searchCriteria), Mockito.any());

    }

    @Test
    public void testGetExternalVmConsole() {
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Host host = Mockito.mock(Host.class);
        Mockito.when(extensionManager.getInstanceConsole(virtualMachine, host)).thenReturn(Mockito.mock(com.cloud.agent.api.Answer.class));
        Assert.assertNotNull(spy.getExternalVmConsole(virtualMachine, host));
        Mockito.verify(extensionManager).getInstanceConsole(virtualMachine, host);
    }

    @Test
    public void getStatesForIpAddressSearchReturnsValidStates() {
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getState()).thenReturn("Allocated ,free");
        List<IpAddress.State> result = spy.getStatesForIpAddressSearch(cmd);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(IpAddress.State.Allocated));
        Assert.assertTrue(result.contains(IpAddress.State.Free));
    }

    @Test
    public void getStatesForIpAddressSearchReturnsEmptyListForNullState() {
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getState()).thenReturn(null);
        List<IpAddress.State> result = spy.getStatesForIpAddressSearch(cmd);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getStatesForIpAddressSearchReturnsEmptyListForBlankState() {
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getState()).thenReturn("   ");
        List<IpAddress.State> result = spy.getStatesForIpAddressSearch(cmd);
        Assert.assertTrue(result.isEmpty());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getStatesForIpAddressSearchThrowsExceptionForInvalidState() {
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getState()).thenReturn("InvalidState");
        spy.getStatesForIpAddressSearch(cmd);
    }

    @Test
    public void getStatesForIpAddressSearchHandlesMixedValidAndInvalidStates() {
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getState()).thenReturn("Allocated,InvalidState");
        try {
            spy.getStatesForIpAddressSearch(cmd);
            Assert.fail("Expected InvalidParameterValueException to be thrown");
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals("Invalid state: InvalidState", e.getMessage());
        }
    }
}
