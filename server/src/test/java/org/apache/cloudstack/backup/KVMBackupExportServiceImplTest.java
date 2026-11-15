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
package org.apache.cloudstack.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class KVMBackupExportServiceImplTest {

    @InjectMocks
    KVMBackupExportServiceImpl service;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    BackupDao backupDao;

    @Mock
    ImageTransferDao imageTransferDao;

    @Mock
    AgentManager agentManager;

    @Mock
    HostDao hostDao;

    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    AccountService accountService;

    @Mock
    VirtualMachineManager virtualMachineManager;

    @Mock
    BackupVO backup;

    @Mock
    VolumeVO volume;

    @Mock
    HostVO hostVO;

    @Mock
    VolumeDataFactory volumeDataFactory;

    @Mock
    CallContext callContext;

    @Mock
    User user;

    VMInstanceVO vm;

    @Before
    public void setUp() {
        vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(1L);
        when(vm.getUuid()).thenReturn("vm-uuid");
    }

    @Test
    public void validateVmVolumesForBackup_noNonReadyVolumes_doesNotThrow() {
        when(volumeDao.findByInstanceAndNotStates(1L, Volume.State.Ready)).thenReturn(Collections.emptyList());

        service.validateVmVolumesForBackup(vm);
    }

    @Test
    public void validateVmVolumesForBackup_oneVolumeNotReady_throwsWithVolumeAndInstanceId() {
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getUuid()).thenReturn("vol-not-ready");
        when(volumeDao.findByInstanceAndNotStates(1L, Volume.State.Ready)).thenReturn(List.of(vol));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> service.validateVmVolumesForBackup(vm));

        assert ex.getMessage().contains("vol-not-ready");
        assert ex.getMessage().contains("vm-uuid");
    }

    @Test
    public void validateVmVolumesForBackup_multipleVolumesNotReady_throwsWithAllVolumeIds() {
        VolumeVO vol1 = mock(VolumeVO.class);
        VolumeVO vol2 = mock(VolumeVO.class);
        when(vol1.getUuid()).thenReturn("vol-a");
        when(vol2.getUuid()).thenReturn("vol-b");
        when(volumeDao.findByInstanceAndNotStates(1L, Volume.State.Ready)).thenReturn(List.of(vol1, vol2));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> service.validateVmVolumesForBackup(vm));

        assert ex.getMessage().contains("vol-a");
        assert ex.getMessage().contains("vol-b");
        assert ex.getMessage().contains("vm-uuid");
    }

    private StartBackupCmd mockCmd(Long vmId, String name, String description) {
        StartBackupCmd cmd = mock(StartBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getName()).thenReturn(name);
        when(cmd.getDescription()).thenReturn(description);
        return cmd;
    }

    private void stubVmRunningWithHost(Long vmId, VMInstanceVO vmInstance, Long hostId) {
        when(vmInstanceDao.findById(vmId)).thenReturn(vmInstance);
        when(vmInstance.getState()).thenReturn(State.Running);
        when(vmInstance.getDataCenterId()).thenReturn(10L);
        when(vmInstance.getAccountId()).thenReturn(100L);
        when(vmInstance.getDomainId()).thenReturn(200L);
        when(backupDao.findByVmId(vmId)).thenReturn(null);
        when(volumeDao.findByInstanceAndNotStates(vmId, Volume.State.Ready)).thenReturn(Collections.emptyList());
        when(virtualMachineManager.findClusterAndHostIdForVm(vmInstance, false))
                .thenReturn(new Pair<>(5L, hostId));
        when(vmInstanceDetailsDao.listDetailsKeyPairs(vmId)).thenReturn(new HashMap<>());
    }

    @Test
    public void createBackup_instanceNotFound_throws() {
        when(vmInstanceDao.findById(99L)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> service.createBackup(mockCmd(99L, "backup", null)));
    }

    @Test
    public void createBackup_instanceNotRunningOrStopped_throws() {
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Migrating);
        when(vm.getDataCenterId()).thenReturn(10L);

        assertThrows(CloudRuntimeException.class,
                () -> service.createBackup(mockCmd(1L, "backup", null)));
    }

    @Test
    public void createBackup_backupAlreadyInProgress_throws() {
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Running);
        when(vm.getDataCenterId()).thenReturn(10L);
        BackupVO existing = mock(BackupVO.class);
        when(existing.getStatus()).thenReturn(Backup.Status.BackingUp);
        when(backupDao.findByVmId(1L)).thenReturn(existing);

        assertThrows(CloudRuntimeException.class,
                () -> service.createBackup(mockCmd(1L, "backup", null)));
    }

    @Test
    public void createBackup_hostCannotBeDetermined_throws() {
        stubVmRunningWithHost(1L, vm, null);

        assertThrows(CloudRuntimeException.class,
                () -> service.createBackup(mockCmd(1L, "backup", null)));
    }

    @Test
    public void createBackup_happyPath_persistsBackupWithQueuedStatus() {
        stubVmRunningWithHost(1L, vm, 42L);
        BackupVO persisted = mock(BackupVO.class);
        when(backupDao.persist(any(BackupVO.class))).thenReturn(persisted);

        Backup result = service.createBackup(mockCmd(1L, "my-backup", "desc"));

        assertNotNull(result);
        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        verify(backupDao).persist(captor.capture());
        assertEquals(Backup.Status.Queued, captor.getValue().getStatus());
        assertEquals("my-backup", captor.getValue().getName());
        assertEquals("desc", captor.getValue().getDescription());
        assertEquals(Long.valueOf(42L), captor.getValue().getHostId());
        assertEquals(Long.valueOf(1L), captor.getValue().getVmId());
    }

    @Test
    public void createBackup_noNameProvided_generatesNameFromVmId() {
        stubVmRunningWithHost(1L, vm, 42L);
        when(backupDao.persist(any(BackupVO.class))).thenReturn(mock(BackupVO.class));

        service.createBackup(mockCmd(1L, null, null));

        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        verify(backupDao).persist(captor.capture());
        assertNotNull(captor.getValue().getName());
        assert captor.getValue().getName().startsWith("1-");
    }

    @Test
    public void createBackup_existingBackupNotInProgress_proceedsNormally() {
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Stopped);
        when(vm.getDataCenterId()).thenReturn(10L);
        when(vm.getAccountId()).thenReturn(100L);
        when(vm.getDomainId()).thenReturn(200L);
        BackupVO existing = mock(BackupVO.class);
        when(existing.getStatus()).thenReturn(Backup.Status.BackedUp);
        when(backupDao.findByVmId(1L)).thenReturn(existing);
        when(volumeDao.findByInstanceAndNotStates(1L, Volume.State.Ready)).thenReturn(Collections.emptyList());
        when(virtualMachineManager.findClusterAndHostIdForVm(vm, false)).thenReturn(new Pair<>(5L, 42L));
        when(vmInstanceDetailsDao.listDetailsKeyPairs(1L)).thenReturn(new HashMap<>());
        when(backupDao.persist(any(BackupVO.class))).thenReturn(mock(BackupVO.class));

        Backup result = service.createBackup(mockCmd(1L, "backup", null));

        assertNotNull(result);
    }

    @Test
    public void createBackup_withActiveCheckpoint_setsFromCheckpointId() {
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Running);
        when(vm.getDataCenterId()).thenReturn(10L);
        when(vm.getAccountId()).thenReturn(100L);
        when(vm.getDomainId()).thenReturn(200L);
        when(backupDao.findByVmId(1L)).thenReturn(null);
        when(volumeDao.findByInstanceAndNotStates(1L, Volume.State.Ready)).thenReturn(Collections.emptyList());
        when(virtualMachineManager.findClusterAndHostIdForVm(vm, false)).thenReturn(new Pair<>(5L, 42L));
        Map<String, String> details = new HashMap<>();
        details.put("active.checkpoint.id", "ckp-abc123");
        when(vmInstanceDetailsDao.listDetailsKeyPairs(1L)).thenReturn(details);
        when(backupDao.persist(any(BackupVO.class))).thenReturn(mock(BackupVO.class));

        service.createBackup(mockCmd(1L, "backup", null));

        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        verify(backupDao).persist(captor.capture());
        assertEquals("ckp-abc123", captor.getValue().getFromCheckpointId());
    }

    @Test
    public void createBackup_noActiveCheckpoint_fromCheckpointIdIsNull() {
        stubVmRunningWithHost(1L, vm, 42L);
        when(backupDao.persist(any(BackupVO.class))).thenReturn(mock(BackupVO.class));

        service.createBackup(mockCmd(1L, "backup", null));

        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        verify(backupDao).persist(captor.capture());
        assert captor.getValue().getFromCheckpointId() == null;
        assertNotNull(captor.getValue().getToCheckpointId());
        assert captor.getValue().getToCheckpointId().startsWith("ckp-");
    }

    @Test
    public void removeFailedBackup_setsErrorStatusAndRemovesRecord() {
        BackupVO backup = mock(BackupVO.class);
        when(backup.getId()).thenReturn(10L);

        service.removeFailedBackup(backup);

        verify(backup).setStatus(Backup.Status.Error);
        verify(backupDao).update(10L, backup);
        verify(backupDao).remove(10L);
    }

    @Test
    public void finalizeImageTransfer_downloadStoppedVm_stopNbdFails_throwsAndDoesNotUpdate() throws Exception {
        ImageTransferVO transfer = new ImageTransferVO("tx-download-fail", 11L, 21L, 31L, "sock",
                ImageTransferVO.Phase.transferring, ImageTransfer.Direction.download, 1L, 1L, 1L);
        ReflectionTestUtils.setField(transfer, "id", 501L);
        when(imageTransferDao.findById(501L)).thenReturn(transfer);
        when(backupDao.findById(11L)).thenReturn(backup);
        when(backup.getHostId()).thenReturn(31L);
        when(backup.getVmId()).thenReturn(41L);
        when(vmInstanceDao.findById(41L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Stopped);
        when(agentManager.send(eq(31L), any(Command.class))).thenReturn(
                new Answer(null, true, null),
                new Answer(null, false, "stop failed")
        );

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> service.finalizeImageTransfer(501L));

        assert ex.getMessage().contains("Failed to stop the nbd server");
        verify(imageTransferDao, never()).update(eq(501L), any(ImageTransferVO.class));
        verify(imageTransferDao, never()).remove(501L);
    }

    @Test
    public void createImageTransfer_uploadFileBackend_success() throws Exception {
        ReflectionTestUtils.setField(BackupManager.BackupFrameworkEnabled, "_value", Boolean.FALSE);
        try (MockedStatic<CallContext> mocked = mockStatic(CallContext.class)) {
            mocked.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingUser()).thenReturn(user);

            when(volumeDao.findById(601L)).thenReturn(volume);
            when(volume.getId()).thenReturn(601L);
            when(volume.getState()).thenReturn(Volume.State.Ready);
            when(volume.getDataCenterId()).thenReturn(1L);
            when(volume.getPoolId()).thenReturn(701L);
            when(volume.getPath()).thenReturn("vol-601.qcow2");
            when(volume.getAccountId()).thenReturn(11L);
            when(volume.getDomainId()).thenReturn(12L);
            when(imageTransferDao.findByVolume(601L)).thenReturn(null);

            StoragePoolVO pool = new StoragePoolVO();
            pool.setId(701L);
            pool.setScope(ScopeType.ZONE);
            pool.setDataCenterId(1L);
            pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
            pool.setUuid("pool-701");
            pool.setPath("/primary");
            when(primaryDataStoreDao.findById(701L)).thenReturn(pool);

            when(hostDao.findByDataCenterId(1L)).thenReturn(List.of(hostVO));
            when(hostVO.getId()).thenReturn(801L);
            when(hostVO.getDataCenterId()).thenReturn(1L);
            when(agentManager.send(eq(801L), any(Command.class)))
                    .thenReturn(new CreateImageTransferAnswer(null, true, null, "ticket-1", "https://transfer/file"));

            final ImageTransferVO[] persisted = new ImageTransferVO[1];
            when(imageTransferDao.persist(any(ImageTransferVO.class))).thenAnswer(i -> {
                persisted[0] = i.getArgument(0);
                ReflectionTestUtils.setField(persisted[0], "id", 901L);
                return persisted[0];
            });
            when(imageTransferDao.findById(901L)).thenAnswer(i -> persisted[0]);

            ImageTransfer created = service.createImageTransfer(601L, null, ImageTransfer.Direction.upload, ImageTransfer.Format.cow);

            assertNotNull(created);
            assertEquals(901L, created.getId());
            assertEquals(ImageTransfer.Direction.upload, created.getDirection());
            verify(volumeDao).updateState(eq(Volume.State.Ready), eq(Volume.Event.RestoreRequested), eq(Volume.State.Restoring), eq(volume), isNull());
            verify(agentManager, times(1)).send(eq(801L), any(Command.class));
        }
    }

    @Test
    public void createImageTransfer_downloadStoppedVm_startsNbdAndCreatesTransfer() throws Exception {
        ReflectionTestUtils.setField(BackupManager.BackupFrameworkEnabled, "_value", Boolean.FALSE);
        try (MockedStatic<CallContext> mocked = mockStatic(CallContext.class)) {
            mocked.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingUser()).thenReturn(user);

            when(volumeDao.findById(602L)).thenReturn(volume);
            when(volume.getId()).thenReturn(602L);
            when(volume.getUuid()).thenReturn("vol-602");
            when(volume.getDataCenterId()).thenReturn(1L);
            when(volume.getPoolId()).thenReturn(702L);
            when(volume.getPath()).thenReturn("vol-602.raw");
            when(imageTransferDao.findByVolume(602L)).thenReturn(null);

            StoragePoolVO pool = new StoragePoolVO();
            pool.setId(702L);
            pool.setScope(ScopeType.ZONE);
            pool.setDataCenterId(1L);
            pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
            pool.setUuid("pool-702");
            when(primaryDataStoreDao.findById(702L)).thenReturn(pool);

            when(backupDao.findById(612L)).thenReturn(backup);
            when(backup.getVmId()).thenReturn(55L);
            when(backup.getHostId()).thenReturn(802L);
            when(backup.getUuid()).thenReturn("backup-612");
            when(backup.getFromCheckpointId()).thenReturn("ckp-prev");
            when(backup.getAccountId()).thenReturn(11L);
            when(backup.getDomainId()).thenReturn(12L);
            when(backup.getZoneId()).thenReturn(1L);
            when(vmInstanceDao.findById(55L)).thenReturn(vm);
            when(vm.getState()).thenReturn(State.Stopped);
            when(vmInstanceDetailsDao.listDetailsKeyPairs(55L)).thenReturn(Map.of(VmDetailConstants.ACTIVE_CHECKPOINT_ID, "ckp-active"));
            when(hostDao.findById(802L)).thenReturn(hostVO);
            when(hostVO.getDataCenterId()).thenReturn(1L);
            when(volumeDataFactory.getVolume(602L)).thenReturn(mock(VolumeInfo.class));

            when(agentManager.send(eq(802L), any(Command.class))).thenReturn(
                    new StartNBDServerAnswer(null, true, null),
                    new CreateImageTransferAnswer(null, true, null, "ticket-2", "https://transfer/nbd")
            );

            final ImageTransferVO[] persisted = new ImageTransferVO[1];
            when(imageTransferDao.persist(any(ImageTransferVO.class))).thenAnswer(i -> {
                persisted[0] = i.getArgument(0);
                ReflectionTestUtils.setField(persisted[0], "id", 902L);
                return persisted[0];
            });
            when(imageTransferDao.findById(902L)).thenAnswer(i -> persisted[0]);

            ImageTransfer created = service.createImageTransfer(602L, 612L, ImageTransfer.Direction.download, ImageTransfer.Format.raw);

            assertNotNull(created);
            assertEquals(902L, created.getId());
            assertEquals(ImageTransfer.Direction.download, created.getDirection());
            verify(agentManager, times(2)).send(eq(802L), any(Command.class));
        }
    }
}
