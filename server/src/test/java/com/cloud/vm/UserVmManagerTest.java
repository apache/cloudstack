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

package com.cloud.vm;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVmNicIpCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class UserVmManagerTest {

    @Spy
    @InjectMocks
    private UserVmManagerImpl _userVmMgr;
    @Mock
    private VirtualMachineManager _itMgr;
    @Mock
    private VolumeOrchestrationService _storageMgr;
    @Mock
    private Account _account;
    @Mock
    private AccountManager _accountMgr;
    @Mock
    private AccountService _accountService;
    @Mock
    private ConfigurationManager _configMgr;
    @Mock
    private CapacityManager _capacityMgr;
    @Mock
    private AccountDao _accountDao;
    @Mock
    private ConfigurationDao _configDao;
    @Mock
    private UserDao _userDao;
    @Mock
    private UserVmDao _vmDao;
    @Mock
    private VMInstanceDao _vmInstanceDao;
    @Mock
    private VMTemplateDao _templateDao;
    @Mock
    private TemplateDataStoreDao _templateStoreDao;
    @Mock
    private VolumeDao _volsDao;
    @Mock
    private RestoreVMCmd _restoreVMCmd;
    @Mock
    private AccountVO _accountMock;
    @Mock
    private UserVO _userMock;
    @Mock
    private UserVmVO _vmMock;
    @Mock
    private VMInstanceVO _vmInstance;
    @Mock
    private VMTemplateVO _templateMock;
    @Mock
    private TemplateDataStoreVO _templateDataStoreMock;
    @Mock
    private VolumeVO _volumeMock;
    @Mock
    private List<VolumeVO> _rootVols;
    @Mock
    private Account _accountMock2;
    @Mock
    private ServiceOfferingDao _offeringDao;
    @Mock
    private ServiceOfferingVO _offeringVo;
    @Mock
    private EntityManager _entityMgr;
    @Mock
    private ResourceLimitService _resourceLimitMgr;
    @Mock
    private PrimaryDataStoreDao _storagePoolDao;
    @Mock
    private UsageEventDao _usageEventDao;
    @Mock
    private VMSnapshotDao _vmSnapshotDao;
    @Mock
    private UpdateVmNicIpCmd _updateVmNicIpCmd;
    @Mock
    private NicDao _nicDao;
    @Mock
    private VlanDao _vlanDao;
    @Mock
    private NicVO _nicMock;
    @Mock
    private NetworkModel _networkModel;
    @Mock
    private NetworkDao _networkDao;
    @Mock
    private NetworkVO _networkMock;
    @Mock
    private DataCenterDao _dcDao;
    @Mock
    private DataCenterVO _dcMock;
    @Mock
    private IpAddressManager _ipAddrMgr;
    @Mock
    private IPAddressDao _ipAddressDao;
    @Mock
    private NetworkOfferingDao _networkOfferingDao;
    @Mock
    private NetworkOfferingVO _networkOfferingMock;
    @Mock
    private NetworkOrchestrationService _networkMgr;

    @Before
    public void setup() {
        doReturn(8L).when(_vmMock).getAccountId();
        lenient().when(_userDao.findById(anyLong())).thenReturn(_userMock);
        lenient().doReturn(Account.State.enabled).when(_account).getState();
        lenient().when(_vmMock.getId()).thenReturn(314L);
        lenient().when(_vmInstance.getId()).thenReturn(1L);
        lenient().when(_vmInstance.getServiceOfferingId()).thenReturn(2L);

        List<VMSnapshotVO> mockList = new ArrayList<>();

    }

    @Test
    public void testValidateRootDiskResize() {
        HypervisorType hypervisorType = HypervisorType.Any;
        Long rootDiskSize = Long.valueOf(10);
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Map<String, String> customParameters = new HashMap<String, String>();
        Map<String, String> vmDetals = new HashMap<String, String>();

        vmDetals.put("rootDiskController", "ide");
        when(vm.getDetails()).thenReturn(vmDetals);
        when(templateVO.getSize()).thenReturn((rootDiskSize << 30) + 1);
        //Case 1: >
        try {
            _userVmMgr.validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);
            Assert.fail("Function should throw InvalidParameterValueException");
        } catch (Exception e) {
            assertThat(e, instanceOf(InvalidParameterValueException.class));
        }

        //Case 2: =
        when(templateVO.getSize()).thenReturn((rootDiskSize << 30));
        customParameters.put("rootdisksize", "10");
        _userVmMgr.validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);
        assert (!customParameters.containsKey("rootdisksize"));

        when(templateVO.getSize()).thenReturn((rootDiskSize << 30) - 1);

        //Case 3:  <

        //Case 3.1: HypervisorType!=VMware
        _userVmMgr.validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);

        hypervisorType = HypervisorType.VMware;
        //Case 3.2:   0->(rootDiskController!=scsi)
        try {
            _userVmMgr.validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);
            Assert.fail("Function should throw InvalidParameterValueException");
        } catch (Exception e) {
            assertThat(e, instanceOf(InvalidParameterValueException.class));
        }

        //Case 3.3:   1->(rootDiskController==scsi)
        vmDetals.put("rootDiskController", "scsi");
        _userVmMgr.validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);
    }

    // Test scaleVm on incompatible HV.
    @Test(expected = InvalidParameterValueException.class)
    public void testScaleVMF1() throws Exception {

        ScaleVMCmd cmd = new ScaleVMCmd();
        Class<?> _class = cmd.getClass();

        Field idField = _class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cmd, 1L);

        Field serviceOfferingIdField = _class.getDeclaredField("serviceOfferingId");
        serviceOfferingIdField.setAccessible(true);
        serviceOfferingIdField.set(cmd, 1L);

        lenient().when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);

        // UserContext.current().setEventDetails("Vm Id: "+getId());
        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        //AccountVO(String accountName, long domainId, String networkDomain, short type, int regionId)
        lenient().doReturn(VirtualMachine.State.Running).when(_vmInstance).getState();

        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    // Test scaleVm on equal service offerings.
    @Test(expected = InvalidParameterValueException.class)
    public void testScaleVMF2() throws Exception {

        ScaleVMCmd cmd = new ScaleVMCmd();
        Class<?> _class = cmd.getClass();

        Field idField = _class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cmd, 1L);

        Field serviceOfferingIdField = _class.getDeclaredField("serviceOfferingId");
        serviceOfferingIdField.setAccessible(true);
        serviceOfferingIdField.set(cmd, 1L);

        lenient().when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        lenient().doReturn(Hypervisor.HypervisorType.XenServer).when(_vmInstance).getHypervisorType();

        lenient().doReturn(VirtualMachine.State.Running).when(_vmInstance).getState();

        lenient().doNothing().when(_accountMgr).checkAccess(_account, null, true, _templateMock);

        lenient().doNothing().when(_itMgr).checkIfCanUpgrade(_vmMock, _offeringVo);

        ServiceOffering so1 = getSvcoffering(512);
        lenient().when(_offeringDao.findById(anyLong())).thenReturn((ServiceOfferingVO)so1);
        lenient().when(_offeringDao.findByIdIncludingRemoved(anyLong(), anyLong())).thenReturn((ServiceOfferingVO)so1);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    // Test scaleVm for Stopped vm.
    //@Test(expected=InvalidParameterValueException.class)
    public void testScaleVMF3() throws Exception {

        ScaleVMCmd cmd = new ScaleVMCmd();
        Class<?> _class = cmd.getClass();

        Field idField = _class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cmd, 1L);

        Field serviceOfferingIdField = _class.getDeclaredField("serviceOfferingId");
        serviceOfferingIdField.setAccessible(true);
        serviceOfferingIdField.set(cmd, 1L);

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        doReturn(Hypervisor.HypervisorType.XenServer).when(_vmInstance).getHypervisorType();

        ServiceOffering so1 = getSvcoffering(512);
        ServiceOffering so2 = getSvcoffering(256);

        when(_entityMgr.findById(eq(ServiceOffering.class), anyLong())).thenReturn(so2);
        when(_entityMgr.findById(ServiceOffering.class, 1L)).thenReturn(so1);

        doReturn(VirtualMachine.State.Stopped).when(_vmInstance).getState();
        when(_vmDao.findById(anyLong())).thenReturn(null);

        doReturn(true).when(_itMgr).upgradeVmDb(anyLong(), so1, so2);

        //when(_vmDao.findById(anyLong())).thenReturn(_vmMock);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    // Test scaleVm for Running vm. Full positive test.
    public void testScaleVMF4() throws Exception {

        ScaleVMCmd cmd = new ScaleVMCmd();
        Class<?> _class = cmd.getClass();

        Field idField = _class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cmd, 1L);

        Field serviceOfferingIdField = _class.getDeclaredField("serviceOfferingId");
        serviceOfferingIdField.setAccessible(true);
        serviceOfferingIdField.set(cmd, 1L);

        //UserContext.current().setEventDetails("Vm Id: "+getId());
        //Account account = (Account) new AccountVO("testaccount", 1L, "networkdomain", (short) 0, 1);
        //AccountVO(String accountName, long domainId, String networkDomain, short type, int regionId)
        //UserContext.registerContext(1, account, null, true);

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        doReturn(Hypervisor.HypervisorType.XenServer).when(_vmInstance).getHypervisorType();

        ServiceOffering so1 = getSvcoffering(512);
        ServiceOffering so2 = getSvcoffering(256);

        when(_entityMgr.findById(eq(ServiceOffering.class), anyLong())).thenReturn(so2);
        when(_entityMgr.findById(ServiceOffering.class, 1L)).thenReturn(so1);

        doReturn(VirtualMachine.State.Running).when(_vmInstance).getState();

        //when(ApiDBUtils.getCpuOverprovisioningFactor()).thenReturn(3f);
        when(_capacityMgr.checkIfHostHasCapacity(anyLong(), anyInt(), anyLong(), anyBoolean(), anyFloat(), anyFloat(), anyBoolean())).thenReturn(false);
        when(_itMgr.reConfigureVm(_vmInstance.getUuid(), so2, so1, new HashMap<String, String>(), false)).thenReturn(_vmInstance);

        doReturn(true).when(_itMgr).upgradeVmDb(anyLong(), so1, so2);

        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    private ServiceOfferingVO getSvcoffering(int ramSize) {
        String name = "name";
        String displayText = "displayText";
        int cpu = 1;
        //int ramSize = 256;
        int speed = 128;

        boolean ha = false;
        boolean useLocalStorage = false;

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText, Storage.ProvisioningType.THIN, useLocalStorage, false, null, false, null,
                false);
        return serviceOffering;
    }

    // Test Move VM b/w accounts where caller is not ROOT/Domain admin
    @Test(expected = InvalidParameterValueException.class)
    public void testMoveVmToUser1() throws Exception {
        AssignVMCmd cmd = new AssignVMCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("virtualMachineId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("accountName");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "account");

        Field domainIdField = _class.getDeclaredField("domainId");
        domainIdField.setAccessible(true);
        domainIdField.set(cmd, 1L);

        // caller is of type 0
        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, caller);
        try {

            _userVmMgr.moveVMToUser(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    // Test Move VM b/w accounts where caller doesn't have access to the old or new account
    @Test(expected = PermissionDeniedException.class)
    public void testMoveVmToUser2() throws Exception {

        AssignVMCmd cmd = new AssignVMCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("virtualMachineId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("accountName");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "account");

        Field domainIdField = _class.getDeclaredField("domainId");
        domainIdField.setAccessible(true);
        domainIdField.set(cmd, 1L);

        // caller is of type 0
        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)1, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        AccountVO oldAccount = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        oldAccount.setId(1L);

        AccountVO newAccount = new AccountVO("testaccount", 1, "networkdomain", (short)1, UUID.randomUUID().toString());
        newAccount.setId(2L);

        UserVmVO vm = new UserVmVO(10L, "test", "test", 1L, HypervisorType.Any, 1L, false, false, 1L, 1L, 1, 5L, "test", "test", 1L);
        vm.setState(VirtualMachine.State.Stopped);
        when(_vmDao.findById(anyLong())).thenReturn(vm);

        when(_accountService.getActiveAccountById(anyLong())).thenReturn(oldAccount);

        when(_accountMgr.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(newAccount);

        doThrow(new PermissionDeniedException("Access check failed")).when(_accountMgr).checkAccess(nullable(Account.class), nullable(AccessType.class), nullable(Boolean.class), nullable(ControlledEntity.class));

        CallContext.register(user, caller);

        when(_accountMgr.isRootAdmin(anyLong())).thenReturn(true);

        try {
            _userVmMgr.moveVMToUser(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testUpdateVmNicIpSuccess1() throws Exception {
        UpdateVmNicIpCmd cmd = new UpdateVmNicIpCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("nicId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("ipAddr");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "10.10.10.10");

        NicVO nic = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        when(_nicDao.findById(anyLong())).thenReturn(nic);
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
        doReturn(9L).when(_networkMock).getNetworkOfferingId();
        when(_networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(_networkOfferingMock);
        doReturn(10L).when(_networkOfferingMock).getId();

        List<Service> services = new ArrayList<Service>();
        services.add(Service.Dhcp);
        when(_networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        when(_vmMock.getState()).thenReturn(State.Stopped);
        lenient().doNothing().when(_accountMgr).checkAccess(_account, null, true, _vmMock);
        when(_accountDao.findByIdIncludingRemoved(anyLong())).thenReturn(_accountMock);

        when(_networkMock.getState()).thenReturn(Network.State.Implemented);
        when(_networkMock.getDataCenterId()).thenReturn(3L);
        when(_networkMock.getGuestType()).thenReturn(GuestType.Isolated);
        when(_dcDao.findById(anyLong())).thenReturn(_dcMock);
        when(_dcMock.getNetworkType()).thenReturn(NetworkType.Advanced);

        when(_ipAddrMgr.allocateGuestIP(Mockito.eq(_networkMock), anyString())).thenReturn("10.10.10.10");
        doNothing().when(_networkMgr).implementNetworkElementsAndResources(Mockito.any(DeployDestination.class), Mockito.any(ReservationContext.class), Mockito.eq(_networkMock),
                Mockito.eq(_networkOfferingMock));
        when(_nicDao.persist(any(NicVO.class))).thenReturn(nic);

        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, caller);
        try {
            _userVmMgr.updateNicIpForVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testUpdateVmNicIpSuccess2() throws Exception {
        UpdateVmNicIpCmd cmd = new UpdateVmNicIpCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("nicId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("ipAddr");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "10.10.10.10");

        NicVO nic = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        when(_nicDao.findById(anyLong())).thenReturn(nic);
        nic.setIPv4Address("10.10.10.9");
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
        doReturn(9L).when(_networkMock).getNetworkOfferingId();
        when(_networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(_networkOfferingMock);
        doReturn(10L).when(_networkOfferingMock).getId();

        List<Service> services = new ArrayList<Service>();
        when(_networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        lenient().when(_vmMock.getState()).thenReturn(State.Running);
        lenient().doNothing().when(_accountMgr).checkAccess(_account, null, true, _vmMock);
        when(_accountDao.findByIdIncludingRemoved(anyLong())).thenReturn(_accountMock);

        when(_networkMock.getState()).thenReturn(Network.State.Implemented);
        when(_networkMock.getDataCenterId()).thenReturn(3L);
        when(_networkMock.getGuestType()).thenReturn(GuestType.Shared);
        when(_dcDao.findById(anyLong())).thenReturn(_dcMock);
        when(_dcMock.getNetworkType()).thenReturn(NetworkType.Advanced);

        IPAddressVO newIp = mock(IPAddressVO.class);
        when(newIp.getVlanId()).thenReturn(1L);

        VlanVO vlan = mock(VlanVO.class);
        when(vlan.getVlanGateway()).thenReturn("10.10.10.1");
        when(vlan.getVlanNetmask()).thenReturn("255.255.255.0");

        when(_ipAddrMgr.allocatePublicIpForGuestNic(Mockito.eq(_networkMock), nullable(Long.class), Mockito.eq(_accountMock), anyString())).thenReturn("10.10.10.10");
        when(_ipAddressDao.findByIpAndSourceNetworkId(anyLong(), eq("10.10.10.10"))).thenReturn(newIp);
        when(_ipAddressDao.findByIpAndSourceNetworkId(anyLong(), eq("10.10.10.9"))).thenReturn(null);
        when(_nicDao.persist(any(NicVO.class))).thenReturn(nic);
        when(_vlanDao.findById(anyLong())).thenReturn(vlan);

        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, caller);
        try {
            _userVmMgr.updateNicIpForVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    // vm is running in network with dhcp support
    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateVmNicIpFailure1() throws Exception {
        UpdateVmNicIpCmd cmd = new UpdateVmNicIpCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("nicId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("ipAddr");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "10.10.10.10");

        NicVO nic = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        when(_nicDao.findById(anyLong())).thenReturn(nic);
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
        when(_networkMock.getState()).thenReturn(Network.State.Implemented);
        doReturn(9L).when(_networkMock).getNetworkOfferingId();
        when(_networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(_networkOfferingMock);
        doReturn(10L).when(_networkOfferingMock).getId();

        List<Service> services = new ArrayList<Service>();
        services.add(Service.Dhcp);
        when(_networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        when(_vmMock.getState()).thenReturn(State.Running);

        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, caller);
        try {
            _userVmMgr.updateNicIpForVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    // vm is stopped in isolated network in advanced zone
    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateVmNicIpFailure2() throws Exception {
        UpdateVmNicIpCmd cmd = new UpdateVmNicIpCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("nicId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("ipAddr");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "10.10.10.10");

        NicVO nic = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        when(_nicDao.findById(anyLong())).thenReturn(nic);
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
        doReturn(9L).when(_networkMock).getNetworkOfferingId();
        when(_networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(_networkOfferingMock);
        doReturn(10L).when(_networkOfferingMock).getId();

        List<Service> services = new ArrayList<Service>();
        services.add(Service.Dhcp);
        when(_networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        when(_vmMock.getState()).thenReturn(State.Stopped);
        lenient().doNothing().when(_accountMgr).checkAccess(_account, null, true, _vmMock);
        when(_accountDao.findByIdIncludingRemoved(anyLong())).thenReturn(_accountMock);

        when(_networkMock.getState()).thenReturn(Network.State.Implemented);
        when(_networkMock.getDataCenterId()).thenReturn(3L);
        when(_networkMock.getGuestType()).thenReturn(GuestType.Isolated);
        when(_dcDao.findById(anyLong())).thenReturn(_dcMock);
        when(_dcMock.getNetworkType()).thenReturn(NetworkType.Advanced);

        when(_ipAddrMgr.allocateGuestIP(Mockito.eq(_networkMock), anyString())).thenReturn(null);

        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, caller);
        try {
            _userVmMgr.updateNicIpForVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    // vm is stopped in shared network in advanced zone
    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateVmNicIpFailure3() throws Exception {
        UpdateVmNicIpCmd cmd = new UpdateVmNicIpCmd();
        Class<?> _class = cmd.getClass();

        Field virtualmachineIdField = _class.getDeclaredField("nicId");
        virtualmachineIdField.setAccessible(true);
        virtualmachineIdField.set(cmd, 1L);

        Field accountNameField = _class.getDeclaredField("ipAddr");
        accountNameField.setAccessible(true);
        accountNameField.set(cmd, "10.10.10.10");

        NicVO nic = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        when(_nicDao.findById(anyLong())).thenReturn(nic);
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
        doReturn(9L).when(_networkMock).getNetworkOfferingId();
        when(_networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(_networkOfferingMock);
        doReturn(10L).when(_networkOfferingMock).getId();

        List<Service> services = new ArrayList<Service>();
        services.add(Service.Dhcp);
        when(_networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        when(_vmMock.getState()).thenReturn(State.Stopped);
        lenient().doNothing().when(_accountMgr).checkAccess(_account, null, true, _vmMock);
        when(_accountDao.findByIdIncludingRemoved(anyLong())).thenReturn(_accountMock);

        when(_networkMock.getState()).thenReturn(Network.State.Implemented);
        when(_networkMock.getDataCenterId()).thenReturn(3L);
        when(_networkMock.getGuestType()).thenReturn(GuestType.Shared);
        when(_dcDao.findById(anyLong())).thenReturn(_dcMock);
        when(_dcMock.getNetworkType()).thenReturn(NetworkType.Advanced);

        lenient().when(_ipAddrMgr.allocatePublicIpForGuestNic(Mockito.eq(_networkMock), anyLong(), Mockito.eq(_accountMock), anyString())).thenReturn(null);

        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, caller);
        try {
            _userVmMgr.updateNicIpForVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testApplyUserDataInNetworkWithoutUserDataSupport() throws Exception {
        UserVm userVm = mock(UserVm.class);
        when(userVm.getId()).thenReturn(1L);

        when(_nicMock.getNetworkId()).thenReturn(2L);
        when(_networkMock.getNetworkOfferingId()).thenReturn(3L);
        when(_networkDao.findById(2L)).thenReturn(_networkMock);

        // No userdata support
        assertFalse(_userVmMgr.applyUserData(HypervisorType.KVM, userVm, _nicMock));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testApplyUserDataInNetworkWithoutElement() throws Exception {
        UserVm userVm = mock(UserVm.class);
        lenient().when(userVm.getId()).thenReturn(1L);

        when(_nicMock.getNetworkId()).thenReturn(2L);
        when(_networkMock.getNetworkOfferingId()).thenReturn(3L);
        when(_networkDao.findById(2L)).thenReturn(_networkMock);

        UserDataServiceProvider userDataServiceProvider = mock(UserDataServiceProvider.class);
        lenient().when(userDataServiceProvider.saveUserData(any(Network.class), any(NicProfile.class), any(VirtualMachineProfile.class))).thenReturn(true);

        // Userdata support, but no implementing element
        when(_networkModel.areServicesSupportedByNetworkOffering(3L, Service.UserData)).thenReturn(true);
        _userVmMgr.applyUserData(HypervisorType.KVM, userVm, _nicMock);
    }

    @Test
    public void testApplyUserDataSuccessful() throws Exception {
        UserVm userVm = mock(UserVm.class);
        lenient().when(userVm.getId()).thenReturn(1L);

        when(_nicMock.getNetworkId()).thenReturn(2L);
        when(_networkMock.getNetworkOfferingId()).thenReturn(3L);
        when(_networkDao.findById(2L)).thenReturn(_networkMock);

        UserDataServiceProvider userDataServiceProvider = mock(UserDataServiceProvider.class);
        when(userDataServiceProvider.saveUserData(any(Network.class), any(NicProfile.class), any(VirtualMachineProfile.class))).thenReturn(true);

        // Userdata support with implementing element
        when(_networkModel.areServicesSupportedByNetworkOffering(3L, Service.UserData)).thenReturn(true);
        when(_networkModel.getUserDataUpdateProvider(_networkMock)).thenReturn(userDataServiceProvider);
        assertTrue(_userVmMgr.applyUserData(HypervisorType.KVM, userVm, _nicMock));
    }

    @Test
    public void testPersistDeviceBusInfoWithNullController() {
        when(_vmMock.getDetail(any(String.class))).thenReturn(null);
        _userVmMgr.persistDeviceBusInfo(_vmMock, null);
        verify(_vmDao, times(0)).saveDetails(any(UserVmVO.class));
    }

    @Test
    public void testPersistDeviceBusInfoWithEmptyController() {
        when(_vmMock.getDetail(any(String.class))).thenReturn("");
        _userVmMgr.persistDeviceBusInfo(_vmMock, "");
        verify(_vmDao, times(0)).saveDetails(any(UserVmVO.class));
    }

    @Test
    public void testPersistDeviceBusInfo() {
        when(_vmMock.getDetail(any(String.class))).thenReturn(null);
        _userVmMgr.persistDeviceBusInfo(_vmMock, "lsilogic");
        verify(_vmDao, times(1)).saveDetails(any(UserVmVO.class));
    }

    @Test
    public void testValideBase64WithoutPadding() {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm8";
        String encodedUserdataWithPadding = "Zm8=";

        // Verify that we accept both but return the padded version
        assertTrue("validate return the value with padding", encodedUserdataWithPadding.equals(_userVmMgr.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET)));
        assertTrue("validate return the value with padding", encodedUserdataWithPadding.equals(_userVmMgr.validateUserData(encodedUserdataWithPadding, BaseCmd.HTTPMethod.GET)));
    }

    @Test
    public void testValidateUrlEncodedBase64() throws UnsupportedEncodingException {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm+8/w8=";
        String urlEncodedUserdata = java.net.URLEncoder.encode(encodedUserdata, "UTF-8");

        // Verify that we accept both but return the padded version
        assertEquals("validate return the value with padding", encodedUserdata, _userVmMgr.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET));
        assertEquals("validate return the value with padding", encodedUserdata, _userVmMgr.validateUserData(urlEncodedUserdata, BaseCmd.HTTPMethod.GET));
    }
}
