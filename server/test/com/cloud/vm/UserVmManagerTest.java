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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class UserVmManagerTest {

    @Spy UserVmManagerImpl _userVmMgr = new UserVmManagerImpl();
    @Mock VirtualMachineManager _itMgr;
    @Mock VolumeManager _storageMgr;
    @Mock Account _account;
    @Mock AccountManager _accountMgr;
    @Mock AccountService _accountService;
    @Mock ConfigurationManager _configMgr;
    @Mock CapacityManager _capacityMgr;
    @Mock AccountDao _accountDao;
    @Mock
    ConfigurationDao _configDao;
    @Mock UserDao _userDao;
    @Mock UserVmDao _vmDao;
    @Mock VMInstanceDao _vmInstanceDao;
    @Mock VMTemplateDao _templateDao;
    @Mock VolumeDao _volsDao;
    @Mock RestoreVMCmd _restoreVMCmd;
    @Mock AccountVO _accountMock;
    @Mock UserVO _userMock;
    @Mock UserVmVO _vmMock;
    @Mock VMInstanceVO _vmInstance;
    @Mock VMTemplateVO _templateMock;
    @Mock VolumeVO _volumeMock;
    @Mock List<VolumeVO> _rootVols;
    @Mock Account _accountMock2;
    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        _userVmMgr._vmDao = _vmDao;
        _userVmMgr._vmInstanceDao = _vmInstanceDao;
        _userVmMgr._templateDao = _templateDao;
        _userVmMgr._volsDao = _volsDao;
        _userVmMgr._itMgr = _itMgr;
        _userVmMgr.volumeMgr = _storageMgr;
        _userVmMgr._accountDao = _accountDao;
        _userVmMgr._accountService = _accountService;
        _userVmMgr._userDao = _userDao;
        _userVmMgr._accountMgr = _accountMgr;
        _userVmMgr._configMgr = _configMgr;
        _userVmMgr._capacityMgr = _capacityMgr;
        _userVmMgr._scaleRetry = 2;

        doReturn(3L).when(_account).getId();
        doReturn(8L).when(_vmMock).getAccountId();
        when(_accountDao.findById(anyLong())).thenReturn(_accountMock);
        when(_userDao.findById(anyLong())).thenReturn(_userMock);
        doReturn(Account.State.enabled).when(_account).getState();
        when(_vmMock.getId()).thenReturn(314L);
        when(_vmInstance.getId()).thenReturn(1L);
        when(_vmInstance.getServiceOfferingId()).thenReturn(2L);

    }

    // Test restoreVm when VM state not in running/stopped case
    @Test(expected=CloudRuntimeException.class)
    public void testRestoreVMF1() throws ResourceAllocationException, InsufficientCapacityException, ResourceUnavailableException {

        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_templateDao.findById(anyLong())).thenReturn(_templateMock);
        doReturn(VirtualMachine.State.Error).when(_vmMock).getState();
        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.restoreVMInternal(_account, _vmMock, null);
        } finally {
            CallContext.unregister();
        }
    }

    // Test restoreVm when VM is in stopped state
    @Test
    public void testRestoreVMF2()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException {

        doReturn(VirtualMachine.State.Stopped).when(_vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_volsDao.findByInstanceAndType(314L,Volume.Type.ROOT)).thenReturn(_rootVols);
        doReturn(false).when(_rootVols).isEmpty();
        when(_rootVols.get(eq(0))).thenReturn(_volumeMock);
        doReturn(3L).when(_volumeMock).getTemplateId();
        when(_templateDao.findById(anyLong())).thenReturn(_templateMock);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, null)).thenReturn(_volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(_volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());
      
        when(_templateMock.getUuid()).thenReturn("e0552266-7060-11e2-bbaa-d55f5db67735");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.restoreVMInternal(_account, _vmMock, null);
        } finally {
            CallContext.unregister();
        }

    }

    // Test restoreVM when VM is in running state
    @Test
    public void testRestoreVMF3()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException {

        doReturn(VirtualMachine.State.Running).when(_vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_volsDao.findByInstanceAndType(314L,Volume.Type.ROOT)).thenReturn(_rootVols);
        doReturn(false).when(_rootVols).isEmpty();
        when(_rootVols.get(eq(0))).thenReturn(_volumeMock);
        doReturn(3L).when(_volumeMock).getTemplateId();
        when(_templateDao.findById(anyLong())).thenReturn(_templateMock);
        when(_itMgr.stop(_vmMock, _userMock, _account)).thenReturn(true);
        when(_itMgr.start(_vmMock, null, _userMock, _account)).thenReturn(_vmMock);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, null)).thenReturn(_volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(_volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());
     
        when(_templateMock.getUuid()).thenReturn("e0552266-7060-11e2-bbaa-d55f5db67735");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.restoreVMInternal(_account, _vmMock, null);
        } finally {
            CallContext.unregister();
        }

    }

    // Test restoreVM on providing new template Id, when VM is in running state
    @Test
    public void testRestoreVMF4()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
        doReturn(VirtualMachine.State.Running).when(_vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_volsDao.findByInstanceAndType(314L,Volume.Type.ROOT)).thenReturn(_rootVols);
        doReturn(false).when(_rootVols).isEmpty();
        when(_rootVols.get(eq(0))).thenReturn(_volumeMock);
        doReturn(3L).when(_volumeMock).getTemplateId();
        doReturn(ImageFormat.VHD).when(_templateMock).getFormat();
        when(_templateDao.findById(anyLong())).thenReturn(_templateMock);
        doNothing().when(_accountMgr).checkAccess(_account, null, true, _templateMock);
        when(_itMgr.stop(_vmMock, _userMock, _account)).thenReturn(true);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, 14L)).thenReturn(_volumeMock);
        when(_templateMock.getGuestOSId()).thenReturn(5L);
        doNothing().when(_vmMock).setGuestOSId(anyLong());
        doNothing().when(_vmMock).setTemplateId(3L);
        when(_vmDao.update(314L, _vmMock)).thenReturn(true);
        when(_itMgr.start(_vmMock, null, _userMock, _account)).thenReturn(_vmMock);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, null)).thenReturn(_volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(_volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());
      
        when(_templateMock.getUuid()).thenReturn("b1a3626e-72e0-4697-8c7c-a110940cc55d");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.restoreVMInternal(_account, _vmMock, 14L);
        } finally {
            CallContext.unregister();
        }

    }

    // Test restoreVM on providing new ISO Id, when VM(deployed using ISO) is in running state
    @Test
    public void testRestoreVMF5()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
        doReturn(VirtualMachine.State.Running).when(_vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);
        when(_volsDao.findByInstanceAndType(314L, Volume.Type.ROOT)).thenReturn(_rootVols);
        doReturn(false).when(_rootVols).isEmpty();
        when(_rootVols.get(eq(0))).thenReturn(_volumeMock);
        doReturn(null).when(_volumeMock).getTemplateId();
        doReturn(3L).when(_vmMock).getIsoId();
        doReturn(ImageFormat.ISO).when(_templateMock).getFormat();
        when(_templateDao.findById(anyLong())).thenReturn(_templateMock);
        doNothing().when(_accountMgr).checkAccess(_account, null, true, _templateMock);
        when(_itMgr.stop(_vmMock, _userMock, _account)).thenReturn(true);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, null)).thenReturn(_volumeMock);
        doNothing().when(_vmMock).setIsoId(14L);
        when(_templateMock.getGuestOSId()).thenReturn(5L);
        doNothing().when(_vmMock).setGuestOSId(anyLong());
        doNothing().when(_vmMock).setTemplateId(3L);
        when(_vmDao.update(314L, _vmMock)).thenReturn(true);
        when(_itMgr.start(_vmMock, null, _userMock, _account)).thenReturn(_vmMock);
        when(_storageMgr.allocateDuplicateVolume(_volumeMock, null)).thenReturn(_volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(_volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());

        when(_templateMock.getUuid()).thenReturn("b1a3626e-72e0-4697-8c7c-a110940cc55d");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.restoreVMInternal(_account, _vmMock, 14L);
        } finally {
            CallContext.unregister();
        }

        verify(_vmMock, times(1)).setIsoId(14L);

    }
    // Test scaleVm on incompatible HV.
    @Test(expected=InvalidParameterValueException.class)
    public void testScaleVMF1()  throws Exception {

        ScaleVMCmd cmd = new ScaleVMCmd();
        Class<?> _class = cmd.getClass();

        Field idField = _class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cmd, 1L);

        Field serviceOfferingIdField = _class.getDeclaredField("serviceOfferingId");
        serviceOfferingIdField.setAccessible(true);
        serviceOfferingIdField.set(cmd, 1L);

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);

       // UserContext.current().setEventDetails("Vm Id: "+getId());
        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short) 0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }


    }

    // Test scaleVm on equal service offerings.
    @Test(expected=InvalidParameterValueException.class)
    public void testScaleVMF2()  throws Exception {

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

        doReturn(VirtualMachine.State.Running).when(_vmInstance).getState();

        doNothing().when(_accountMgr).checkAccess(_account, null, true, _templateMock);

        doNothing().when(_itMgr).checkIfCanUpgrade(_vmMock, cmd.getServiceOfferingId());


        ServiceOffering so1 =  getSvcoffering(512);
        ServiceOffering so2 =  getSvcoffering(256);

        when(_configMgr.getServiceOffering(anyLong())).thenReturn(so1);
        when(_configMgr.getServiceOffering(1L)).thenReturn(so1);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    // Test scaleVm for Stopped vm.
    @Test(expected=InvalidParameterValueException.class)
    public void testScaleVMF3()  throws Exception {

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


        ServiceOffering so1 =  getSvcoffering(512);
        ServiceOffering so2 =  getSvcoffering(256);

        when(_configMgr.getServiceOffering(anyLong())).thenReturn(so2);
        when(_configMgr.getServiceOffering(1L)).thenReturn(so1);

        doReturn(VirtualMachine.State.Stopped).when(_vmInstance).getState();
        when(_vmDao.findById(anyLong())).thenReturn(null);


        doReturn(true).when(_itMgr).upgradeVmDb(anyLong(),anyLong());

        //when(_vmDao.findById(anyLong())).thenReturn(_vmMock);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    // Test scaleVm for Running vm. Full positive test.
    public void testScaleVMF4()  throws Exception {

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

        ServiceOffering so1 =  getSvcoffering(512);
        ServiceOffering so2 =  getSvcoffering(256);

        when(_configMgr.getServiceOffering(anyLong())).thenReturn(so2);
        when(_configMgr.getServiceOffering(1L)).thenReturn(so1);

        doReturn(VirtualMachine.State.Running).when(_vmInstance).getState();

        //when(ApiDBUtils.getCpuOverprovisioningFactor()).thenReturn(3f);
        when(_capacityMgr.checkIfHostHasCapacity(anyLong(), anyInt(), anyLong(), anyBoolean(), anyFloat(), anyFloat(),  anyBoolean())).thenReturn(false);
        when(_itMgr.reConfigureVm(_vmInstance, so1, false)).thenReturn(_vmInstance);

        doReturn(true).when(_itMgr).upgradeVmDb(anyLong(), anyLong());

        when(_vmDao.findById(anyLong())).thenReturn(_vmMock);

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, account);
        try {
            _userVmMgr.upgradeVirtualMachine(cmd);
        } finally {
            CallContext.unregister();
        }

    }

    private ServiceOfferingVO getSvcoffering(int ramSize){

        long id  = 4L;
        String name = "name";
        String displayText = "displayText";
        int cpu = 1;
        //int ramSize = 256;
        int speed = 128;

        boolean ha = false;
        boolean useLocalStorage = false;

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText, useLocalStorage, false, null, false, null, false);
        return serviceOffering;
    }

    // Test Move VM b/w accounts where caller is not ROOT/Domain admin
    @Test(expected=InvalidParameterValueException.class)
    public void testMoveVmToUser1()  throws Exception {
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
        Account caller = new AccountVO("testaccount", 1, "networkdomain", (short) 0,
                UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, caller);
        try {

        _userVmMgr.moveVMToUser(cmd);
        } finally {
            CallContext.unregister();
        }
    }


    // Test Move VM b/w accounts where caller doesn't have access to the old or new account
    @Test(expected=PermissionDeniedException.class)
    public void testMoveVmToUser2()  throws Exception {
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
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());


        Account oldAccount = new AccountVO("testaccount", 1, "networkdomain", (short) 0,
                UUID.randomUUID().toString());
        Account newAccount = new AccountVO("testaccount", 1, "networkdomain", (short) 1,
                UUID.randomUUID().toString());

        UserVmVO vm = new UserVmVO(10L, "test", "test", 1L, HypervisorType.Any, 1L, false, false, 1L, 1L,
                5L, "test", "test", 1L);
        vm.setState(VirtualMachine.State.Stopped);
        when(_vmDao.findById(anyLong())).thenReturn(vm);

        when(_accountService.getActiveAccountById(anyLong())).thenReturn(oldAccount);

        when(_accountService.getActiveAccountByName(anyString(), anyLong())).thenReturn(newAccount);

        doThrow(new PermissionDeniedException("Access check failed")).when(_accountMgr).checkAccess(any(Account.class), any(AccessType.class),
                any(Boolean.class), any(ControlledEntity.class));

        CallContext.register(user, caller);
        try {
            _userVmMgr.moveVMToUser(cmd);
        } finally {
            CallContext.unregister();
        }
    }

}