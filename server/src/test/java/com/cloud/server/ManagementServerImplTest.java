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

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.gpu.GPU;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
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
import com.cloud.utils.Ternary;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.agent.manager.allocator.HostAllocator;
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
import org.mockito.InjectMocks;
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
import java.util.Map;

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

    @Mock
    UserVmDetailsDao userVmDetailsDao;

    @Mock
    HostDetailsDao hostDetailsDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    HostDao hostDao;

    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    ServiceOfferingDao offeringDao;

    @Mock
    DiskOfferingDao diskOfferingDao;

    @Mock
    HypervisorCapabilitiesDao hypervisorCapabilitiesDao;

    @Mock
    DataStoreManager dataStoreManager;

    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    DpdkHelper dpdkHelper;

    @Mock
    AffinityGroupVMMapDao affinityGroupVMMapDao;

    @Mock
    DeploymentPlanningManager dpMgr;

    @Mock
    DataCenterDao dcDao;

    @Mock
    HostAllocator hostAllocator;

    @InjectMocks
    @Spy
    ManagementServerImpl spy;

    private AutoCloseable closeable;
    private MockedStatic<ApiDBUtils> apiDBUtilsMock;

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

        spy.setHostAllocators(List.of(hostAllocator));

        // Mock ApiDBUtils static method
        apiDBUtilsMock = Mockito.mockStatic(ApiDBUtils.class);
        // Return empty list to avoid architecture filtering in most tests
        apiDBUtilsMock.when(() -> ApiDBUtils.listZoneClustersArchs(Mockito.anyLong()))
            .thenReturn(new ArrayList<>());
    }

    @After
    public void tearDown() throws Exception {
        if (apiDBUtilsMock != null) {
            apiDBUtilsMock.close();
        }
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

    // ============= Tests for listHostsForMigrationOfVM =============

    @Test(expected = PermissionDeniedException.class)
    public void testListHostsForMigrationOfVMNonRootAdmin() {
        mockRunningVM(1L, HypervisorType.KVM);
        Account caller = Mockito.mock(Account.class);
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(_accountMgr.isRootAdmin(caller.getId())).thenReturn(false);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMNullVM() {
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(null);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMNotRunning() {
        VMInstanceVO vm = mockVM(1L, HypervisorType.KVM, State.Stopped);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMUnsupportedHypervisor() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.BareMetal);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMLxcUserVM() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.LXC);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test
    public void testListHostsForMigrationOfVMGpuEnabled() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);

        // Mock GPU detail
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(Mockito.mock(com.cloud.service.ServiceOfferingDetailsVO.class));

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, java.util.Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.first().first().size());
        Assert.assertEquals(Integer.valueOf(0), result.first().second());
        Assert.assertEquals(0, result.second().size());
    }

    @Test
    public void testListHostsForMigrationOfVMWithSystemVM() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.VMware);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.VMware);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // System VMs can use storage motion
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.VMware, null))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.VMware);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.VMware);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        // Verify this doesn't throw exception - system VMs should be migratable
        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify storage motion capability was checked
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.VMware, null);

        // Verify result structure and data
        Assert.assertNotNull(result);
        Assert.assertNotNull("All hosts list should not be null", result.first());
        Assert.assertNotNull("Suitable hosts list should not be null", result.second());
        Assert.assertNotNull("Storage motion map should not be null", result.third());

        // Verify all hosts returned (from searchForServers)
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("All hosts list should contain 2 hosts", 2, result.first().first().size());

        // Verify suitable hosts (from host allocator)
        Assert.assertEquals("Should return 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Suitable hosts should contain host1",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Suitable hosts should contain host2",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithDomainRouter() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume);

        // Verify domain router can be migrated
        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify hypervisor capabilities were checked
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.KVM, "");

        // Verify result contains expected hosts
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should return 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Result should contain host 101",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Result should contain host 102",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithMultipleVolumes() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        // Multiple volumes - root and data disk
        VolumeVO rootVolume = mockVolume(1L, 1L);
        VolumeVO dataVolume = mockVolume(2L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(rootVolume, dataVolume));

        DiskOfferingVO sharedOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(1L)).thenReturn(sharedOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, rootVolume);

        // Verify multiple volumes are handled
        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify all volumes were checked
        Mockito.verify(volumeDao).findCreatedByInstance(vm.getId());
        Mockito.verify(diskOfferingDao, Mockito.times(2)).findById(1L);

        // Verify result
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should return 2 suitable hosts for migration", 2, result.second().size());
        Assert.assertTrue("Suitable hosts should include host 101",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Suitable hosts should include host 102",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMWithMixedStorage() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.XenServer);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.XenServer);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // No storage motion support
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.XenServer, null))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        // Mixed storage - one shared, one local
        VolumeVO sharedVolume = mockVolume(1L, 1L);
        VolumeVO localVolume = mockVolume(2L, 2L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(sharedVolume, localVolume));

        DiskOfferingVO sharedOffering = mockSharedDiskOffering(1L);
        DiskOfferingVO localOffering = mockLocalDiskOffering(2L);
        Mockito.when(diskOfferingDao.findById(sharedVolume.getDiskOfferingId())).thenReturn(sharedOffering);
        Mockito.when(diskOfferingDao.findById(localVolume.getDiskOfferingId())).thenReturn(localOffering);

        // Should throw exception because we have local storage without storage motion support
        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test
    public void testListHostsForMigrationOfVMKVMWithNullHypervisorVersion() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        // KVM host with null hypervisor version
        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // KVM null version should be treated as empty string
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify KVM null version was converted to empty string
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.KVM, "");

        // Verify result data
        Assert.assertNotNull(result);
        Assert.assertEquals("Total hosts should be 2", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Suitable hosts should be 2", 2, result.second().size());
        Assert.assertTrue("Should contain host 101",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Should contain host 102",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMNonUefiVm() {
        // Test VM migration for non-UEFI VM (regular VM migration flow)
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify hosts are returned for migration (non-UEFI VMs don't need UEFI-enabled hosts)
        Assert.assertNotNull(result);
        Assert.assertEquals("Should have 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Should contain host 101",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Should contain host 102",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithUefiVmClusterScope() {
        // Test UEFI VM migration with cluster-scoped search (no storage motion)
        // This exercises the code path where filteredHosts is NULL and allocateTo without list is called
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // No storage motion support - cluster-scoped search
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        // Mock filterUefiHostsForMigration to return success with filtered hosts (only host1 is UEFI-compatible)
        List<HostVO> uefiCompatibleHosts = List.of(host1);
        Pair<Boolean, List<HostVO>> uefiFilterResult = new Pair<>(true, uefiCompatibleHosts);
        Mockito.doReturn(uefiFilterResult).when(spy).filterUefiHostsForMigration(
            Mockito.anyList(), Mockito.anyList(), Mockito.any());

        // Setup other mocks
        Mockito.when(dpdkHelper.isVMDpdkEnabled(vm.getId())).thenReturn(false);
        Mockito.when(affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId())).thenReturn(0L);
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        Mockito.when(dcDao.findById(srcHost.getDataCenterId())).thenReturn(dc);
        Mockito.doNothing().when(dpMgr).checkForNonDedicatedResources(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(dpMgr).reorderHostsByPriority(Mockito.any(), Mockito.anyList());

        // After UEFI filtering, filteredHosts is set to uefiCompatibleHosts (line 1582)
        // So allocateTo WITH list parameter is called (line 1608) even in cluster scope
        Mockito.when(hostAllocator.allocateTo(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(new ArrayList<>(uefiCompatibleHosts));

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify result structure
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("All hosts list should contain 2 hosts", 2, result.first().first().size());

        // Verify only UEFI-compatible hosts are in suitable list
        Assert.assertEquals("Should have 1 UEFI-compatible suitable host", 1, result.second().size());
        Assert.assertTrue("Host 101 should be the only UEFI-compatible host",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertFalse("Host 102 should not be in suitable hosts (not UEFI-compatible)",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithUefiVmZoneWideScope() {
        // Test UEFI VM migration with zone-wide search (storage motion enabled)
        // This exercises the code path where filteredHosts IS populated and allocateTo WITH list is called
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.VMware);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.VMware);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // Storage motion supported - zone-wide search with filteredHosts
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.VMware, null))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for zone-wide search (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.VMware);
        HostVO host2 = mockHost(102L, 2L, 1L, 1L, HypervisorType.VMware); // Different cluster
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        // Mock filterUefiHostsForMigration to return success with filtered hosts (only host1 is UEFI-compatible)
        List<HostVO> uefiCompatibleHosts = List.of(host1);
        Pair<Boolean, List<HostVO>> uefiFilterResult = new Pair<>(true, uefiCompatibleHosts);
        Mockito.doReturn(uefiFilterResult).when(spy).filterUefiHostsForMigration(
            Mockito.anyList(), Mockito.anyList(), Mockito.any());

        // Override hostAllocator to return only UEFI-compatible hosts
        // Uses allocateTo WITH list parameter (filteredHosts is populated in zone-wide search)
        Mockito.when(hostAllocator.allocateTo(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(new ArrayList<>(uefiCompatibleHosts));

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify result structure
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("All hosts list should contain 2 hosts", 2, result.first().first().size());

        // Verify only UEFI-compatible hosts are in suitable list
        Assert.assertEquals("Should have 1 UEFI-compatible suitable host", 1, result.second().size());
        Assert.assertTrue("Host 101 should be the only UEFI-compatible host",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertFalse("Host 102 should not be in suitable hosts (not UEFI-compatible)",
            result.second().stream().anyMatch(h -> h.getId() == 102L));

        // Verify storage motion map is populated
        Assert.assertNotNull("Storage motion map should not be null", result.third());
    }

    @Test
    public void testListHostsForMigrationOfVMUefiFilteringReturnsEmpty() {
        // Test case where UEFI filtering results in no suitable hosts
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        // Mock filterUefiHostsForMigration FIRST to return false (no UEFI-enabled hosts found)
        // This simulates the scenario where UEFI VM has no compatible hosts
        Pair<Boolean, List<HostVO>> uefiFilterResult = new Pair<>(false, null);
        Mockito.doReturn(uefiFilterResult).when(spy).filterUefiHostsForMigration(
            Mockito.anyList(), Mockito.anyList(), Mockito.any());

        // Note: No other mocks needed because when filterUefiHostsForMigration returns false,
        // the method returns early and doesn't proceed to host allocation or other processing

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify result structure
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertNotNull("All hosts list should not be null", result.first());
        Assert.assertNotNull("Suitable hosts list should not be null", result.second());
        Assert.assertNotNull("Storage motion map should not be null", result.third());

        // Verify all hosts are still returned (from searchForServers)
        Assert.assertEquals("Should still return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("All hosts list should contain 2 hosts", 2, result.first().first().size());

        // Verify suitable hosts list is empty due to UEFI filtering
        Assert.assertEquals("Should have 0 suitable hosts after UEFI filtering", 0, result.second().size());

        // Verify storage motion map is empty
        Assert.assertTrue("Storage motion map should be empty when no suitable hosts", result.third().isEmpty());
    }

    @Test
    public void testListHostsForMigrationOfVMStorageMotionCapabilityCheck() {
        // Test User VM with VMware - should check storage motion for User VMs
        VMInstanceVO userVm = mockRunningVM(1L, HypervisorType.VMware);
        Mockito.when(userVm.getType()).thenReturn(VirtualMachine.Type.User);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(userVm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(userVm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.VMware);
        Mockito.when(hostDao.findById(userVm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.VMware, null))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(userVm.getId(), userVm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(userVm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.VMware);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.VMware);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(userVm, srcHost, hosts, volume, true);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify storage motion capability was checked for User VM
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.VMware, null);

        // Verify response data
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Host 101 should be in suitable list",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Host 102 should be in suitable list",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithAllSupportedHypervisors() {
        // Test each supported hypervisor type
        HypervisorType[] supportedTypes = {
            HypervisorType.XenServer,
            HypervisorType.VMware,
            HypervisorType.KVM,
            HypervisorType.Ovm,
            HypervisorType.Hyperv,
            HypervisorType.Ovm3
        };

        for (HypervisorType hypervisorType : supportedTypes) {
            VMInstanceVO vm = mockRunningVM(1L, hypervisorType);
            Account caller = mockRootAdminAccount();
            Mockito.doReturn(caller).when(spy).getCaller();
            Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
            Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
                .thenReturn(null);

            HostVO srcHost = mockHost(100L, 1L, 1L, 1L, hypervisorType);
            Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

            String version = hypervisorType == HypervisorType.KVM ? "" : null;
            Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(hypervisorType, version))
                .thenReturn(false);

            ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
            Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

            VolumeVO volume = mockVolume(1L, 1L);
            Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

            DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
            Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

            // Mock searchForServers for cluster-scoped search
            HostVO host1 = mockHost(101L, 1L, 1L, 1L, hypervisorType);
            HostVO host2 = mockHost(102L, 1L, 1L, 1L, hypervisorType);
            List<HostVO> hosts = List.of(host1, host2);
            Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
            Mockito.doReturn(hostsPair).when(spy).searchForServers(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
                Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
                Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
                Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

            setupMigrationMocks(vm, srcHost, hosts, volume);

            Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
                spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

            // Verify hypervisor is in supported hypervisors list
            Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(hypervisorType, version);

            // Verify validation passed for this hypervisor
            Assert.assertNotNull("Result should not be null for " + hypervisorType, result);
            Assert.assertEquals("Should return 2 total hosts for " + hypervisorType,
                Integer.valueOf(2), result.first().second());
            Assert.assertEquals("Should have 2 suitable hosts for " + hypervisorType,
                2, result.second().size());
            Assert.assertTrue("Host 101 should be available for " + hypervisorType,
                result.second().stream().anyMatch(h -> h.getId() == 101L));
            Assert.assertTrue("Host 102 should be available for " + hypervisorType,
                result.second().stream().anyMatch(h -> h.getId() == 102L));

            // Reset mocks for next iteration
            Mockito.reset(vmInstanceDao, hostDao, serviceOfferingDetailsDao, volumeDao,
                diskOfferingDao, hypervisorCapabilitiesDao, offeringDao, dpdkHelper,
                affinityGroupVMMapDao, dpMgr, dcDao, hostAllocator, dataStoreManager);
            Mockito.reset(spy);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMSourceHostNotFound() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(null);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostsForMigrationOfVMLocalStorageNoStorageMotion() {
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.XenServer);
        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.XenServer);
        Account caller = mockRootAdminAccount();

        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // Mock storage motion not supported
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.XenServer, null))
            .thenReturn(false);

        // Mock local storage usage
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockLocalDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);
    }

    @Test
    public void testListHostsForMigrationOfVMStorageMotionCheckForSystemVM() {
        // Test that storage motion capability is checked for System VMs
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.VMware);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.VMware);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // Storage motion supported for VMware
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.VMware, null))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.VMware);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.VMware);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify that storage motion capability was checked for system VM (VMware is in hypervisorTypes list)
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.VMware, null);

        // Verify response structure
        Assert.assertNotNull(result);
        Assert.assertEquals("Should have 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertTrue("Should have suitable hosts", result.second().size() > 0);
    }

    @Test
    public void testListHostsForMigrationOfVMStorageMotionCheckForUserVM() {
        // Test that storage motion capability is checked for User VMs
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // Storage motion supported for User VM with KVM
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify User VM can migrate with storage (User VM type always checks)
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.KVM, "");

        // Verify response data
        Assert.assertNotNull(result);
        Assert.assertEquals("Should have 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertTrue("Should have suitable hosts", result.second().size() > 0);
    }

    @Test
    public void testListHostsForMigrationOfVMWithoutStorageMotionClusterScope() {
        // When storage motion not supported, should search only in same cluster
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.XenServer);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.XenServer);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // No storage motion support
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.XenServer, null))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers - verify cluster scope is used
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.XenServer);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.XenServer);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.eq(0L), Mockito.eq(20L), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.eq(1L), // cluster=1L
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.eq(100L));

        setupMigrationMocks(vm, srcHost, hosts, volume);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify XenServer without storage motion was checked
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.XenServer, null);
        // Verify cluster-scoped search was used (not zone-wide)
        Mockito.verify(spy).searchForServers(
            Mockito.eq(0L), Mockito.eq(20L), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.eq(1L),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.eq(100L));

        // Verify response data
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Should contain host 101",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Should contain host 102",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    @Test
    public void testListHostsForMigrationOfVMWithNoVolumes() {
        // Edge case: VM with no volumes
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        // No volumes
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(new ArrayList<>());

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        // Set up mocks without volume since there are no volumes
        Pair<Boolean, List<HostVO>> uefiResult = new Pair<>(true, hosts);
        Mockito.doReturn(uefiResult).when(spy).filterUefiHostsForMigration(
            Mockito.anyList(), Mockito.anyList(), Mockito.any());
        Mockito.when(dpdkHelper.isVMDpdkEnabled(vm.getId())).thenReturn(false);
        Mockito.when(affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId())).thenReturn(0L);
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        Mockito.when(dcDao.findById(1L)).thenReturn(dc);
        Mockito.doNothing().when(dpMgr).checkForNonDedicatedResources(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(dpMgr).reorderHostsByPriority(Mockito.any(), Mockito.anyList());

        Mockito.when(hostAllocator.allocateTo(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(new ArrayList<>(hosts));

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Should still process without throwing exception for usesLocal check
        Mockito.verify(volumeDao).findCreatedByInstance(vm.getId());

        // Verify response
        Assert.assertNotNull(result);
        Assert.assertEquals("Should have 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts even with no volumes", 2, result.second().size());
        Assert.assertTrue("Storage motion map should be empty for VM with no volumes",
            result.third().isEmpty());
    }

    @Test
    public void testListHostsForMigrationOfVMOverloadedMethod() {
        // Test the overloaded method that takes vmId instead of vm object
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search with keyword
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.eq("keyword-test"), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume);

        // Call overloaded method with vmId
        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, "keyword-test");

        // Verify VM was fetched by ID
        Mockito.verify(vmInstanceDao).findById(1L);

        // Verify keyword was passed to searchForServers
        Mockito.verify(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.eq("keyword-test"), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        // Verify response data
        Assert.assertNotNull(result);
        Assert.assertEquals("Should have 2 hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
    }

    @Test
    public void testListHostsForMigrationOfVMVmwareStorageMotionCheck() {
        // VMware should check storage motion even for non-User VMs
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.VMware);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.VMware);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        // VMware with DomainRouter should still check storage motion
        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.VMware, null))
            .thenReturn(true);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers with zone-wide scope (storage motion enabled)
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.VMware);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.VMware);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.any(HypervisorType.class), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume, true);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify VMware always checks storage motion (hypervisorTypes list includes VMware)
        Mockito.verify(hypervisorCapabilitiesDao).isStorageMotionSupported(HypervisorType.VMware, null);

        // Verify response
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Host 101 should be in the list",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
    }


    @Test
    public void testListHostsForMigrationOfVMWithNullKeyword() {
        // Test with null keyword parameter
        VMInstanceVO vm = mockRunningVM(1L, HypervisorType.KVM);

        Account caller = mockRootAdminAccount();
        Mockito.doReturn(caller).when(spy).getCaller();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()))
            .thenReturn(null);

        HostVO srcHost = mockHost(100L, 1L, 1L, 1L, HypervisorType.KVM);
        Mockito.when(hostDao.findById(vm.getHostId())).thenReturn(srcHost);

        Mockito.when(hypervisorCapabilitiesDao.isStorageMotionSupported(HypervisorType.KVM, ""))
            .thenReturn(false);

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offeringDao.findById(vm.getId(), vm.getServiceOfferingId())).thenReturn(offering);

        VolumeVO volume = mockVolume(1L, 1L);
        Mockito.when(volumeDao.findCreatedByInstance(vm.getId())).thenReturn(List.of(volume));

        DiskOfferingVO diskOffering = mockSharedDiskOffering(1L);
        Mockito.when(diskOfferingDao.findById(volume.getDiskOfferingId())).thenReturn(diskOffering);

        // Mock searchForServers for cluster-scoped search
        HostVO host1 = mockHost(101L, 1L, 1L, 1L, HypervisorType.KVM);
        HostVO host2 = mockHost(102L, 1L, 1L, 1L, HypervisorType.KVM);
        List<HostVO> hosts = List.of(host1, host2);
        Pair<List<HostVO>, Integer> hostsPair = new Pair<>(hosts, 2);
        Mockito.doReturn(hostsPair).when(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        setupMigrationMocks(vm, srcHost, hosts, volume);

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> result =
            spy.listHostsForMigrationOfVM(1L, 0L, 20L, null);

        // Verify null keyword is handled
        Mockito.verify(vmInstanceDao).findById(1L);

        // Verify searchForServers was called with null keyword
        Mockito.verify(spy).searchForServers(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.any(Type.class),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(),
            Mockito.isNull(), Mockito.isNull(), Mockito.anyLong());

        // Verify response data
        Assert.assertNotNull(result);
        Assert.assertEquals("Should return 2 total hosts", Integer.valueOf(2), result.first().second());
        Assert.assertEquals("Should have 2 suitable hosts", 2, result.second().size());
        Assert.assertTrue("Host 101 should be available",
            result.second().stream().anyMatch(h -> h.getId() == 101L));
        Assert.assertTrue("Host 102 should be available",
            result.second().stream().anyMatch(h -> h.getId() == 102L));
    }

    // Note: Tests for success scenarios with complex flows (managed storage, zone-wide volumes,
    // DPDK exclusion, affinity groups, architecture filtering) require full setup with mocking
    // private methods like hasSuitablePoolsForVolume(), excludeNonDPDKEnabledHosts(), and
    // filterUefiHostsForMigration() which are better suited for integration tests.

    // ============= Helper methods for tests =============

    /**
     * Sets up common mocks for successful migration tests
     * For storage motion tests, set forceStorageMotion=true to configure volume in same cluster
     * (which avoids complex filtering logic for cross-cluster storage motion)
     */
    private void setupMigrationMocks(VMInstanceVO vm, HostVO srcHost,
                                     List<HostVO> targetHosts, VolumeVO volume) {
        setupMigrationMocks(vm, srcHost, targetHosts, volume, false);
    }

    private void setupMigrationMocks(VMInstanceVO vm, HostVO srcHost,
                                     List<HostVO> targetHosts, VolumeVO volume,
                                     boolean forceStorageMotion) {
        // Mock dataStoreManager for volume pool lookup (lenient as not used in all paths)
        // For storage motion tests, put volume in same cluster to avoid complex filtering
        PrimaryDataStore primaryDataStore = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(dataStoreManager.getPrimaryDataStore(volume.getPoolId())).thenReturn(primaryDataStore);
        // If not forceStorageMotion, volume is in same cluster (no storage motion needed)
        // If forceStorageMotion, set volClusterId to null (zone-wide storage)
        Mockito.when(primaryDataStore.getClusterId()).thenReturn(forceStorageMotion ? null : srcHost.getClusterId());

        // Mock zoneWideVolumeRequiresStorageMotion for zone-wide volumes
        if (forceStorageMotion) {
            Mockito.doReturn(false).when(spy).zoneWideVolumeRequiresStorageMotion(
                Mockito.any(), Mockito.any(), Mockito.any());
        }

        // Mock filterUefiHostsForMigration - must return hosts properly
        Pair<Boolean, List<HostVO>> uefiResult = new Pair<>(true, new ArrayList<>(targetHosts));
        Mockito.doReturn(uefiResult).when(spy).filterUefiHostsForMigration(
            Mockito.anyList(), Mockito.anyList(), Mockito.any());

        // Mock DPDK check
        Mockito.when(dpdkHelper.isVMDpdkEnabled(vm.getId())).thenReturn(false);

        // Mock affinity group count
        Mockito.when(affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId())).thenReturn(0L);

        // Mock datacenter
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        Mockito.when(dcDao.findById(srcHost.getDataCenterId())).thenReturn(dc);

        // Mock dedicated resources check
        Mockito.doNothing().when(dpMgr).checkForNonDedicatedResources(
            Mockito.any(), Mockito.any(), Mockito.any());

        // Mock priority reordering
        Mockito.doNothing().when(dpMgr).reorderHostsByPriority(Mockito.any(), Mockito.anyList());

        // Mock host allocators - both signatures
        // 1. Version with filteredHosts list (used when canMigrateWithStorage = true)
        Mockito.when(hostAllocator.allocateTo(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.anyList(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(new ArrayList<>(targetHosts));
    }

    private VMInstanceVO mockRunningVM(Long id, HypervisorType hypervisorType) {
        return mockVM(id, hypervisorType, State.Running);
    }

    private VMInstanceVO mockVM(Long id, HypervisorType hypervisorType, State state) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(id);
        when(vm.getState()).thenReturn(state);
        when(vm.getHypervisorType()).thenReturn(hypervisorType);
        when(vm.getHostId()).thenReturn(100L);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vm.getUuid()).thenReturn("uuid-" + id);
        when(vm.getDataCenterId()).thenReturn(1L);
        return vm;
    }

    private Account mockRootAdminAccount() {
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(_accountMgr.isRootAdmin(1L)).thenReturn(true);
        return account;
    }

    private HostVO mockHost(Long id, Long clusterId, Long podId, Long dcId, HypervisorType hypervisorType) {
        HostVO host = new HostVO("guid-" + id);
        ReflectionTestUtils.setField(host, "id", id);
        ReflectionTestUtils.setField(host, "clusterId", clusterId);
        ReflectionTestUtils.setField(host, "podId", podId);
        ReflectionTestUtils.setField(host, "dataCenterId", dcId);
        ReflectionTestUtils.setField(host, "hypervisorType", hypervisorType);
        ReflectionTestUtils.setField(host, "type", Host.Type.Routing);
        ReflectionTestUtils.setField(host, "hypervisorVersion", null);
        return host;
    }

    private VolumeVO mockVolume(Long id, Long poolId) {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        when(volume.getId()).thenReturn(id);
        when(volume.getPoolId()).thenReturn(poolId);
        when(volume.getDiskOfferingId()).thenReturn(1L);
        when(volume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        return volume;
    }

    private DiskOfferingVO mockLocalDiskOffering(Long id) {
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOffering.getId()).thenReturn(id);
        Mockito.when(diskOffering.isUseLocalStorage()).thenReturn(true);
        return diskOffering;
    }

    private DiskOfferingVO mockSharedDiskOffering(Long id) {
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOffering.getId()).thenReturn(id);
        Mockito.when(diskOffering.isUseLocalStorage()).thenReturn(false);
        return diskOffering;
    }
}
