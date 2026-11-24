/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.cloud.agent.AgentManager;
import com.cloud.configuration.Resource;
import com.cloud.user.ResourceLimitService;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class KvmFileBasedNoCopySnapshotStrategyTest {

    private static final long SNAPSHOT_ID = 10L;
    private static final long VOLUME_ID = 20L;
    private static final long POOL_ID = 30L;
    private static final long VM_ID = 40L;
    private static final long HOST_ID = 50L;
    private static final long ZONE_ID = 60L;
    private static final String VOLUME_UUID = "volume-uuid";
    private static final String VOLUME_PATH = "current-delta";

    private KvmFileBasedNoCopySnapshotStrategy strategy;
    private SnapshotDao snapshotDao;
    private SnapshotDataStoreDao snapshotStoreDao;
    private SnapshotDetailsDao snapshotDetailsDao;
    private SnapshotDataFactory snapshotDataFactory;
    private VolumeDao volumeDao;
    private VolumeDataFactory volumeDataFactory;
    private VMInstanceDao vmInstanceDao;
    private VMSnapshotDao vmSnapshotDao;
    private PrimaryDataStoreDao storagePoolDao;
    private SnapshotManager snapshotManager;
    private HostDetailsDao hostDetailsDao;
    private EndPointSelector endPointSelector;
    private ResourceLimitService resourceLimitService;

    private SnapshotInfo snapshotInfo;
    private SnapshotVO snapshotVO;
    private VolumeVO volumeVO;
    private VolumeInfo volumeInfo;
    private VolumeObjectTO volumeTO;
    private StoragePoolVO storagePool;
    private CreateSnapshotPayload payload;
    private VMInstanceVO vm;

    @Before
    public void setup() {
        strategy = Mockito.spy(new KvmFileBasedNoCopySnapshotStrategy());
        snapshotDao = mock(SnapshotDao.class);
        snapshotStoreDao = mock(SnapshotDataStoreDao.class);
        snapshotDetailsDao = mock(SnapshotDetailsDao.class);
        snapshotDataFactory = mock(SnapshotDataFactory.class);
        volumeDao = mock(VolumeDao.class);
        volumeDataFactory = mock(VolumeDataFactory.class);
        vmInstanceDao = mock(VMInstanceDao.class);
        vmSnapshotDao = mock(VMSnapshotDao.class);
        storagePoolDao = mock(PrimaryDataStoreDao.class);
        snapshotManager = mock(SnapshotManager.class);

        strategy.snapshotDao = snapshotDao;
        strategy.snapshotStoreDao = snapshotStoreDao;
        strategy.snapshotDetailsDao = snapshotDetailsDao;
        strategy.snapshotZoneDao = mock(SnapshotZoneDao.class);
        strategy.snapshotDataFactory = snapshotDataFactory;
        strategy.volumeDao = volumeDao;
        strategy.volumeDataFactory = volumeDataFactory;
        strategy.vmInstanceDao = vmInstanceDao;
        strategy.vmSnapshotDao = vmSnapshotDao;
        strategy.storagePoolDao = storagePoolDao;
        strategy.snapshotManager = snapshotManager;
        resourceLimitService = mock(ResourceLimitService.class);
        strategy.resourceLimitService = resourceLimitService;

        snapshotVO = mockSnapshotVO(SNAPSHOT_ID, Snapshot.State.BackedUp);
        when(snapshotVO.getTypeDescription()).thenReturn(Snapshot.Type.MANUAL.name());
        when(snapshotDao.acquireInLockTable(SNAPSHOT_ID)).thenReturn(snapshotVO);

        payload = mock(CreateSnapshotPayload.class);
        snapshotInfo = mock(SnapshotInfo.class);
        when(snapshotInfo.getId()).thenReturn(SNAPSHOT_ID);
        when(snapshotInfo.getUuid()).thenReturn("snapshot-uuid");
        when(snapshotInfo.getVolumeId()).thenReturn(VOLUME_ID);
        when(snapshotInfo.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(snapshotInfo.getPayload()).thenReturn(payload);

        volumeVO = mock(VolumeVO.class);
        when(volumeVO.getId()).thenReturn(VOLUME_ID);
        when(volumeVO.getUuid()).thenReturn(VOLUME_UUID);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeVO.getDataCenterId()).thenReturn(ZONE_ID);
        when(volumeVO.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(volumeVO.getPassphraseId()).thenReturn(null);
        when(volumeVO.getKmsKeyId()).thenReturn(null);
        when(volumeVO.getInstanceId()).thenReturn(null);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);
        when(volumeDao.findByIdIncludingRemoved(VOLUME_ID)).thenReturn(volumeVO);

        storagePool = mock(StoragePoolVO.class);
        when(storagePool.getId()).thenReturn(POOL_ID);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(storagePool);
        doReturn(true).when(strategy).isNoCopySnapshotEnabledForStoragePool(POOL_ID);

        volumeTO = new VolumeObjectTO();
        volumeTO.setPath(VOLUME_PATH);
        volumeTO.setUuid(VOLUME_UUID);
        volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(VOLUME_ID);
        when(volumeInfo.getUuid()).thenReturn(VOLUME_UUID);
        when(volumeInfo.getPoolId()).thenReturn(POOL_ID);
        when(volumeInfo.getState()).thenReturn(Volume.State.Ready);
        when(volumeInfo.getTO()).thenReturn(volumeTO);
        when(volumeInfo.getInstanceId()).thenReturn(null);
        when(volumeInfo.stateTransit(any())).thenReturn(true);
        when(snapshotInfo.getBaseVolume()).thenReturn(volumeInfo);
        when(volumeDataFactory.getVolume(VOLUME_ID)).thenReturn(volumeInfo);

        vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getUuid()).thenReturn("vm-uuid");
        when(vm.getInstanceName()).thenReturn("i-2-40-VM");
        when(vm.getHostId()).thenReturn(HOST_ID);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getBackupOfferingId()).thenReturn(null);
        when(vmInstanceDao.findById(VM_ID)).thenReturn(vm);

        doReturn(HOST_ID).when(strategy).getHostIdForOperation(any(), any());
        hostDetailsDao = mock(HostDetailsDao.class);
        strategy.hostDetailsDao = hostDetailsDao;
        endPointSelector = mock(EndPointSelector.class);
        strategy.endPointSelector = endPointSelector;
        when(hostDetailsDao.findDetail(HOST_ID, Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME)).thenReturn(new DetailVO(HOST_ID,
                Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME, "true"));
    }

    private SnapshotVO mockSnapshotVO(long id, Snapshot.State state) {
        SnapshotVO snapshot = mock(SnapshotVO.class);
        when(snapshot.getId()).thenReturn(id);
        when(snapshot.getUuid()).thenReturn("snapshot-" + id);
        when(snapshot.getVolumeId()).thenReturn(VOLUME_ID);
        when(snapshot.getDataCenterId()).thenReturn(ZONE_ID);
        when(snapshot.getState()).thenReturn(state);
        when(snapshotDao.findById(id)).thenReturn(snapshot);
        return snapshot;
    }

    private void attachVolumeToVm() {
        when(volumeVO.getInstanceId()).thenReturn(VM_ID);
        when(volumeInfo.getInstanceId()).thenReturn(VM_ID);
    }

    /**
     * Mocks a chain of no-copy snapshots of the volume (oldest first) and returns them.
     */
    private List<SnapshotVO> mockChain(long... ids) {
        List<SnapshotVO> chain = new ArrayList<>();
        long sequence = 0;
        for (long id : ids) {
            sequence++;
            SnapshotVO snapshot = id == SNAPSHOT_ID ? snapshotVO : mockSnapshotVO(id, Snapshot.State.BackedUp);
            when(snapshotManager.isKvmFileBasedNoCopySnapshot(id)).thenReturn(true);

            SnapshotObjectTO snapshotTO = new SnapshotObjectTO();
            snapshotTO.setPath("frozen-" + id);
            SnapshotInfo info = mock(SnapshotInfo.class);
            when(info.getTO()).thenReturn(snapshotTO);
            when(snapshotDataFactory.getSnapshot(id, POOL_ID, DataStoreRole.Primary)).thenReturn(info);

            SnapshotDataStoreVO storeRef = mock(SnapshotDataStoreVO.class);
            when(storeRef.getInstallPath()).thenReturn("frozen-" + id);
            when(storeRef.getPhysicalSize()).thenReturn(100L);
            when(storeRef.getDataStoreId()).thenReturn(POOL_ID);
            when(snapshotStoreDao.findOneBySnapshotAndDatastoreRole(id, DataStoreRole.Primary)).thenReturn(storeRef);
            when(snapshotDetailsDao.findDetail(id, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE))
                    .thenReturn(new SnapshotDetailsVO(id, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE, String.valueOf(sequence), false));
            chain.add(snapshot);
        }
        when(snapshotDao.listByVolumeId(VOLUME_ID)).thenReturn(chain);
        return chain;
    }

    // canHandle

    @Test
    public void canHandleTakeEligibleDetachedVolume() {
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeEligibleRunningVm() {
        attachVolumeToVm();
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeWhenDisabledForStoragePool() {
        doReturn(false).when(strategy).isNoCopySnapshotEnabledForStoragePool(POOL_ID);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeNotKvm() {
        when(snapshotInfo.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeGroupSnapshot() {
        when(snapshotVO.getTypeDescription()).thenReturn(Snapshot.Type.GROUP.name());
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeUnsupportedPoolType() {
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeManagedPool() {
        when(storagePool.isManaged()).thenReturn(true);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeRawVolume() {
        when(volumeVO.getFormat()).thenReturn(Storage.ImageFormat.RAW);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeEncryptedVolume() {
        when(volumeVO.getPassphraseId()).thenReturn(1L);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeWhenBackedUpToSecondary() {
        when(snapshotManager.isBackupSnapshotToSecondaryForZone(ZONE_ID)).thenReturn(true);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeWithoutPayload() {
        when(snapshotInfo.getPayload()).thenReturn(null);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeIncrementalSnapshot() {
        when(payload.isKvmIncrementalSnapshot()).thenReturn(true);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeCopyToOtherZones() {
        when(payload.getZoneIds()).thenReturn(List.of(2L));
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeQuiesce() {
        when(payload.getQuiescevm()).thenReturn(true);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeVmWithVmSnapshots() {
        attachVolumeToVm();
        when(vmSnapshotDao.findByVm(VM_ID)).thenReturn(List.of(mock(VMSnapshotVO.class)));
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeVmWithBackupOffering() {
        attachVolumeToVm();
        when(vm.getBackupOfferingId()).thenReturn(1L);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeVmMigrating() {
        attachVolumeToVm();
        when(vm.getState()).thenReturn(VirtualMachine.State.Migrating);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeRunningVmOnHostWithoutSingleVolumeCapability() {
        attachVolumeToVm();
        when(hostDetailsDao.findDetail(HOST_ID, Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME)).thenReturn(null);
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleTakeStoppedVmDoesNotNeedSingleVolumeCapability() {
        attachVolumeToVm();
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(hostDetailsDao.findDetail(HOST_ID, Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME)).thenReturn(null);
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotInfo, null, SnapshotOperation.TAKE));
    }

    @Test
    public void canHandleOtherOperationsOfNoCopySnapshot() {
        when(snapshotManager.isKvmFileBasedNoCopySnapshot(SNAPSHOT_ID)).thenReturn(true);
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotVO, null, SnapshotOperation.DELETE));
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotVO, ZONE_ID, SnapshotOperation.DELETE));
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotVO, null, SnapshotOperation.REVERT));
        assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(snapshotVO, null, SnapshotOperation.BACKUP));
        assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotVO, null, SnapshotOperation.COPY));
    }

    @Test
    public void canHandleOtherOperationsOfRegularSnapshot() {
        when(snapshotManager.isKvmFileBasedNoCopySnapshot(SNAPSHOT_ID)).thenReturn(false);
        for (SnapshotOperation op : List.of(SnapshotOperation.DELETE, SnapshotOperation.REVERT, SnapshotOperation.BACKUP, SnapshotOperation.COPY)) {
            assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(snapshotVO, null, op));
        }
    }

    // take

    private SnapshotObject mockSnapshotOnPrimary() {
        SnapshotObject snapshotOnPrimary = mock(SnapshotObject.class);
        doReturn(snapshotOnPrimary).when(strategy).createSnapshotOnPrimary(snapshotInfo);
        return snapshotOnPrimary;
    }

    @Test
    public void takeSnapshotOfRunningVmMovesVolumeOntoNewDelta() throws Exception {
        attachVolumeToVm();
        SnapshotObject snapshotOnPrimary = mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getMapVolumeToSnapshotSize()).thenReturn(Map.of(VOLUME_UUID, 1234L));
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());

        assertSame(snapshotOnPrimary, strategy.takeSnapshot(snapshotInfo));

        CreateDiskOnlyVmSnapshotCommand command = (CreateDiskOnlyVmSnapshotCommand) commandCaptor.getValue();
        assertEquals(VirtualMachine.State.Running, command.getVmState());
        assertEquals("i-2-40-VM", command.getVmName());
        assertEquals(1, command.getVolumeTosAndNewPaths().size());
        String newPath = command.getVolumeTosAndNewPaths().get(0).second();
        assertNotEquals(VOLUME_PATH, newPath);
        assertTrue(!command.isUefiEnabled() && !command.getTarget().getQuiescevm());

        verify(volumeVO).setPath(newPath);
        verify(volumeDao).update(VOLUME_ID, volumeVO);
        ArgumentCaptor<CreateObjectAnswer> answerCaptor = ArgumentCaptor.forClass(CreateObjectAnswer.class);
        verify(snapshotOnPrimary).processEvent(eq(ObjectInDataStoreStateMachine.Event.OperationSucceeded), answerCaptor.capture());
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO) answerCaptor.getValue().getData();
        assertEquals(VOLUME_PATH, snapshotTO.getPath());
        assertEquals(Long.valueOf(1234L), snapshotTO.getPhysicalSize());
        verify(snapshotOnPrimary).processEvent(Snapshot.Event.OperationSucceeded);
        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT, "true", false);
        verify(volumeInfo).stateTransit(Volume.Event.SnapshotRequested);
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
        verify(snapshotDao).releaseFromLockTable(SNAPSHOT_ID);
    }

    @Test
    public void takeSnapshotOfDetachedVolumeUsesStoppedPath() {
        mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getMapVolumeToSnapshotSize()).thenReturn(Map.of(VOLUME_UUID, 1L));
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());

        strategy.takeSnapshot(snapshotInfo);

        assertEquals(VirtualMachine.State.Stopped, ((CreateDiskOnlyVmSnapshotCommand) commandCaptor.getValue()).getVmState());
    }

    @Test
    public void takeSnapshotFailureKeepsVolumePath() throws Exception {
        SnapshotObject snapshotOnPrimary = mockSnapshotOnPrimary();
        Answer answer = new Answer(null, false, "qemu-img failed");
        doReturn(answer).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeVO, never()).setPath(any());
        verify(snapshotDetailsDao, never()).addDetail(eq(SNAPSHOT_ID), eq(KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE), any(),
                Mockito.anyBoolean());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
        verify(snapshotOnPrimary).processEvent(Snapshot.Event.OperationFailed);
        verify(snapshotOnPrimary).processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
        verify(snapshotDao).releaseFromLockTable(SNAPSHOT_ID);
        // A definite failure is a failed snapshot like any other: its regular cleanup is safe, as its volume keeps its file.
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT);
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);
    }

    // revert

    @Test
    public void revertLatestSnapshotPutsVolumeOnNewDelta() {
        mockChain(5L, SNAPSHOT_ID);
        RevertDiskOnlyVmSnapshotAnswer answer = mock(RevertDiskOnlyVmSnapshotAnswer.class);
        VolumeObjectTO reverted = new VolumeObjectTO();
        reverted.setPath("new-delta");
        when(answer.getResult()).thenReturn(true);
        when(answer.getVolumeObjectTos()).thenReturn(List.of(reverted));
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());

        assertTrue(strategy.revertSnapshot(snapshotInfo));

        RevertDiskOnlyVmSnapshotCommand command = (RevertDiskOnlyVmSnapshotCommand) commandCaptor.getValue();
        assertEquals(1, command.getSnapshotObjectTos().size());
        assertEquals("frozen-" + SNAPSHOT_ID, command.getSnapshotObjectTos().get(0).getPath());
        assertEquals(VOLUME_PATH, command.getSnapshotObjectTos().get(0).getVolume().getPath());
        verify(volumeVO).setPath("new-delta");
        verify(volumeInfo).stateTransit(Volume.Event.RevertSnapshotRequested);
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void revertOlderSnapshotIsRefused() {
        mockChain(SNAPSHOT_ID, 15L);
        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(snapshotDao).releaseFromLockTable(SNAPSHOT_ID);
    }

    @Test
    public void revertWithRunningVmIsRefused() {
        mockChain(SNAPSHOT_ID);
        attachVolumeToVm();
        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void revertFailureKeepsVolumePath() {
        mockChain(SNAPSHOT_ID);
        doReturn(new Answer(null, false, "failed")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(volumeVO, never()).setPath(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    // delete

    private MergeDiskOnlyVmSnapshotCommand deleteAndCaptureMerge() {
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(new Answer(null, true, null)).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());
        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));
        return (MergeDiskOnlyVmSnapshotCommand) commandCaptor.getValue();
    }

    @Test
    public void deleteMostRecentSnapshotMergesVolumeDeltaIntoIt() {
        attachVolumeToVm();
        mockChain(5L, SNAPSHOT_ID);

        MergeDiskOnlyVmSnapshotCommand command = deleteAndCaptureMerge();

        assertTrue(command.isVmRunning());
        DeltaMergeTreeTO tree = command.getDeltaMergeTreeToList().get(0);
        assertEquals("frozen-" + SNAPSHOT_ID, tree.getParent().getPath());
        assertSame(volumeTO, tree.getChild());
        assertTrue(tree.getGrandChildren().isEmpty());
        verify(volumeVO).setPath("frozen-" + SNAPSHOT_ID);
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
        verify(volumeInfo).stateTransit(Volume.Event.SnapshotRequested);
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void deleteMiddleSnapshotMergesNextSnapshotIntoIt() {
        List<SnapshotVO> chain = mockChain(5L, SNAPSHOT_ID, 15L);

        MergeDiskOnlyVmSnapshotCommand command = deleteAndCaptureMerge();

        assertTrue(!command.isVmRunning());
        DeltaMergeTreeTO tree = command.getDeltaMergeTreeToList().get(0);
        assertEquals("frozen-" + SNAPSHOT_ID, tree.getParent().getPath());
        assertEquals("frozen-15", tree.getChild().getPath());
        assertEquals(1, tree.getGrandChildren().size());
        assertSame(volumeTO, tree.getGrandChildren().get(0));

        SnapshotDataStoreVO childRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(chain.get(2).getId(), DataStoreRole.Primary);
        verify(childRef).setInstallPath("frozen-" + SNAPSHOT_ID);
        verify(childRef).setPhysicalSize(200L);
        verify(volumeVO, never()).setPath(any());
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void deleteOldestOfThreeSnapshotsRebasesThirdSnapshot() {
        mockChain(SNAPSHOT_ID, 15L, 25L);

        DeltaMergeTreeTO tree = deleteAndCaptureMerge().getDeltaMergeTreeToList().get(0);

        assertEquals("frozen-15", tree.getChild().getPath());
        assertEquals("frozen-25", tree.getGrandChildren().get(0).getPath());
    }

    @Test
    public void deleteFailureKeepsMetadata() {
        mockChain(SNAPSHOT_ID);
        doReturn(new Answer(null, false, "blockcommit failed")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(volumeVO, never()).setPath(any());
        verify(snapshotVO, never()).setState(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    private Command deleteSnapshotOfGoneVolumeAndCaptureCommand() {
        EndPoint endPoint = mock(EndPoint.class);
        when(endPoint.getId()).thenReturn(HOST_ID);
        when(endPointSelector.select(any(SnapshotInfo.class))).thenReturn(endPoint);
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(new Answer(null, true, null)).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));
        return commandCaptor.getValue();
    }

    @Test
    public void deleteSnapshotOfExpungedVolumeDeletesItsFrozenFile() {
        mockChain(SNAPSHOT_ID);
        when(volumeVO.getState()).thenReturn(Volume.State.Expunged);
        when(volumeVO.getRemoved()).thenReturn(new java.util.Date());

        DeleteDiskOnlyVmSnapshotCommand command = (DeleteDiskOnlyVmSnapshotCommand) deleteSnapshotOfGoneVolumeAndCaptureCommand();

        assertEquals(1, command.getSnapshots().size());
        assertEquals("frozen-" + SNAPSHOT_ID, command.getSnapshots().get(0).getPath());
        verify(volumeInfo, never()).stateTransit(any());
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void deleteSnapshotOfRemovedButNotExpungedVolumeOnlyRemovesMetadata() {
        mockChain(SNAPSHOT_ID);
        when(volumeVO.getState()).thenReturn(Volume.State.Destroy);  // e.g. unmanaged: the files stay with the unmanaged disk
        when(volumeVO.getRemoved()).thenReturn(new java.util.Date());

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void deleteSnapshotOfExpungedVolumeKeepsMetadataWhenTheFileCannotBeDeleted() {
        mockChain(SNAPSHOT_ID);
        when(volumeVO.getState()).thenReturn(Volume.State.Expunged);
        EndPoint endPoint = mock(EndPoint.class);
        when(endPoint.getId()).thenReturn(HOST_ID);
        when(endPointSelector.select(any(SnapshotInfo.class))).thenReturn(endPoint);
        doReturn(new Answer(null, false, "permission denied")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));
        verify(snapshotVO, never()).setState(any());
    }

    @Test
    public void deleteSnapshotInErrorOnlyRemovesMetadata() {
        when(snapshotVO.getState()).thenReturn(Snapshot.State.Error);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void deleteSnapshotFromOtherZoneIsRefused() {
        assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, ZONE_ID + 1));
    }

    @Test
    public void deleteSnapshotWhileVolumeIsInAnotherStateIsRefused() {
        mockChain(SNAPSHOT_ID);
        when(volumeInfo.getState()).thenReturn(Volume.State.Migrating);
        when(volumeInfo.stateTransit(Volume.Event.SnapshotRequested)).thenThrow(new CloudRuntimeException("no transition"));
        assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
    }

    // volume reservation races (stateTransit returns false when its optimistic update loses)

    @Test
    public void takeSnapshotIsAbortedWhenTheVolumeReservationLosesARace() {
        when(volumeInfo.stateTransit(Volume.Event.SnapshotRequested)).thenReturn(false);

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(strategy, never()).createSnapshotOnPrimary(any());
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(snapshotDao).releaseFromLockTable(SNAPSHOT_ID);
    }

    @Test
    public void revertSnapshotIsAbortedWhenTheVolumeReservationLosesARace() {
        mockChain(SNAPSHOT_ID);
        when(volumeInfo.stateTransit(Volume.Event.RevertSnapshotRequested)).thenReturn(false);

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void deleteSnapshotIsAbortedWhenTheVolumeReservationLosesARace() {
        mockChain(SNAPSHOT_ID);
        when(volumeInfo.stateTransit(Volume.Event.SnapshotRequested)).thenReturn(false);

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void revertReadsTheChainOnlyAfterReservingTheVolume() {
        mockChain(5L, SNAPSHOT_ID);
        doReturn(new Answer(null, false, "failed")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        org.mockito.InOrder inOrder = Mockito.inOrder(volumeInfo, snapshotDao);
        inOrder.verify(volumeInfo).stateTransit(Volume.Event.RevertSnapshotRequested);
        inOrder.verify(snapshotDao).listByVolumeId(VOLUME_ID);
    }

    @Test
    public void deleteReadsTheChainOnlyAfterReservingTheVolume() {
        mockChain(SNAPSHOT_ID);
        doReturn(new Answer(null, false, "failed")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        org.mockito.InOrder inOrder = Mockito.inOrder(volumeInfo, snapshotDao);
        inOrder.verify(volumeInfo).stateTransit(Volume.Event.SnapshotRequested);
        inOrder.verify(snapshotDao).listByVolumeId(VOLUME_ID);
    }

    // chain order follows the takes, not the snapshot ids

    @Test
    public void chainIsOrderedByTakeSequenceNotById() {
        mockChain(SNAPSHOT_ID + 5, SNAPSHOT_ID);  // the snapshot with the higher id was taken first

        List<SnapshotVO> chain = strategy.listNoCopySnapshotsOfVolume(VOLUME_ID);

        assertEquals(SNAPSHOT_ID + 5, chain.get(0).getId());
        assertEquals(SNAPSHOT_ID, chain.get(1).getId());
    }

    @Test
    public void revertOfTheLatestTakeIsAllowedEvenWithALowerId() {
        mockChain(SNAPSHOT_ID + 5, SNAPSHOT_ID);
        RevertDiskOnlyVmSnapshotAnswer answer = mock(RevertDiskOnlyVmSnapshotAnswer.class);
        VolumeObjectTO reverted = new VolumeObjectTO();
        reverted.setPath("new-delta");
        when(answer.getResult()).thenReturn(true);
        when(answer.getVolumeObjectTos()).thenReturn(List.of(reverted));
        doReturn(answer).when(strategy).sendToHost(anyLong(), any());

        assertTrue(strategy.revertSnapshot(snapshotInfo));
    }

    @Test
    public void revertOfAnOlderTakeIsRefusedEvenWithAHigherId() {
        mockChain(SNAPSHOT_ID, SNAPSHOT_ID - 5);  // the snapshot with the lower id was taken last
        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void deleteMergeTreeFollowsTheTakeSequence() {
        mockChain(SNAPSHOT_ID, SNAPSHOT_ID - 5);  // taken: SNAPSHOT_ID first, then SNAPSHOT_ID - 5

        DeltaMergeTreeTO tree = deleteAndCaptureMerge().getDeltaMergeTreeToList().get(0);

        assertEquals("frozen-" + SNAPSHOT_ID, tree.getParent().getPath());
        assertEquals("frozen-" + (SNAPSHOT_ID - 5), tree.getChild().getPath());
    }

    @Test
    public void takeSnapshotRecordsTheNextChainSequence() {
        mockChain(3L, 4L);
        mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getMapVolumeToSnapshotSize()).thenReturn(Map.of(VOLUME_UUID, 1L));
        doReturn(answer).when(strategy).sendToHost(anyLong(), any());

        strategy.takeSnapshot(snapshotInfo);

        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE, "3", false);
    }

    @Test
    public void deleteSnapshotWhileVmIsStartingIsRefused() {
        mockChain(SNAPSHOT_ID);
        attachVolumeToVm();
        when(vm.getState()).thenReturn(VirtualMachine.State.Starting);
        assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    // unknown outcomes and partial merges

    @Test
    public void takeSnapshotAbortsWhenABackupOfferingWasAssignedMeanwhile() {
        attachVolumeToVm();
        when(vm.getBackupOfferingId()).thenReturn(1L);

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT, "true", false);
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT);
        verify(strategy, never()).createSnapshotOnPrimary(any());
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void takeSnapshotOfRunningVmWithoutAnswerKeepsTheVolumeReserved() {
        attachVolumeToVm();
        mockSnapshotOnPrimary();
        doReturn(null).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeVO, never()).setPath(any());
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationSucceeded);
        verify(snapshotDetailsDao).addDetail(eq(SNAPSHOT_ID), eq(KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH), any(),
                eq(false));
        // Not marked as failed: its outcome is unknown, and a failed snapshot could be cleaned up although its file may be in use.
        verify(strategy, never()).markSnapshotOnPrimaryAsFailed(any());
        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_RESOURCE_COUNT_RELEASED, "true", false);
        verify(snapshotDetailsDao, never()).removeDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT);
    }

    // commands that were not sent (host down or disconnected)

    @Test
    public void sendToHostDistinguishesNotSentFromNoAnswer() throws Exception {
        AgentManager agentManager = mock(AgentManager.class);
        strategy.agentManager = agentManager;
        Command command = mock(Command.class);
        Mockito.doCallRealMethod().when(strategy).sendToHost(anyLong(), any());

        when(agentManager.send(HOST_ID, command)).thenThrow(new AgentUnavailableException("host is down", HOST_ID));
        assertThrows(KvmFileBasedNoCopySnapshotStrategy.CommandNotSentException.class, () -> strategy.sendToHost(HOST_ID, command));

        Mockito.reset(agentManager);
        when(agentManager.send(HOST_ID, command)).thenThrow(new OperationTimedoutException(new Command[0], HOST_ID, 1L, 10, false));
        assertNull(strategy.sendToHost(HOST_ID, command));

        Mockito.reset(agentManager);
        when(agentManager.send(HOST_ID, command)).thenThrow(new CloudRuntimeException("failed processing the answer"));
        assertNull(strategy.sendToHost(HOST_ID, command));

        Mockito.reset(agentManager);
        Answer answer = new Answer(null, true, null);
        when(agentManager.send(HOST_ID, command)).thenReturn(answer);
        assertSame(answer, strategy.sendToHost(HOST_ID, command));
    }

    @Test
    public void takeSnapshotOfRunningVmWhoseHostIsDownReleasesTheVolume() throws Exception {
        attachVolumeToVm();
        SnapshotObject snapshotOnPrimary = mockSnapshotOnPrimary();
        doThrow(new KvmFileBasedNoCopySnapshotStrategy.CommandNotSentException("host is down", null)).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
        verify(volumeVO, never()).setPath(any());
        verify(snapshotOnPrimary).processEvent(Snapshot.Event.OperationFailed);
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);
    }

    @Test
    public void deleteSnapshotWhoseHostIsDownReleasesTheVolumeAndKeepsItRevertible() {
        attachVolumeToVm();
        mockChain(SNAPSHOT_ID);
        doThrow(new KvmFileBasedNoCopySnapshotStrategy.CommandNotSentException("host is down", null)).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
        // Recorded before sending, and removed again as nothing was sent.
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE);
    }

    @Test
    public void mergeIsMarkedIncompleteBeforeTheHostIsAsked() {
        mockChain(SNAPSHOT_ID);
        deleteAndCaptureMerge();

        InOrder inOrder = Mockito.inOrder(snapshotDetailsDao, strategy);
        inOrder.verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE, "true", false);
        inOrder.verify(strategy).sendToHost(eq(HOST_ID), any());
    }

    @Test
    public void deleteWhoseChildIsAlreadyTheMergedFileOnlyUpdatesTheMetadata() {
        mockChain(SNAPSHOT_ID);
        volumeTO.setPath("frozen-" + SNAPSHOT_ID);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeVO).setPath("frozen-" + SNAPSHOT_ID);
        verify(snapshotVO).setState(Snapshot.State.Destroyed);
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void revertIsRefusedWhenTheVolumeIsOnTheFileOfASnapshot() {
        mockChain(SNAPSHOT_ID);
        volumeTO.setPath("frozen-" + SNAPSHOT_ID);

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void revertSnapshotWhoseHostIsDownReleasesTheVolume() {
        mockChain(SNAPSHOT_ID);
        doThrow(new KvmFileBasedNoCopySnapshotStrategy.CommandNotSentException("host is down", null)).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
        verify(volumeVO, never()).setPath(any());
    }

    @Test
    public void releaseVolumeNeverThrows() {
        when(volumeInfo.stateTransit(Volume.Event.OperationFailed)).thenThrow(new CloudRuntimeException("no transition"));
        strategy.releaseVolume(volumeInfo, Volume.Event.OperationFailed);
        when(volumeInfo.stateTransit(Volume.Event.OperationSucceeded)).thenReturn(false);
        strategy.releaseVolume(volumeInfo, Volume.Event.OperationSucceeded);
    }

    // takes that did not complete (no answer for a running Instance, or a management server restart)

    private SnapshotVO mockIncompleteTake(String pendingVolumePath, String pendingFrozenPath, Volume.State volumeState, String volumePath) {
        SnapshotVO creating = mock(SnapshotVO.class);
        when(creating.getId()).thenReturn(SNAPSHOT_ID);
        when(creating.getUuid()).thenReturn("snapshot-" + SNAPSHOT_ID);
        when(creating.getVolumeId()).thenReturn(VOLUME_ID);
        when(creating.getDataCenterId()).thenReturn(ZONE_ID);
        when(creating.getState()).thenReturn(Snapshot.State.Creating);
        when(snapshotDao.acquireInLockTable(SNAPSHOT_ID)).thenReturn(creating);
        if (pendingVolumePath != null) {
            when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH))
                    .thenReturn(new SnapshotDetailsVO(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH,
                            pendingVolumePath, false));
        }
        if (pendingFrozenPath != null) {
            when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_FROZEN_PATH))
                    .thenReturn(new SnapshotDetailsVO(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_FROZEN_PATH,
                            pendingFrozenPath, false));
        }
        when(volumeVO.getState()).thenReturn(volumeState);
        when(volumeVO.getPath()).thenReturn(volumePath);
        return creating;
    }

    @Test
    public void deleteOfAnIncompleteTakeThatHappenedCompletesItThenMergesIt() {
        List<SnapshotVO> chainBefore = mockChain(5L);
        SnapshotVO creating = mockIncompleteTake("new-delta", "frozen-10", Volume.State.Ready, "new-delta");
        SnapshotDataStoreVO storeRef = mock(SnapshotDataStoreVO.class);
        when(storeRef.getInstallPath()).thenReturn("frozen-10");
        when(storeRef.getDataStoreId()).thenReturn(POOL_ID);
        when(snapshotStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(storeRef);
        SnapshotObjectTO snapshotTO = new SnapshotObjectTO();
        snapshotTO.setPath("frozen-10");
        SnapshotInfo info = mock(SnapshotInfo.class);
        when(info.getTO()).thenReturn(snapshotTO);
        when(snapshotDataFactory.getSnapshot(SNAPSHOT_ID, POOL_ID, DataStoreRole.Primary)).thenReturn(info);
        when(snapshotManager.isKvmFileBasedNoCopySnapshot(SNAPSHOT_ID)).thenReturn(true);
        // Once completed, the snapshot is BackedUp and part of the chain, after snapshot 5.
        List<SnapshotVO> chainAfter = new ArrayList<>(chainBefore);
        chainAfter.add(snapshotVO);
        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE))
                .thenReturn(new SnapshotDetailsVO(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE, "2", false));
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating, creating, snapshotVO);
        when(snapshotDao.listByVolumeId(VOLUME_ID)).thenReturn(chainBefore, chainAfter);

        MergeDiskOnlyVmSnapshotCommand command = deleteAndCaptureMerge();

        verify(storeRef).setInstallPath("frozen-10");
        verify(storeRef).setState(ObjectInDataStoreStateMachine.State.Ready);
        verify(creating).setState(Snapshot.State.BackedUp);
        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE, "2", false);
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);
        assertEquals("frozen-10", command.getDeltaMergeTreeToList().get(0).getParent().getPath());
    }

    @Test
    public void deleteOfAnIncompleteTakeThatDidNotHappenDiscardsIt() {
        SnapshotVO creating = mockIncompleteTake("new-delta", VOLUME_PATH, Volume.State.Ready, VOLUME_PATH);
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(creating).setState(Snapshot.State.Destroyed);
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeVO, never()).setPath(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void deleteOfAnIncompleteTakeIsRefusedWhileItsVolumeIsStillReserved() {
        SnapshotVO creating = mockIncompleteTake("new-delta", VOLUME_PATH, Volume.State.Snapshotting, VOLUME_PATH);
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);

        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        assertTrue(e.getMessage().contains("new-delta"));
        verify(creating, never()).setState(any());
        verify(volumeInfo, never()).stateTransit(any());
    }

    @Test
    public void deleteOfAnIncompleteTakeThatNeverReachedTheHostDiscardsIt() {
        SnapshotVO creating = mockIncompleteTake(null, null, Volume.State.Snapshotting, VOLUME_PATH);
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(creating).setState(Snapshot.State.Destroyed);
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void deleteOfAnIncompleteTakeFrozenByALaterTakeAsksToDeleteTheLaterOnesFirst() {
        mockChain(5L);
        SnapshotDataStoreVO laterStoreRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(5L, DataStoreRole.Primary);
        when(laterStoreRef.getInstallPath()).thenReturn("new-delta");
        SnapshotVO creating = mockIncompleteTake("new-delta", "frozen-10", Volume.State.Ready, "newer-delta");
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);

        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        assertTrue(e.getMessage().contains("taken after it first"));
        verify(creating, never()).setState(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void deleteOfASnapshotLeftByAFailedTakeCountsItAgainFirst() {
        SnapshotVO creating = mockIncompleteTake("new-delta", VOLUME_PATH, Volume.State.Ready, VOLUME_PATH);
        when(creating.getAccountId()).thenReturn(7L);
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);
        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_RESOURCE_COUNT_RELEASED))
                .thenReturn(new SnapshotDetailsVO(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_RESOURCE_COUNT_RELEASED, "true", false));

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(resourceLimitService).incrementResourceCount(7L, Resource.ResourceType.snapshot);
        verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_RESOURCE_COUNT_RELEASED);
    }

    @Test
    public void deleteOfARegularlyTakenSnapshotDoesNotCountItAgain() {
        mockChain(SNAPSHOT_ID);
        deleteAndCaptureMerge();
        verify(resourceLimitService, never()).incrementResourceCount(Mockito.anyLong(), any());
    }

    private void mockIncompleteTakeOfAnotherSnapshot(List<SnapshotVO> chain) {
        SnapshotVO incomplete = mock(SnapshotVO.class);
        when(incomplete.getId()).thenReturn(99L);
        when(incomplete.getUuid()).thenReturn("incomplete-snapshot");
        when(incomplete.getState()).thenReturn(Snapshot.State.Creating);
        when(snapshotDetailsDao.findDetail(99L, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH))
                .thenReturn(new SnapshotDetailsVO(99L, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH, "new-delta", false));
        List<SnapshotVO> all = new ArrayList<>(chain);
        all.add(incomplete);
        when(snapshotDao.listByVolumeId(VOLUME_ID)).thenReturn(all);
    }

    @Test
    public void deleteIsRefusedWhileAnotherTakeOfTheVolumeDidNotComplete() {
        mockIncompleteTakeOfAnotherSnapshot(mockChain(SNAPSHOT_ID));

        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        assertTrue(e.getMessage().contains("incomplete-snapshot"));
        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void revertIsRefusedWhileAnotherTakeOfTheVolumeDidNotComplete() {
        mockIncompleteTakeOfAnotherSnapshot(mockChain(SNAPSHOT_ID));

        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void takeIsRefusedWhileAnotherTakeOfTheVolumeDidNotComplete() {
        mockIncompleteTakeOfAnotherSnapshot(new ArrayList<>());
        mockSnapshotOnPrimary();

        assertThrows(InvalidParameterValueException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(strategy, never()).createSnapshotOnPrimary(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void deleteOfAnIncompleteTakeWhoseFileWasFrozenByALaterTakeDiscardsIt() {
        mockChain(5L);
        SnapshotDataStoreVO laterStoreRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(5L, DataStoreRole.Primary);
        when(laterStoreRef.getInstallPath()).thenReturn(VOLUME_PATH);
        SnapshotVO creating = mockIncompleteTake("new-delta", VOLUME_PATH, Volume.State.Ready, "newer-delta");
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(creating).setState(Snapshot.State.Destroyed);
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    // the host acted, but saving the outcome in the DB failed

    @Test
    public void takeSnapshotOfRunningVmWhoseNewPathCannotBeSavedKeepsTheVolumeReserved() {
        attachVolumeToVm();
        mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), any());
        when(volumeDao.update(Mockito.eq(VOLUME_ID), any())).thenThrow(new CloudRuntimeException("database error"));

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationSucceeded);
        verify(strategy, never()).markSnapshotOnPrimaryAsFailed(any());
        verify(snapshotDetailsDao, never()).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);
    }

    @Test
    public void revertWhoseNewPathCannotBeSavedKeepsTheVolumeReserved() {
        mockChain(SNAPSHOT_ID);
        RevertDiskOnlyVmSnapshotAnswer answer = mock(RevertDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        VolumeObjectTO revertedTO = new VolumeObjectTO();
        revertedTO.setPath("reverted-delta");
        when(answer.getVolumeObjectTos()).thenReturn(List.of(revertedTO));
        doReturn(answer).when(strategy).sendToHost(anyLong(), any());
        when(volumeDao.update(Mockito.eq(VOLUME_ID), any())).thenThrow(new CloudRuntimeException("database error"));

        CloudRuntimeException e = assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        assertTrue(e.getMessage().contains("reverted-delta"));
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void mergeWhoseMetadataCannotBeSavedKeepsTheVolumeReserved() {
        mockChain(SNAPSHOT_ID);
        doReturn(new Answer(null, true, null)).when(strategy).sendToHost(anyLong(), any());
        when(volumeDao.update(Mockito.eq(VOLUME_ID), any())).thenThrow(new CloudRuntimeException("database error"));

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationSucceeded);
    }

    @Test
    public void deleteOfAnIncompleteTakeOfAnExpungedVolumeDeletesTheFrozenFile() {
        SnapshotVO creating = mockIncompleteTake("new-delta", "frozen-10", Volume.State.Expunged, "new-delta");
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(creating);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(null);
        SnapshotDataStoreVO storeRef = mock(SnapshotDataStoreVO.class);
        when(storeRef.getDataStoreId()).thenReturn(POOL_ID);
        when(snapshotStoreDao.findOneBySnapshotAndDatastoreRole(SNAPSHOT_ID, DataStoreRole.Primary)).thenReturn(storeRef);
        doNothing().when(strategy).deleteFrozenFileOfExpungedVolume(creating, storeRef);

        assertTrue(strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(storeRef).setInstallPath("frozen-10");
        verify(strategy).deleteFrozenFileOfExpungedVolume(creating, storeRef);
        verify(creating).setState(Snapshot.State.Destroyed);
    }

    @Test
    public void takeSnapshotRecordsThePendingVolumePathBeforeSendingAndRemovesItOnSuccess() {
        mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getMapVolumeToSnapshotSize()).thenReturn(Map.of(VOLUME_UUID, 1L));
        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), commandCaptor.capture());

        strategy.takeSnapshot(snapshotInfo);

        String newPath = ((CreateDiskOnlyVmSnapshotCommand) commandCaptor.getValue()).getVolumeTosAndNewPaths().get(0).second();
        InOrder inOrder = Mockito.inOrder(snapshotDetailsDao, strategy);
        inOrder.verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH, newPath, false);
        inOrder.verify(strategy).sendToHost(eq(HOST_ID), any());
        inOrder.verify(snapshotDetailsDao).removeDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);
    }

    @Test
    public void takeSnapshotWhoseMetadataFailsAfterTheHostSucceededIsNotMarkedAsFailed() {
        SnapshotObject snapshotOnPrimary = mockSnapshotOnPrimary();
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getMapVolumeToSnapshotSize()).thenReturn(Map.of(VOLUME_UUID, 1L));
        doReturn(answer).when(strategy).sendToHost(eq(HOST_ID), any());
        Mockito.doThrow(new CloudRuntimeException("database error")).when(snapshotOnPrimary).processEvent(eq(ObjectInDataStoreStateMachine.Event.OperationSucceeded),
                any(CreateObjectAnswer.class));

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeVO).setPath(any());
        verify(volumeInfo).stateTransit(Volume.Event.OperationSucceeded);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(strategy, never()).markSnapshotOnPrimaryAsFailed(any());
        verify(snapshotDetailsDao, never()).removeDetail(SNAPSHOT_ID, SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT);
    }

    @Test
    public void markSnapshotOnPrimaryAsFailedNeverThrows() throws Exception {
        SnapshotObject snapshotOnPrimary = mock(SnapshotObject.class);
        Mockito.doThrow(new CloudRuntimeException("database error")).when(snapshotOnPrimary).processEvent(Snapshot.Event.OperationFailed);
        Mockito.doThrow(new CloudRuntimeException("database error")).when(snapshotOnPrimary).processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);

        strategy.markSnapshotOnPrimaryAsFailed(snapshotOnPrimary);

        verify(snapshotOnPrimary).processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
    }

    @Test
    public void deleteSnapshotDeletedConcurrentlyWhileWaitingForTheLockIsRefused() {
        mockChain(SNAPSHOT_ID);
        SnapshotVO destroyed = mock(SnapshotVO.class);
        when(destroyed.getState()).thenReturn(Snapshot.State.Destroyed);
        when(snapshotDao.findById(SNAPSHOT_ID)).thenReturn(snapshotVO, destroyed);

        assertThrows(InvalidParameterValueException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(strategy, never()).sendToHost(anyLong(), any());
        verify(volumeInfo, never()).stateTransit(any());
        verify(snapshotDao).releaseFromLockTable(SNAPSHOT_ID);
    }

    @Test
    public void takeSnapshotOfDetachedVolumeWithoutAnswerReleasesTheVolume() {
        mockSnapshotOnPrimary();
        doReturn(null).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.takeSnapshot(snapshotInfo));

        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void deleteSendsTheMergeWithTheAgentMergeTimeoutAsWait() {
        mockChain(SNAPSHOT_ID);
        assertEquals(KvmFileBasedNoCopySnapshotStrategy.MERGE_COMMAND_WAIT_SECONDS, deleteAndCaptureMerge().getWait());
    }

    @Test
    public void failedMergeMarksTheSnapshotAsNotRevertible() {
        mockChain(SNAPSHOT_ID);
        doReturn(new Answer(null, false, "blockcommit failed")).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE, "true", false);
        verify(volumeInfo).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void mergeWithoutAnswerKeepsTheVolumeReserved() {
        mockChain(SNAPSHOT_ID);
        doReturn(null).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.deleteSnapshot(SNAPSHOT_ID, null));

        verify(snapshotDetailsDao).addDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE, "true", false);
        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeVO, never()).setPath(any());
    }

    @Test
    public void revertOfAPartiallyMergedSnapshotIsRefused() {
        mockChain(SNAPSHOT_ID);
        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE))
                .thenReturn(new SnapshotDetailsVO(SNAPSHOT_ID, KvmFileBasedNoCopySnapshotStrategy.KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE, "true", false));

        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void revertWithoutAnswerKeepsTheVolumeReserved() {
        mockChain(SNAPSHOT_ID);
        doReturn(null).when(strategy).sendToHost(anyLong(), any());

        assertThrows(CloudRuntimeException.class, () -> strategy.revertSnapshot(snapshotInfo));

        verify(volumeInfo, never()).stateTransit(Volume.Event.OperationFailed);
        verify(volumeVO, never()).setPath(any());
    }

    @Test
    public void revertWhileTheVmIsStartingIsRefused() {
        mockChain(SNAPSHOT_ID);
        attachVolumeToVm();
        when(vm.getState()).thenReturn(VirtualMachine.State.Starting);

        assertThrows(InvalidParameterValueException.class, () -> strategy.revertSnapshot(snapshotInfo));
        verify(strategy, never()).sendToHost(anyLong(), any());
    }

    @Test
    public void backupSnapshotIsRefused() {
        assertThrows(CloudRuntimeException.class, () -> strategy.backupSnapshot(snapshotInfo));
    }

    @Test
    public void getHostIdForRunningVmIsItsHost() {
        Mockito.reset(strategy);
        attachVolumeToVm();
        assertEquals(HOST_ID, strategy.getHostIdForOperation(volumeInfo, vm));
    }

    @Test
    public void createSnapshotOnPrimaryRequestsCreation() throws Exception {
        Mockito.reset(strategy);
        DataStore dataStore = mock(DataStore.class);
        SnapshotObject snapshotOnPrimary = mock(SnapshotObject.class);
        when(snapshotInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.create(snapshotInfo)).thenReturn(snapshotOnPrimary);

        assertSame(snapshotOnPrimary, strategy.createSnapshotOnPrimary(snapshotInfo));

        verify(snapshotOnPrimary).processEvent(Snapshot.Event.CreateRequested);
        verify(snapshotOnPrimary).processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);
    }
}
