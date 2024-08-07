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

package org.apache.cloudstack.storage.fileshare;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.DestroyFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.dao.FileShareDao;
import org.apache.cloudstack.storage.fileshare.query.dao.FileShareJoinDao;
import org.apache.cloudstack.storage.fileshare.query.vo.FileShareJoinVO;
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
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.org.Grouping;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@RunWith(MockitoJUnitRunner.class)
public class FileShareServiceImplTest {

    @Mock
    private AccountManager accountMgr;

    @Mock
    private FileShareDao fileShareDao;

    @Mock
    private FileShareJoinDao fileShareJoinDao;

    @Mock
    private DataCenterDao dataCenterDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    private ConfigurationManager configMgr;

    @Mock
    private VolumeApiService volumeApiService;

    @Mock
    private FileShareProvider provider;

    @Mock
    private FileShareLifeCycle lifeCycle;

    @Spy
    @InjectMocks
    private FileShareServiceImpl fileShareServiceImpl;

    private static final long s_ownerId = 1L;
    private static final long s_zoneId = 2L;
    private static final long s_diskOfferingId = 3L;
    private static final long s_serviceOfferingId = 4L;
    private static final long s_domainId = 5L;
    private static final long s_volumeId = 6L;
    private static final long s_vmId = 7L;
    private static final long s_networkId = 8L;
    private static final long s_fileShareId = 9L;
    private static final long s_size = 10L;
    private static final long s_minIops = 1000L;
    private static final long s_maxIops = 2000L;
    private static final String s_providerName = "STORAGEFSVM";
    private static final String s_fsFormat = "EXT4";
    private static final String s_name = "TestFileShare";
    private static final String s_description = "Test Description";

    @Mock
    Account owner;
    @Mock
    protected StateMachine2<FileShare.State, FileShare.Event, FileShare> _stateMachine;

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

