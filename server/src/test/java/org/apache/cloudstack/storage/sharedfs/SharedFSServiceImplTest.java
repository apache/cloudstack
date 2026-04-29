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

package org.apache.cloudstack.storage.sharedfs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.sharedfs.ChangeSharedFSDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.ChangeSharedFSServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.CreateSharedFSCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.DestroySharedFSCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.ListSharedFSCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.UpdateSharedFSCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.sharedfs.dao.SharedFSDao;
import org.apache.cloudstack.storage.sharedfs.query.dao.SharedFSJoinDao;
import org.apache.cloudstack.storage.sharedfs.query.vo.SharedFSJoinVO;
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

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.org.Grouping;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;

@RunWith(MockitoJUnitRunner.class)
public class SharedFSServiceImplTest {

    @Mock
    private AccountManager accountMgr;

    @Mock
    private SharedFSDao sharedFSDao;

    @Mock
    private SharedFSJoinDao sharedFSJoinDao;

    @Mock
    private DataCenterDao dataCenterDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    NicDao nicDao;

    @Mock
    NetworkDao networkDao;

    @Mock
    private ConfigurationManager configMgr;

    @Mock
    private VolumeApiService volumeApiService;

    @Mock
    private SharedFSProvider provider;

    @Mock
    private SharedFSLifeCycle lifeCycle;

    @Spy
    @InjectMocks
    private SharedFSServiceImpl sharedFSServiceImpl;

    private static final long s_ownerId = 1L;
    private static final long s_zoneId = 2L;
    private static final long s_diskOfferingId = 3L;
    private static final long s_serviceOfferingId = 4L;
    private static final long s_domainId = 5L;
    private static final long s_volumeId = 6L;
    private static final long s_vmId = 7L;
    private static final long s_networkId = 8L;
    private static final long s_sharedFSId = 9L;
    private static final long s_size = 10L;
    private static final long s_minIops = 1000L;
    private static final long s_maxIops = 2000L;
    private static final String s_providerName = "SHAREDFSVM";
    private static final String s_fsFormat = "EXT4";
    private static final String s_name = "TestSharedFS";
    private static final String s_description = "Test Description";

    @Mock
    Account owner;
    @Mock
    protected StateMachine2<SharedFS.State, SharedFS.Event, SharedFS> _stateMachine;

    private MockedStatic<CallContext> callContextMocked;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        callContextMocked = mockStatic(CallContext.class);
        CallContext callContextMock = mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        when(callContextMock.getCallingAccount()).thenReturn(owner);
        when(accountMgr.getActiveAccountById(s_ownerId)).thenReturn(owner);

