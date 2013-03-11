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
package com.cloud.vm.snapshot;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;



public class VMSnapshotManagerTest {
    @Spy VMSnapshotManagerImpl _vmSnapshotMgr = new VMSnapshotManagerImpl();
    @Mock Account admin;
    @Mock VMSnapshotDao _vmSnapshotDao;
    @Mock VolumeDao _volumeDao;
    @Mock AccountDao _accountDao;
    @Mock VMInstanceDao _vmInstanceDao;
    @Mock UserVmDao _userVMDao;
    @Mock HostDao _hostDao;
    @Mock UserDao _userDao;
    @Mock AgentManager _agentMgr;
    @Mock HypervisorGuruManager _hvGuruMgr;
    @Mock AccountManager _accountMgr;
    @Mock GuestOSDao _guestOSDao;
    @Mock PrimaryDataStoreDao _storagePoolDao;
    @Mock SnapshotDao _snapshotDao;
    @Mock VirtualMachineManager _itMgr;
    @Mock ConfigurationDao _configDao;
    int _vmSnapshotMax = 10;
    
    private static long TEST_VM_ID = 3L;
    @Mock UserVmVO vmMock;
    @Mock VolumeVO volumeMock;
    
    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        doReturn(admin).when(_vmSnapshotMgr).getCaller();
        _vmSnapshotMgr._accountDao = _accountDao;
        _vmSnapshotMgr._userVMDao = _userVMDao;
        _vmSnapshotMgr._vmSnapshotDao = _vmSnapshotDao;
        _vmSnapshotMgr._volumeDao = _volumeDao;
        _vmSnapshotMgr._accountMgr = _accountMgr;
        _vmSnapshotMgr._snapshotDao = _snapshotDao;
        _vmSnapshotMgr._guestOSDao = _guestOSDao;
        
        doNothing().when(_accountMgr).checkAccess(any(Account.class), any(AccessType.class), 
                any(Boolean.class), any(ControlledEntity.class));
        
        _vmSnapshotMgr._vmSnapshotMax = _vmSnapshotMax;
        
        when(_userVMDao.findById(anyLong())).thenReturn(vmMock);
        when(_vmSnapshotDao.findByName(anyLong(), anyString())).thenReturn(null);
        when(_vmSnapshotDao.findByVm(anyLong())).thenReturn(new ArrayList<VMSnapshotVO>());
       
        List<VolumeVO> mockVolumeList = new ArrayList<VolumeVO>();
        mockVolumeList.add(volumeMock);
        when(volumeMock.getInstanceId()).thenReturn(TEST_VM_ID);
        when(_volumeDao.findByInstance(anyLong())).thenReturn(mockVolumeList);
        
        when(vmMock.getInstanceName()).thenReturn("i-3-VM-TEST");
        when(vmMock.getState()).thenReturn(State.Running);
        
        when(_guestOSDao.findById(anyLong())).thenReturn(mock(GuestOSVO.class));
    } 
    
    // vmId null case
    @Test(expected=InvalidParameterValueException.class)
    public void testAllocVMSnapshotF1() throws ResourceAllocationException{
        when(_userVMDao.findById(TEST_VM_ID)).thenReturn(null);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
    }

    // vm state not in [running, stopped] case
    @Test(expected=InvalidParameterValueException.class)
    public void testAllocVMSnapshotF2() throws ResourceAllocationException{
        when(vmMock.getState()).thenReturn(State.Starting);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
    }
    
    // VM in stopped state & snapshotmemory case
    @Test(expected=InvalidParameterValueException.class)
    public void testCreateVMSnapshotF3() throws AgentUnavailableException, OperationTimedoutException, ResourceAllocationException{
        when(vmMock.getState()).thenReturn(State.Stopped);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
    }
    
    // max snapshot limit case
    @SuppressWarnings("unchecked")
    @Test(expected=CloudRuntimeException.class)
    public void testAllocVMSnapshotF4() throws ResourceAllocationException{
        List<VMSnapshotVO> mockList = mock(List.class);
        when(mockList.size()).thenReturn(10);
        when(_vmSnapshotDao.findByVm(TEST_VM_ID)).thenReturn(mockList);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
    }
    
    // active volume snapshots case
    @SuppressWarnings("unchecked")
    @Test(expected=CloudRuntimeException.class)
    public void testAllocVMSnapshotF5() throws ResourceAllocationException{
        List<SnapshotVO> mockList = mock(List.class);
        when(mockList.size()).thenReturn(1);
        when(_snapshotDao.listByInstanceId(TEST_VM_ID,Snapshot.State.Creating,
                Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp)).thenReturn(mockList);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
    }
    
    // successful creation case
    @Test
    public void testCreateVMSnapshot() throws AgentUnavailableException, OperationTimedoutException, ResourceAllocationException, NoTransitionException{
        when(vmMock.getState()).thenReturn(State.Running);
        _vmSnapshotMgr.allocVMSnapshot(TEST_VM_ID,"","",true);
        
        when(_vmSnapshotDao.findCurrentSnapshotByVmId(anyLong())).thenReturn(null);
        doReturn(new ArrayList<VolumeTO>()).when(_vmSnapshotMgr).getVolumeTOList(anyLong());
        doReturn(new CreateVMSnapshotAnswer(null,true,"")).when(_vmSnapshotMgr).sendToPool(anyLong(), any(CreateVMSnapshotCommand.class));
        doNothing().when(_vmSnapshotMgr).processAnswer(any(VMSnapshotVO.class), 
                any(UserVmVO.class), any(Answer.class), anyLong());
        doReturn(true).when(_vmSnapshotMgr).vmSnapshotStateTransitTo(any(VMSnapshotVO.class),any(VMSnapshot.Event.class));
        _vmSnapshotMgr.createVmSnapshotInternal(vmMock, mock(VMSnapshotVO.class), 5L);
    }

}