        Map<String, FileShareProvider> mockProviderMap = new HashMap<>();
        mockProviderMap.put(s_providerName, provider);
        ReflectionTestUtils.setField(fileShareServiceImpl, "fileShareProviderMap", mockProviderMap);
        when(fileShareServiceImpl.getFileShareProvider(s_providerName)).thenReturn(provider);
        when(provider.getFileShareLifeCycle()).thenReturn(lifeCycle);
        ReflectionTestUtils.setField(fileShareServiceImpl, "fileShareStateMachine", _stateMachine);
    }

    @After
    public void tearDown() throws Exception {
        callContextMocked.close();
        closeable.close();
    }

    private CreateFileShareCmd getMockCreateFileShareCmd() {
        CreateFileShareCmd cmd = mock(CreateFileShareCmd.class);
        when(cmd.getEntityOwnerId()).thenReturn(s_ownerId);
        when(cmd.getZoneId()).thenReturn(s_zoneId);
        when(cmd.getDiskOfferingId()).thenReturn(s_diskOfferingId);
        when(cmd.getSize()).thenReturn(s_size);
        when(cmd.getMinIops()).thenReturn(s_minIops);
        when(cmd.getMaxIops()).thenReturn(s_maxIops);
        when(cmd.getFileShareProviderName()).thenReturn(s_providerName);
        when(cmd.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(cmd.getNetworkId()).thenReturn(s_networkId);
        when(cmd.getFsFormat()).thenReturn(s_fsFormat);
        when(cmd.getName()).thenReturn(s_name);
        when(cmd.getDescription()).thenReturn(s_description);
        return cmd;
    }

    private FileShareVO getMockFileShare() {
        FileShareVO fileShare = new FileShareVO(s_name, s_description, s_domainId, s_ownerId, s_zoneId,
                s_providerName, FileShare.Protocol.NFS, FileShare.FileSystemType.valueOf(s_fsFormat), s_serviceOfferingId);
        return fileShare;
    }

    @Test
    public void testCreateFileShare() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        CreateFileShareCmd cmd = getMockCreateFileShareCmd();

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(0L)).thenReturn(fileShare);

        Pair<Long, Long> result = new Pair<>(s_volumeId, s_vmId);
        when(lifeCycle.commitFileShare(fileShare, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops)).thenReturn(result);
        when(fileShareDao.update(fileShare.getId(), fileShare)).thenReturn(true);

        Assert.assertEquals(fileShareServiceImpl.createFileShare(cmd), fileShare);
        Assert.assertEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(s_vmId));
        Assert.assertEquals(Optional.ofNullable(fileShare.getVolumeId()), Optional.ofNullable(s_volumeId));
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
    }

    @Test
    public void testCreateFileShareInvalidZone() {
        CreateFileShareCmd cmd = getMockCreateFileShareCmd();

        when(dataCenterDao.findById(s_zoneId)).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> fileShareServiceImpl.createFileShare(cmd));

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);
        Assert.assertThrows(PermissionDeniedException.class, () -> fileShareServiceImpl.createFileShare(cmd));
    }

    @Test
    public void tesCreateFileShareInvalidDiskOffering() {
        CreateFileShareCmd cmd = getMockCreateFileShareCmd();

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(false);
        Assert.assertThrows(InvalidParameterValueException.class, () -> fileShareServiceImpl.createFileShare(cmd));

        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(false);
        Assert.assertThrows(InvalidParameterValueException.class, () -> fileShareServiceImpl.createFileShare(cmd));
    }

    @Test
    public void testCreateFileShareInvalidFsFormat() {
        CreateFileShareCmd cmd = getMockCreateFileShareCmd();

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(s_diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        when(cmd.getFsFormat()).thenReturn("ext2");
        Assert.assertThrows(InvalidParameterValueException.class, () -> fileShareServiceImpl.createFileShare(cmd));
    }

    @Test
    public void testStartFileShare() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException, NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Detached);

        Long newVmId = 1000L;
        Pair<Boolean, Long> result = new Pair<>(true, newVmId);
        when(lifeCycle.reDeployFileShare(fileShare)).thenReturn(result);

        Assert.assertEquals(fileShareServiceImpl.startFileShare(s_fileShareId), fileShare);
        Assert.assertEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(newVmId));
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.StartRequested, null, fileShareDao);
        verify(_stateMachine, times(2)).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartFileShareInvalidState() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Ready);
        fileShareServiceImpl.startFileShare(s_fileShareId);
    }

    @Test
    public void testStartFileShareRedeployFailed() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException, NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Detached);

        Long newVmId = 1000L;
        Pair<Boolean, Long> result = new Pair<>(false, newVmId);
        when(lifeCycle.reDeployFileShare(fileShare)).thenReturn(result);

        Assert.assertNull(fileShareServiceImpl.startFileShare(s_fileShareId));
        Assert.assertNotEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(newVmId));
        verify(lifeCycle, never()).startFileShare(any());
        verify(_stateMachine, never()).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.OperationFailed, null, fileShareDao);
    }

    @Test
    public void testStopFileShare() throws NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Ready);
        Assert.assertEquals(fileShareServiceImpl.stopFileShare(s_fileShareId, false), fileShare);
        verify(lifeCycle, Mockito.times(1)).stopFileShare(any(), any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.StopRequested, null, fileShareDao);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStopFileShareInvalidState() {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);
        fileShareServiceImpl.stopFileShare(s_fileShareId, false);
    }

    @Test
    public void testRestartFileShareWithoutCleanup() throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        fileShareServiceImpl.restartFileShare(s_fileShareId, false);
        verify(lifeCycle, never()).stopFileShare(any(), any());
        verify(lifeCycle, Mockito.times(1)).startFileShare(any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.StartRequested, null, fileShareDao);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
    }

    @Test
    public void testRestartFileShareWithCleanup() throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        Long newVmId = 1000L;
        Pair<Boolean, Long> result = new Pair<>(true, newVmId);
        when(lifeCycle.reDeployFileShare(fileShare)).thenReturn(result);

        fileShareServiceImpl.restartFileShare(s_fileShareId, true);
        Assert.assertEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(newVmId));
        verify(lifeCycle, never()).stopFileShare(any(), any());
        verify(lifeCycle, times(1)).startFileShare(any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.StartRequested, null, fileShareDao);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.OperationSucceeded, null, fileShareDao);
    }

    @Test
    public void testRestartFileShareWithCleanupFailed() throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Ready);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        Long newVmId = 1000L;
        Pair<Boolean, Long> result = new Pair<>(false, newVmId);
        when(lifeCycle.reDeployFileShare(fileShare)).thenReturn(result);

        fileShareServiceImpl.restartFileShare(s_fileShareId, true);
        Assert.assertNotEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(newVmId));
        verify(lifeCycle, times(1)).stopFileShare(any(), any());
        verify(lifeCycle, never()).startFileShare(any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.StopRequested, null, fileShareDao);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.Detach, null, fileShareDao);
        verify(_stateMachine, never()).transitTo(fileShare, FileShare.Event.StartRequested, null, fileShareDao);
    }

    @Test
    public void testUpdateFileShare() {
        String newName = "New Fileshare";
        String newDescription = "New Fileshare Description";
        UpdateFileShareCmd cmd = mock(UpdateFileShareCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);
        when(cmd.getName()).thenReturn(newName);
        when(cmd.getDescription()).thenReturn(newDescription);

        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        fileShareServiceImpl.updateFileShare(cmd);
        Assert.assertEquals(fileShare.getName(), newName);
        Assert.assertEquals(fileShare.getDescription(), newDescription);
    }

    @Test
    public void testChangeFileShareDiskOffering() throws ResourceAllocationException {
        Long newSize = 200L;
        Long newMinIops = 2000L;
        Long newMaxIops = 4000L;
        Long newDiskOfferingId = 10L;
        ChangeFileShareDiskOfferingCmd cmd = mock(ChangeFileShareDiskOfferingCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);
        when(cmd.getDiskOfferingId()).thenReturn(newDiskOfferingId);
        when(cmd.getSize()).thenReturn(newSize);
        when(cmd.getMinIops()).thenReturn(newMinIops);
        when(cmd.getMaxIops()).thenReturn(newMaxIops);

        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Ready);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        DiskOfferingVO diskOfferingVO = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(newDiskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.isCustomized()).thenReturn(true);
        when(diskOfferingVO.isCustomizedIops()).thenReturn(true);

        fileShareServiceImpl.changeFileShareDiskOffering(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeFileShareDiskOfferingInvalidState() throws ResourceAllocationException {
        ChangeFileShareDiskOfferingCmd cmd = mock(ChangeFileShareDiskOfferingCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);

        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Destroyed);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        fileShareServiceImpl.changeFileShareDiskOffering(cmd);
    }

    @Test
    public void testChangeFileShareServiceOffering() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException, NoTransitionException {
        ChangeFileShareServiceOfferingCmd cmd = mock(ChangeFileShareServiceOfferingCmd.class);
        Long newServiceOfferingId = 100L;
        when(cmd.getServiceOfferingId()).thenReturn(newServiceOfferingId);
        when(cmd.getId()).thenReturn(s_fileShareId);

        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(zone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        Long newVmId = 1000L;
        Pair<Boolean, Long> result = new Pair<>(true, newVmId);
        when(lifeCycle.reDeployFileShare(fileShare)).thenReturn(result);

        fileShareServiceImpl.changeFileShareServiceOffering(cmd);
        Assert.assertEquals(Optional.ofNullable(fileShare.getVmId()), Optional.ofNullable(newVmId));
        Assert.assertEquals(Optional.ofNullable(fileShare.getServiceOfferingId()), Optional.ofNullable(newServiceOfferingId));
        verify(_stateMachine, never()).transitTo(fileShare, FileShare.Event.StartRequested, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeFileShareServiceOfferingInvalidState() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException {
        ChangeFileShareServiceOfferingCmd cmd = mock(ChangeFileShareServiceOfferingCmd.class);
        Long newServiceOfferingId = 100L;
        when(cmd.getId()).thenReturn(s_fileShareId);

        FileShareVO fileShare = getMockFileShare();
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Starting);
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);

        fileShareServiceImpl.changeFileShareServiceOffering(cmd);
    }

    @Test
    public void testDestroyFileShare() throws NoTransitionException {
        DestroyFileShareCmd cmd = mock(DestroyFileShareCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);
        when(cmd.isExpunge()).thenReturn(false);

        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);

        Assert.assertEquals(fileShareServiceImpl.destroyFileShare(cmd), true);
        verify(lifeCycle, never()).deleteFileShare(any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.DestroyRequested, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDestroyFileShareInvalidState() {
        DestroyFileShareCmd cmd = mock(DestroyFileShareCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);
        when(cmd.isExpunge()).thenReturn(false);

        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Ready);

        fileShareServiceImpl.destroyFileShare(cmd);
    }

    @Test
    public void testRecoverFileShare() throws NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Destroyed);
        Assert.assertEquals(fileShareServiceImpl.recoverFileShare(s_fileShareId), fileShare);
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.RecoveryRequested, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRecoverFileShareInvalidState() {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Expunged);
        fileShareServiceImpl.recoverFileShare(s_fileShareId);
    }

    @Test
    public void testDeleteFileShare() throws NoTransitionException {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Destroyed);
        fileShareServiceImpl.deleteFileShare(s_fileShareId);
        verify(lifeCycle, Mockito.times(1)).deleteFileShare(any());
        verify(_stateMachine, times(1)).transitTo(fileShare, FileShare.Event.ExpungeOperation, null, fileShareDao);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteFileShareInvalidState() {
        FileShareVO fileShare = getMockFileShare();
        when(fileShareDao.findById(s_fileShareId)).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "state", FileShare.State.Stopped);
        fileShareServiceImpl.deleteFileShare(s_fileShareId);
    }

    private ListFileSharesCmd getMockListFileShareCmd() {
        ListFileSharesCmd cmd = mock(ListFileSharesCmd.class);
        when(cmd.getId()).thenReturn(s_fileShareId);
        when(cmd.getName()).thenReturn(s_name);
        when(cmd.getZoneId()).thenReturn(s_zoneId);
        when(cmd.getDiskOfferingId()).thenReturn(s_diskOfferingId);
        when(cmd.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(cmd.getAccountName()).thenReturn("account");
        when(cmd.getDomainId()).thenReturn(s_domainId);
        return cmd;
    }

    @Test
    public void testSearchForFileShares() {
        SearchBuilder<FileShareVO> sb = mock(SearchBuilder.class);
        when(fileShareDao.createSearchBuilder()).thenReturn(sb);

        FileShareVO fileShare = getMockFileShare();
        when(sb.entity()).thenReturn(fileShare);
        ReflectionTestUtils.setField(fileShare, "id", s_fileShareId);

        VolumeVO volume = mock(VolumeVO.class);
        SearchBuilder<VolumeVO> volumeSb = mock(SearchBuilder.class);
        when(volumeSb.entity()).thenReturn(volume);
        when(volumeDao.createSearchBuilder()).thenReturn(volumeSb);

        SearchCriteria<FileShareVO> sc = mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);

        Pair<List<FileShareVO>, Integer> result = new Pair<>(List.of(fileShare), 1);
        when(fileShareDao.searchAndCount(any(), any())).thenReturn(result);
        FileShareJoinVO fileShareJoinVO = mock(FileShareJoinVO.class);
        when(fileShareJoinDao.searchByIds(List.of(s_fileShareId).toArray(new Long[0]))).thenReturn(List.of(fileShareJoinVO));

        when(owner.getId()).thenReturn(s_ownerId);
        when(accountMgr.isRootAdmin(any())).thenReturn(true);
        when(fileShareJoinDao.createFileShareResponses(any(), any())).thenReturn(null);

        ListFileSharesCmd cmd = getMockListFileShareCmd();
        fileShareServiceImpl.searchForFileShares(ResponseObject.ResponseView.Restricted, cmd);

        verify(sc, times(1)).setParameters("id", s_fileShareId);
        verify(sc, times(1)).setParameters("name", s_name);
        verify(sc, times(1)).setParameters("dataCenterId", s_zoneId);
        verify(sc, times(1)).setParameters("serviceOfferingId", s_serviceOfferingId);
        verify(sc, times(1)).setJoinParameters("volSearch", "diskOfferingId", s_diskOfferingId);
        verify(fileShareDao, times(1)).searchAndCount(any(), any());
    }
}
