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

import java.util.List;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;

import static org.mockito.Mockito.*;

public class UserVmManagerTest {

    @Spy UserVmManagerImpl _userVmMgr = new UserVmManagerImpl();
    @Mock VirtualMachineManager _itMgr;
    @Mock StorageManager _storageMgr;
    @Mock Account account;
    @Mock AccountManager _accountMgr;
    @Mock AccountDao _accountDao;
    @Mock UserDao _userDao;
    @Mock UserVmDao _vmDao;
    @Mock VMTemplateDao _templateDao;
    @Mock VolumeDao _volsDao;
    @Mock RestoreVMCmd restoreVMCmd;
    @Mock AccountVO accountMock;
    @Mock UserVO userMock;
    @Mock UserVmVO vmMock;
    @Mock VMTemplateVO templateMock;
    @Mock VolumeVO volumeMock;
    @Mock List<VolumeVO> rootVols;
    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        _userVmMgr._vmDao = _vmDao;
        _userVmMgr._templateDao = _templateDao;
        _userVmMgr._volsDao = _volsDao;
        _userVmMgr._itMgr = _itMgr;
        _userVmMgr._storageMgr = _storageMgr;
        _userVmMgr._accountDao = _accountDao;
        _userVmMgr._userDao = _userDao;

        doReturn(3L).when(account).getId();
        doReturn(8L).when(vmMock).getAccountId();
        when(_accountDao.findById(anyLong())).thenReturn(accountMock);
        when(_userDao.findById(anyLong())).thenReturn(userMock);
        doReturn(Account.State.enabled).when(account).getState();
        when(vmMock.getId()).thenReturn(314L);

    }

    // VM state not in running/stopped case
    @Test(expected=CloudRuntimeException.class)
    public void testRestoreVMF1() throws ResourceAllocationException {

        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(_templateDao.findById(anyLong())).thenReturn(templateMock);
        doReturn(VirtualMachine.State.Error).when(vmMock).getState();
        _userVmMgr.restoreVMInternal(account, vmMock);
    }

    // when VM is in stopped state
    @Test
    public void testRestoreVMF2()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
    ConcurrentOperationException, ResourceAllocationException {

        doReturn(VirtualMachine.State.Stopped).when(vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(_volsDao.findByInstance(anyLong())).thenReturn(rootVols);
        doReturn(false).when(rootVols).isEmpty();
        when(rootVols.get(eq(0))).thenReturn(volumeMock);
        doReturn(3L).when(volumeMock).getTemplateId();
        when(_templateDao.findById(anyLong())).thenReturn(templateMock);
        when(_storageMgr.allocateDuplicateVolume(volumeMock, null)).thenReturn(volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());
        when(_storageMgr.destroyVolume(volumeMock)).thenReturn(true);

        _userVmMgr.restoreVMInternal(account, vmMock);

    }

    // when VM is in running state
    @Test
    public void testRestoreVMF3()  throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
    ConcurrentOperationException, ResourceAllocationException {

        doReturn(VirtualMachine.State.Running).when(vmMock).getState();
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(_volsDao.findByInstance(anyLong())).thenReturn(rootVols);
        doReturn(false).when(rootVols).isEmpty();
        when(rootVols.get(eq(0))).thenReturn(volumeMock);
        doReturn(3L).when(volumeMock).getTemplateId();
        when(_templateDao.findById(anyLong())).thenReturn(templateMock);
        when(_itMgr.stop(vmMock, userMock, account)).thenReturn(true);
        when(_itMgr.start(vmMock, null, userMock, account)).thenReturn(vmMock);
        when(_storageMgr.allocateDuplicateVolume(volumeMock, null)).thenReturn(volumeMock);
        doNothing().when(_volsDao).attachVolume(anyLong(), anyLong(), anyLong());
        when(volumeMock.getId()).thenReturn(3L);
        doNothing().when(_volsDao).detachVolume(anyLong());
        when(_storageMgr.destroyVolume(volumeMock)).thenReturn(true);

        _userVmMgr.restoreVMInternal(account, vmMock);

    }

}