        Map<String, SharedFSProvider> mockProviderMap = new HashMap<>();
        mockProviderMap.put(s_providerName, provider);
        ReflectionTestUtils.setField(sharedFSServiceImpl, "sharedFSProviderMap", mockProviderMap);
        when(sharedFSServiceImpl.getSharedFSProvider(s_providerName)).thenReturn(provider);
        when(provider.getSharedFSLifeCycle()).thenReturn(lifeCycle);
        ReflectionTestUtils.setField(sharedFSServiceImpl, "sharedFSStateMachine", _stateMachine);
    }

    @After
    public void tearDown() throws Exception {
        callContextMocked.close();
        closeable.close();
    }

    private CreateSharedFSCmd getMockCreateSharedFSCmd() {
        CreateSharedFSCmd cmd = mock(CreateSharedFSCmd.class);
        when(cmd.getEntityOwnerId()).thenReturn(s_ownerId);
        when(cmd.getZoneId()).thenReturn(s_zoneId);
        when(cmd.getDiskOfferingId()).thenReturn(s_diskOfferingId);
        when(cmd.getSize()).thenReturn(s_size);
        when(cmd.getMinIops()).thenReturn(s_minIops);
        when(cmd.getMaxIops()).thenReturn(s_maxIops);
        when(cmd.getSharedFSProviderName()).thenReturn(s_providerName);
        when(cmd.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(cmd.getNetworkId()).thenReturn(s_networkId);
        when(cmd.getFsFormat()).thenReturn(s_fsFormat);
        return cmd;
    }

    private SharedFSVO getMockSharedFS() {
        SharedFSVO sharedFS = new SharedFSVO(s_name, s_description, s_domainId, s_ownerId, s_zoneId,
                s_providerName, SharedFS.Protocol.NFS, SharedFS.FileSystemType.valueOf(s_fsFormat), s_serviceOfferingId);
        return sharedFS;
    }

    @Test
    public void testDeploySharedFS() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException, OperationTimedoutException {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(0L)).thenReturn(sharedFS);

        Pair<Long, Long> result = new Pair<>(s_volumeId, s_vmId);
        when(lifeCycle.deploySharedFS(sharedFS, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops)).thenReturn(result);
        when(sharedFSDao.update(sharedFS.getId(), sharedFS)).thenReturn(true);

        Assert.assertEquals(sharedFSServiceImpl.deploySharedFS(cmd), sharedFS);
        Assert.assertEquals(Optional.ofNullable(sharedFS.getVmId()), Optional.ofNullable(s_vmId));
        Assert.assertEquals(Optional.ofNullable(sharedFS.getVolumeId()), Optional.ofNullable(s_volumeId));
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationSucceeded, null, sharedFSDao);
    }

    @Test
    public void testDeploySharedFSException() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException, OperationTimedoutException {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(0L)).thenReturn(sharedFS);

        when(lifeCycle.deploySharedFS(sharedFS, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops)).thenThrow(new CloudRuntimeException(""));

        Assert.assertThrows(CloudRuntimeException.class, () -> sharedFSServiceImpl.deploySharedFS(cmd));
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationFailed, null, sharedFSDao);
        verify(_stateMachine, never()).transitTo(sharedFS, SharedFS.Event.OperationSucceeded, null, sharedFSDao);
    }

    @Test
    public void testAllocSharedFS() throws NoTransitionException {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        when(dataCenterDao.findById(s_zoneId)).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);

        when(cmd.getNetworkId()).thenReturn(s_networkId);
        NetworkVO networkVO = mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkDao.findById(s_networkId)).thenReturn(networkVO);

        sharedFSServiceImpl.allocSharedFS(cmd);
        Assert.assertEquals(Optional.ofNullable(sharedFS.getAccountId()), Optional.ofNullable(s_ownerId));
        Assert.assertEquals(Optional.ofNullable(sharedFS.getDataCenterId()), Optional.ofNullable(s_zoneId));
        Assert.assertEquals(Optional.ofNullable(sharedFS.getServiceOfferingId()), Optional.ofNullable(s_serviceOfferingId));
    }

    @Test
    public void testAllocSharedFSInvalidZone() {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        when(dataCenterDao.findById(s_zoneId)).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);
        Assert.assertThrows(PermissionDeniedException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));

        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        when(zone.isSecurityGroupEnabled()).thenReturn(true);
        Assert.assertThrows(PermissionDeniedException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));
    }

    @Test
    public void tesAllocSharedFSInvalidDiskOffering() {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(false);
        Assert.assertThrows(InvalidParameterValueException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));

        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(false);
        Assert.assertThrows(InvalidParameterValueException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));
    }

    @Test
    public void testAllocSharedFSInvalidFsFormat() {
        CreateSharedFSCmd cmd = getMockCreateSharedFSCmd();

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        when(cmd.getNetworkId()).thenReturn(s_networkId);
        NetworkVO networkVO = mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkDao.findById(s_networkId)).thenReturn(networkVO);

        when(cmd.getFsFormat()).thenReturn("ext2");
        Assert.assertThrows(InvalidParameterValueException.class, () -> sharedFSServiceImpl.allocSharedFS(cmd));
    }

    @Test
    public void testStartSharedFS() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException, NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);

        Assert.assertEquals(sharedFSServiceImpl.startSharedFS(s_sharedFSId), sharedFS);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.StartRequested, null, sharedFSDao);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationSucceeded, null, sharedFSDao);
    }

    @Test
    public void testStartSharedFSException() throws ResourceUnavailableException, InsufficientCapacityException, OperationTimedoutException, NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        doThrow(CloudRuntimeException.class).when(lifeCycle).startSharedFS(sharedFS);

        Assert.assertThrows(CloudRuntimeException.class, () -> sharedFSServiceImpl.startSharedFS(s_sharedFSId));
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.StartRequested, null, sharedFSDao);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationFailed, null, sharedFSDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartSharedFSInvalidState() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);
        sharedFSServiceImpl.startSharedFS(s_sharedFSId);
    }

    @Test
    public void testStopSharedFS() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);
        Assert.assertEquals(sharedFSServiceImpl.stopSharedFS(s_sharedFSId, false), sharedFS);
        verify(lifeCycle, Mockito.times(1)).stopSharedFS(any(), any());
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.StopRequested, null, sharedFSDao);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationSucceeded, null, sharedFSDao);
    }

    @Test
    public void testStopSharedFSException() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);
        doThrow(CloudRuntimeException.class).when(lifeCycle).stopSharedFS(sharedFS, false);

        Assert.assertThrows(CloudRuntimeException.class, () -> sharedFSServiceImpl.stopSharedFS(s_sharedFSId, false));
        verify(lifeCycle, Mockito.times(1)).stopSharedFS(any(), any());
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.StopRequested, null, sharedFSDao);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationFailed, null, sharedFSDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStopSharedFSInvalidState() {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        sharedFSServiceImpl.stopSharedFS(s_sharedFSId, false);
    }

    @Test
    public void testRestartSharedFSWithoutCleanup() throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        sharedFSServiceImpl.restartSharedFS(s_sharedFSId, false);
        verify(lifeCycle, never()).stopSharedFS(any(), any());
        verify(lifeCycle, Mockito.times(1)).startSharedFS(any());
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.StartRequested, null, sharedFSDao);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationSucceeded, null, sharedFSDao);
    }

    @Test
    public void testRestartSharedFSWithCleanup() throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);

        DataCenterVO zone = mock(DataCenterVO.class);

        when(lifeCycle.reDeploySharedFS(sharedFS)).thenReturn(true);
        sharedFSServiceImpl.restartSharedFS(s_sharedFSId, true);
        verify(lifeCycle, never()).stopSharedFS(any(), any());
    }

    @Test
    public void testUpdateSharedFS() {
        String newName = "New SharedFS";
        String newDescription = "New SharedFS Description";
        UpdateSharedFSCmd cmd = mock(UpdateSharedFSCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);
        when(cmd.getName()).thenReturn(newName);
        when(cmd.getDescription()).thenReturn(newDescription);

        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);

        sharedFSServiceImpl.updateSharedFS(cmd);
        Assert.assertEquals(sharedFS.getName(), newName);
        Assert.assertEquals(sharedFS.getDescription(), newDescription);
    }

    @Test
    public void testChangeSharedFSDiskOffering() throws ResourceAllocationException {
        Long newSize = 200L;
        Long newMinIops = 2000L;
        Long newMaxIops = 4000L;
        Long newDiskOfferingId = 10L;
        ChangeSharedFSDiskOfferingCmd cmd = mock(ChangeSharedFSDiskOfferingCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);
        when(cmd.getDiskOfferingId()).thenReturn(newDiskOfferingId);
        when(cmd.getSize()).thenReturn(newSize);
        when(cmd.getMinIops()).thenReturn(newMinIops);
        when(cmd.getMaxIops()).thenReturn(newMaxIops);

        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(newDiskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        sharedFSServiceImpl.changeSharedFSDiskOffering(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeSharedFSDiskOfferingInvalidState() throws ResourceAllocationException {
        ChangeSharedFSDiskOfferingCmd cmd = mock(ChangeSharedFSDiskOfferingCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);

        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Destroyed);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        sharedFSServiceImpl.changeSharedFSDiskOffering(cmd);
    }

    @Test
    public void testChangeSharedFSServiceOffering() throws ResourceUnavailableException, InsufficientCapacityException, ManagementServerException, OperationTimedoutException, NoTransitionException, VirtualMachineMigrationException {
        ChangeSharedFSServiceOfferingCmd cmd = mock(ChangeSharedFSServiceOfferingCmd.class);
        Long newServiceOfferingId = 100L;
        when(cmd.getServiceOfferingId()).thenReturn(newServiceOfferingId);
        when(cmd.getId()).thenReturn(s_sharedFSId);

        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        when(lifeCycle.changeSharedFSServiceOffering(sharedFS, newServiceOfferingId)).thenReturn(true);

        sharedFSServiceImpl.changeSharedFSServiceOffering(cmd);
        Assert.assertEquals(Optional.ofNullable(sharedFS.getServiceOfferingId()), Optional.ofNullable(newServiceOfferingId));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeSharedFSServiceOfferingInvalidState() throws ResourceUnavailableException, InsufficientCapacityException, ManagementServerException, OperationTimedoutException, VirtualMachineMigrationException {
        ChangeSharedFSServiceOfferingCmd cmd = mock(ChangeSharedFSServiceOfferingCmd.class);
        Long newServiceOfferingId = 100L;
        when(cmd.getId()).thenReturn(s_sharedFSId);

        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Starting);
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);

        sharedFSServiceImpl.changeSharedFSServiceOffering(cmd);
    }

    @Test
    public void testDestroySharedFS() throws NoTransitionException {
        DestroySharedFSCmd cmd = mock(DestroySharedFSCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);
        when(cmd.isExpunge()).thenReturn(false);

        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);

        Assert.assertEquals(sharedFSServiceImpl.destroySharedFS(cmd), true);
        verify(lifeCycle, never()).deleteSharedFS(any());
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.DestroyRequested, null, sharedFSDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDestroySharedFSInvalidState() {
        DestroySharedFSCmd cmd = mock(DestroySharedFSCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);
        when(cmd.isExpunge()).thenReturn(false);
        when(cmd.isForced()).thenReturn(false);

        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Ready);

        sharedFSServiceImpl.destroySharedFS(cmd);
    }

    @Test
    public void testRecoverSharedFS() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Destroyed);
        Assert.assertEquals(sharedFSServiceImpl.recoverSharedFS(s_sharedFSId), sharedFS);
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.RecoveryRequested, null, sharedFSDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRecoverSharedFSInvalidState() {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Expunged);
        sharedFSServiceImpl.recoverSharedFS(s_sharedFSId);
    }

    @Test
    public void testDeleteSharedFS() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Destroyed);
        sharedFSServiceImpl.deleteSharedFS(s_sharedFSId);
        verify(lifeCycle, Mockito.times(1)).deleteSharedFS(any());
        verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.ExpungeOperation, null, sharedFSDao);
    }

    @Test (expected = CloudRuntimeException.class)
    public void testDeleteSharedFSTransitionException() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Destroyed);
        when(_stateMachine.transitTo(sharedFS, SharedFS.Event.ExpungeOperation, null, sharedFSDao)).thenThrow(new NoTransitionException(""));
        sharedFSServiceImpl.deleteSharedFS(s_sharedFSId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteSharedFSInvalidState() {
        SharedFSVO sharedFS = getMockSharedFS();
        when(sharedFSDao.findById(s_sharedFSId)).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        sharedFSServiceImpl.deleteSharedFS(s_sharedFSId);
    }

    private ListSharedFSCmd getMockListSharedFSCmd() {
        ListSharedFSCmd cmd = mock(ListSharedFSCmd.class);
        when(cmd.getId()).thenReturn(s_sharedFSId);
        when(cmd.getName()).thenReturn(s_name);
        when(cmd.getZoneId()).thenReturn(s_zoneId);
        when(cmd.getDiskOfferingId()).thenReturn(s_diskOfferingId);
        when(cmd.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(cmd.getAccountName()).thenReturn("account");
        when(cmd.getDomainId()).thenReturn(s_domainId);
        when(cmd.getNetworkId()).thenReturn(s_networkId);
        return cmd;
    }

    @Test
    public void testSearchForSharedFS() {
        SearchBuilder<SharedFSVO> sb = mock(SearchBuilder.class);
        when(sharedFSDao.createSearchBuilder()).thenReturn(sb);

        SharedFSVO sharedFS = getMockSharedFS();
        when(sb.entity()).thenReturn(sharedFS);
        ReflectionTestUtils.setField(sharedFS, "id", s_sharedFSId);

        VolumeVO volume = mock(VolumeVO.class);
        SearchBuilder<VolumeVO> volumeSb = mock(SearchBuilder.class);
        when(volumeSb.entity()).thenReturn(volume);
        when(volumeDao.createSearchBuilder()).thenReturn(volumeSb);

        NicVO nic = mock(NicVO.class);
        SearchBuilder<NicVO> nicSb = mock(SearchBuilder.class);
        when(nicSb.entity()).thenReturn(nic);
        when(nicDao.createSearchBuilder()).thenReturn(nicSb);

        SearchCriteria<SharedFSVO> sc = mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);

        Pair<List<SharedFSVO>, Integer> result = new Pair<>(List.of(sharedFS), 1);
        when(sharedFSDao.searchAndCount(any(), any())).thenReturn(result);
        SharedFSJoinVO sharedFSJoinVO = mock(SharedFSJoinVO.class);
        when(sharedFSJoinDao.searchByIds(List.of(s_sharedFSId).toArray(new Long[0]))).thenReturn(List.of(sharedFSJoinVO));

        when(owner.getId()).thenReturn(s_ownerId);
        when(accountMgr.isRootAdmin(any())).thenReturn(true);
        when(sharedFSJoinDao.createSharedFSResponses(any(), any())).thenReturn(null);

        ListSharedFSCmd cmd = getMockListSharedFSCmd();
        sharedFSServiceImpl.searchForSharedFS(ResponseObject.ResponseView.Restricted, cmd);

        verify(sc, times(1)).setParameters("id", s_sharedFSId);
        verify(sc, times(1)).setParameters("name", s_name);
        verify(sc, times(1)).setParameters("dataCenterId", s_zoneId);
        verify(sc, times(1)).setParameters("serviceOfferingId", s_serviceOfferingId);
        verify(sc, times(1)).setJoinParameters("volSearch", "diskOfferingId", s_diskOfferingId);
        verify(sc, times(1)).setJoinParameters("nicSearch", "networkId", s_networkId);
        verify(sharedFSDao, times(1)).searchAndCount(any(), any());
    }

    @Test
    public void testCleanupSharedFS() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Destroyed);
        when(sharedFSDao.listSharedFSToBeDestroyed(any(Date.class))).thenReturn(List.of(sharedFS));
        try (MockedStatic<GlobalLock> globalLockMocked = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock scanlock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("sharedfsservice.cleanup")).thenReturn(scanlock);
            when(scanlock.lock(30)).thenReturn(true);
            sharedFSServiceImpl.cleanupSharedFS(true);
            verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.ExpungeOperation, null, sharedFSDao);
            verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationFailed, null, sharedFSDao);
        }
    }

    @Test
    public void testCleanupSharedFSInvalidState() throws NoTransitionException {
        SharedFSVO sharedFS = getMockSharedFS();
        ReflectionTestUtils.setField(sharedFS, "state", SharedFS.State.Stopped);
        when(sharedFSDao.listSharedFSToBeDestroyed(any(Date.class))).thenReturn(List.of(sharedFS));
        try (MockedStatic<GlobalLock> globalLockMocked = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock scanlock = mock(GlobalLock.class);
            when(GlobalLock.getInternLock("sharedfsservice.cleanup")).thenReturn(scanlock);
            when(scanlock.lock(30)).thenReturn(true);
            sharedFSServiceImpl.cleanupSharedFS(true);
            verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.ExpungeOperation, null, sharedFSDao);
            verify(_stateMachine, times(1)).transitTo(sharedFS, SharedFS.Event.OperationFailed, null, sharedFSDao);
        }
    }
}
