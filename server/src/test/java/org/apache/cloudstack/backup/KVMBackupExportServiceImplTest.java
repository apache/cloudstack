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
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeBackupExport;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
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
    BackupDetailsDao backupDetailsDao;

    @Mock
    DataStoreManager dataStoreManager;

    @Mock
    VolumeDetailsDao volumeDetailsDao;

    @Mock
    ConfigurationDao configDao;

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

    private StoragePoolVO mockStoragePool(long poolId, Storage.StoragePoolType type, PrimaryDataStoreDriver driver) {
        StoragePoolVO pool = new StoragePoolVO();
        pool.setId(poolId);
        pool.setPoolType(type);
        pool.setName("pool-" + poolId);
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        if (driver != null) {
            PrimaryDataStore dataStore = mock(PrimaryDataStore.class);
            when(dataStore.getDriver()).thenReturn(driver);
            when(dataStoreManager.getPrimaryDataStore(poolId)).thenReturn(dataStore);
        }
        return pool;
    }

    private VolumeVO mockVolume(long volumeId, long poolId) {
        VolumeVO vol = mock(VolumeVO.class);
        Mockito.lenient().when(vol.getId()).thenReturn(volumeId);
        when(vol.getPoolId()).thenReturn(poolId);
        return vol;
    }

    private VolumeInfo mockVolumeInfo(long volumeId, long poolId) {
        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        Mockito.lenient().when(volumeInfo.getId()).thenReturn(volumeId);
        when(volumeInfo.getPoolId()).thenReturn(poolId);
        when(volumeDataFactory.getVolume(volumeId)).thenReturn(volumeInfo);
        return volumeInfo;
    }

    private BackupVO newBackup(long backupId, long vmId) {
        BackupVO newBackup = new BackupVO();
        ReflectionTestUtils.setField(newBackup, "id", backupId);
        newBackup.setVmId(vmId);
        newBackup.setHostId(42L);
        newBackup.setToCheckpointId("ckp-1");
        newBackup.setStatus(Backup.Status.Queued);
        return newBackup;
    }

    private StartBackupCmd mockStartCmd(long backupId, long vmId) {
        StartBackupCmd cmd = mock(StartBackupCmd.class);
        when(cmd.getEntityId()).thenReturn(backupId);
        when(cmd.getVmId()).thenReturn(vmId);
        return cmd;
    }

    @Test
    public void isStorageDriverExport_volumesOnExportedAndFilePools_throws() {
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        StoragePoolVO exportedPool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
        when(driver.canExportVolumesForBackup(exportedPool)).thenReturn(true);
        mockStoragePool(8L, Storage.StoragePoolType.NetworkFilesystem, null);

        assertThrows(CloudRuntimeException.class,
                () -> service.isStorageDriverExport(List.of(mockVolume(3L, 7L), mockVolume(4L, 8L))));
    }

    @Test
    public void startBackup_storageDriverExport_exportsVolumesWithoutHypervisorBackupJob() throws Exception {
        KVMBackupExportServiceImpl spyService = spy(service);
        doNothing().when(spyService).queueBackupFinalizeWaitWorkJob(any(), any());
        BackupVO newBackup = newBackup(5L, 1L);
        when(backupDao.findById(5L)).thenReturn(newBackup);
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Running);
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
        when(driver.canExportVolumesForBackup(pool)).thenReturn(true);
        VolumeVO vol = mockVolume(3L, 7L);
        when(volumeDao.findByInstance(1L)).thenReturn(List.of(vol));
        VolumeInfo volumeInfo = mockVolumeInfo(3L, 7L);
        when(driver.startBackupExport(pool, "ckp-1", List.of(volumeInfo), Collections.emptyMap()))
                .thenReturn(List.of(new VolumeBackupExport(3L, 11L, "/dev/drbd1004", 1073741824L)));

        spyService.startBackup(mockStartCmd(5L, 1L));

        ArgumentCaptor<String> detailValue = ArgumentCaptor.forClass(String.class);
        verify(backupDetailsDao).addDetail(eq(5L), eq("volume.export.3"), detailValue.capture(), eq(false));
        assert detailValue.getValue().contains("/dev/drbd1004");
        assertEquals(Backup.Status.ReadyForImageTransfer, newBackup.getStatus());
        assertNotNull(newBackup.getCheckpointCreateTime());
        verify(agentManager, never()).send(any(Long.class), any(Command.class));
        verify(spyService).queueBackupFinalizeWaitWorkJob(vm, newBackup);
    }

    @Test
    public void startBackup_storageDriverExportFails_releasesExportsAndRemovesBackup() {
        BackupVO newBackup = newBackup(5L, 1L);
        when(backupDao.findById(5L)).thenReturn(newBackup);
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(vm.getState()).thenReturn(State.Stopped);
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
        when(driver.canExportVolumesForBackup(pool)).thenReturn(true);
        VolumeVO vol = mockVolume(3L, 7L);
        when(volumeDao.findByInstance(1L)).thenReturn(List.of(vol));
        VolumeInfo volumeInfo = mockVolumeInfo(3L, 7L);
        when(driver.startBackupExport(pool, "ckp-1", List.of(volumeInfo), Collections.emptyMap()))
                .thenThrow(new CloudRuntimeException("Linstor: no usable host"));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> service.startBackup(mockStartCmd(5L, 1L)));

        assert ex.getMessage().contains("no usable host");
        verify(driver).finishBackupExport(pool, "ckp-1", List.of(volumeInfo));
        verify(backupDao).remove(5L);
        verify(backupDetailsDao, never()).addDetail(any(Long.class), any(), any(), any(Boolean.class));
    }

    @Test
    public void finalizeBackup_storageDriverExport_releasesExportsInsteadOfStoppingBackupJob() throws Exception {
        BackupVO newBackup = newBackup(5L, 1L);
        newBackup.setCheckpointCreateTime(1700000000L);
        when(backupDao.findById(5L)).thenReturn(newBackup);
        when(vmInstanceDao.findById(1L)).thenReturn(vm);
        when(imageTransferDao.listByBackupId(5L)).thenReturn(Collections.emptyList());
        when(backupDetailsDao.listDetailsKeyPairs(5L)).thenReturn(Map.of("volume.export.3",
                "{\"volumeId\":3,\"hostId\":11,\"devicePath\":\"/dev/drbd1004\",\"sizeBytes\":1073741824," +
                        "\"changeTrackingId\":\"CBT-3\"}"));
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
        when(driver.canExportVolumesForBackup(pool)).thenReturn(true);
        VolumeInfo volumeInfo = mockVolumeInfo(3L, 7L);
        when(vmInstanceDetailsDao.listDetailsKeyPairs(1L)).thenReturn(new HashMap<>());
        FinalizeBackupCmd cmd = mock(FinalizeBackupCmd.class);
        when(cmd.getVmId()).thenReturn(1L);
        when(cmd.getBackupId()).thenReturn(5L);

        service.finalizeBackup(cmd);

        verify(driver).finishBackupExport(pool, "ckp-1", List.of(volumeInfo));
        verify(agentManager, never()).send(any(Long.class), any(Command.class));
        verify(vmInstanceDetailsDao).addDetail(1L, VmDetailConstants.ACTIVE_CHECKPOINT_ID, "ckp-1", false);
        // the next backup is incremental to this one
        verify(volumeDetailsDao).addDetail(3L, KVMBackupExportServiceImpl.VOLUME_DETAIL_BACKUP_CHANGE_TRACKING_ID,
                "ckp-1:CBT-3", false);
        assertEquals(Backup.Status.BackedUp, newBackup.getStatus());
    }

    @Test
    public void saveChangeTrackingIds_volumeWithoutChangeTracking_removesOldId() {
        service.saveChangeTrackingIds(newBackup(5L, 1L),
                List.of(new VolumeBackupExport(3L, 11L, "/dev/drbd1004", 1073741824L)));

        verify(volumeDetailsDao).removeDetail(3L, KVMBackupExportServiceImpl.VOLUME_DETAIL_BACKUP_CHANGE_TRACKING_ID);
        verify(volumeDetailsDao, never()).addDetail(any(Long.class), any(), any(), any(Boolean.class));
    }

    @Test
    public void getChangeTrackingIds_onlyIdsOfBackupAtCheckpoint() {
        String key = KVMBackupExportServiceImpl.VOLUME_DETAIL_BACKUP_CHANGE_TRACKING_ID;
        when(volumeDetailsDao.findDetail(3L, key)).thenReturn(new VolumeDetailVO(3L, key, "ckp-0:CBT-3", false));
        // e.g. the active checkpoint was deleted, or the volume was backed up with another instance
        when(volumeDetailsDao.findDetail(4L, key)).thenReturn(new VolumeDetailVO(4L, key, "ckp-9:CBT-4", false));
        List<VolumeVO> volumes = List.of(mockVolume(3L, 7L), mockVolume(4L, 7L), mockVolume(5L, 7L));

        assertEquals(Map.of(3L, "CBT-3"), service.getChangeTrackingIds(volumes, "ckp-0"));
        assertEquals(Collections.emptyMap(), service.getChangeTrackingIds(volumes, null));
    }

    private List<Command> createDownloadTransferOfExport(String exportJson) throws Exception {
        ReflectionTestUtils.setField(BackupManager.BackupFrameworkEnabled, "_value", Boolean.FALSE);
        try (MockedStatic<CallContext> mocked = mockStatic(CallContext.class)) {
            mocked.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingUser()).thenReturn(user);

            when(volumeDao.findById(3L)).thenReturn(volume);
            when(volume.getId()).thenReturn(3L);
            when(volume.getUuid()).thenReturn("vol-3");
            when(volume.getDataCenterId()).thenReturn(1L);
            when(imageTransferDao.findByVolume(3L)).thenReturn(null);

            BackupVO newBackup = newBackup(5L, 1L);
            newBackup.setFromCheckpointId("ckp-0");
            when(backupDao.findById(5L)).thenReturn(newBackup);
            when(vmInstanceDao.findById(1L)).thenReturn(vm);
            when(backupDetailsDao.listDetailsKeyPairs(5L)).thenReturn(Map.of("volume.export.3", exportJson));
            when(hostDao.findById(11L)).thenReturn(hostVO);
            when(hostVO.getDataCenterId()).thenReturn(1L);
            ArgumentCaptor<Command> commands = ArgumentCaptor.forClass(Command.class);
            when(agentManager.send(eq(11L), commands.capture())).thenReturn(
                    new StartNBDServerAnswer(null, true, null),
                    new CreateImageTransferAnswer(null, true, null, "ticket-3", "https://transfer/nbd"));
            when(imageTransferDao.persist(any(ImageTransferVO.class))).thenAnswer(i -> {
                ImageTransferVO transfer = i.getArgument(0);
                ReflectionTestUtils.setField(transfer, "id", 903L);
                return transfer;
            });

            service.createImageTransfer(3L, 5L, ImageTransfer.Direction.download, ImageTransfer.Format.raw);

            return commands.getAllValues();
        }
    }

    @Test
    public void createImageTransfer_downloadStorageDriverExport_servesDeviceOnExportHost() throws Exception {
        List<Command> commands = createDownloadTransferOfExport(
                "{\"volumeId\":3,\"hostId\":11,\"devicePath\":\"/dev/drbd1004\",\"sizeBytes\":1073741824}");

        StartNBDServerCommand nbdCmd = (StartNBDServerCommand) commands.get(0);
        assertEquals("/dev/drbd1004", nbdCmd.getVolumePath());
        assertEquals(Long.valueOf(1073741824L), nbdCmd.getBlockDeviceSizeBytes());
        assertEquals("download", nbdCmd.getDirection());
        assert nbdCmd.getPassphrase() == null;
        CreateImageTransferCommand transferCmd = (CreateImageTransferCommand) commands.get(1);
        // the device has no dirty bitmap, and the storage lists no extents of this export either
        assert transferCmd.getCheckpointId() == null;
        assert transferCmd.getExtentsSource() == null;
        assertEquals(nbdCmd.getSocket(), transferCmd.getSocket());
    }

    private CreateImageTransferCommand createDownloadTransferOfExportOnPool7(String exportJson) throws Exception {
        StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, null);
        pool.setUuid("pool-7");
        when(volume.getPoolId()).thenReturn(7L);
        return (CreateImageTransferCommand) createDownloadTransferOfExport(exportJson).get(1);
    }

    @Test
    public void createImageTransfer_downloadIncrementalStorageDriverExport_hasStorageListExtents() throws Exception {
        CreateImageTransferCommand transferCmd = createDownloadTransferOfExportOnPool7(
                "{\"volumeId\":3,\"hostId\":11,\"devicePath\":\"/dev/drbd1004\",\"sizeBytes\":1073741824," +
                        "\"incremental\":true,\"extentsSource\":{\"snapshot\":\"ckp-1\",\"mode\":\"dirty\"}}");

        assert transferCmd.getCheckpointId() == null;
        assertEquals(Map.of("snapshot", "ckp-1", "mode", "dirty"), transferCmd.getExtentsSource());
        assertEquals(Storage.StoragePoolType.Linstor, transferCmd.getExtentsPoolType());
        assertEquals("pool-7", transferCmd.getExtentsPoolUuid());
    }

    @Test
    public void createImageTransfer_downloadFullStorageDriverExport_hasStorageListExtents() throws Exception {
        // a full export whose storage knows the extents that read as zeros
        CreateImageTransferCommand transferCmd = createDownloadTransferOfExportOnPool7(
                "{\"volumeId\":3,\"hostId\":11,\"devicePath\":\"/dev/drbd1004\",\"sizeBytes\":1073741824," +
                        "\"extentsSource\":{\"snapshot\":\"ckp-1\",\"mode\":\"zero\"}}");

        assertEquals(Map.of("snapshot", "ckp-1", "mode", "zero"), transferCmd.getExtentsSource());
        assertEquals("pool-7", transferCmd.getExtentsPoolUuid());
    }

    /**
     * Creates an upload of a qcow2 image into volume 601 on pool 7, whose driver exports the volumes as devices.
     * @param staging what the driver returns for the temporary device
     * @return the commands sent to the host
     */
    private List<Command> createQcow2UploadToStorageDriverExport(PrimaryDataStoreDriver driver, VolumeBackupExport staging,
            Answer transferAnswer) throws Exception {
        ReflectionTestUtils.setField(BackupManager.BackupFrameworkEnabled, "_value", Boolean.FALSE);
        try (MockedStatic<CallContext> mocked = mockStatic(CallContext.class)) {
            mocked.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingUser()).thenReturn(user);

            when(volumeDao.findById(601L)).thenReturn(volume);
            when(volume.getId()).thenReturn(601L);
            when(volume.getDataCenterId()).thenReturn(1L);
            when(volume.getPoolId()).thenReturn(7L);
            Mockito.lenient().when(volume.getState()).thenReturn(Volume.State.Ready);
            when(imageTransferDao.findByVolume(601L)).thenReturn(null);
            StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
            when(driver.canExportVolumesForBackup(pool)).thenReturn(true);
            VolumeInfo volumeInfo = mock(VolumeInfo.class);
            when(volumeDataFactory.getVolume(601L)).thenReturn(volumeInfo);
            when(driver.prepareVolumeForRestore(pool, volumeInfo))
                    .thenReturn(new VolumeBackupExport(601L, 11L, "/dev/drbd1001", 1073741824L));
            when(hostDao.findById(11L)).thenReturn(hostVO);
            when(hostVO.getId()).thenReturn(11L);
            when(driver.createUploadStaging(eq(pool), eq(volumeInfo), startsWith("upload-"), eq(11L),
                    eq(KVMBackupExportServiceImpl.getUploadStagingSize(1073741824L)))).thenReturn(staging);
            ArgumentCaptor<Command> commands = ArgumentCaptor.forClass(Command.class);
            if (staging != null) {
                when(hostVO.getDataCenterId()).thenReturn(1L);
                when(agentManager.send(eq(11L), commands.capture())).thenReturn(transferAnswer);
                Mockito.lenient().when(imageTransferDao.persist(any(ImageTransferVO.class))).thenAnswer(i -> i.getArgument(0));
            }

            service.createImageTransfer(601L, null, ImageTransfer.Direction.upload, ImageTransfer.Format.cow);

            return commands.getAllValues();
        }
    }

    @Test
    public void createImageTransfer_uploadQcow2ToStorageDriverExport_uploadsToTemporaryDevice() throws Exception {
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);
        List<Command> commands = createQcow2UploadToStorageDriverExport(driver,
                new VolumeBackupExport(601L, 11L, "/dev/drbd1002", 1210056704L),
                new CreateImageTransferAnswer(null, true, null, "ticket-1", "https://transfer/file"));

        CreateImageTransferCommand transferCmd = (CreateImageTransferCommand) commands.get(0);
        assertEquals(ImageTransfer.Backend.file, transferCmd.getBackend());
        assertEquals("/dev/drbd1002", transferCmd.getFile());
        ArgumentCaptor<String> stagingName = ArgumentCaptor.forClass(String.class);
        verify(driver).createUploadStaging(any(), any(), stagingName.capture(), eq(11L), any(Long.class));
        verify(volumeDetailsDao).addDetail(601L, KVMBackupExportServiceImpl.VOLUME_DETAIL_UPLOAD_STAGING,
                stagingName.getValue(), false);
        verify(driver, never()).deleteUploadStaging(any(), any());
    }

    @Test
    public void createImageTransfer_uploadQcow2ToDriverWithoutTemporaryDevices_throws() throws Exception {
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> createQcow2UploadToStorageDriverExport(driver, null, null));

        assert ex.getMessage().contains("Only raw images can be uploaded");
        verify(agentManager, never()).send(any(Long.class), any(Command.class));
    }

    @Test
    public void createImageTransfer_uploadQcow2HostFails_deletesTemporaryDevice() throws Exception {
        PrimaryDataStoreDriver driver = mock(PrimaryDataStoreDriver.class);

        assertThrows(CloudRuntimeException.class, () -> createQcow2UploadToStorageDriverExport(driver,
                new VolumeBackupExport(601L, 11L, "/dev/drbd1002", 1210056704L),
                new CreateImageTransferAnswer(null, false, "no image server", null, null)));

        verify(driver).deleteUploadStaging(any(), startsWith("upload-"));
        verify(volumeDetailsDao, never()).addDetail(any(Long.class), any(), any(), any(Boolean.class));
    }

    /**
     * Finalizes, or cancels, the upload of a qcow2 image to the temporary device upload-tx-up for volume 601 on pool 7.
     * @return the finalize command sent to the host
     */
    private FinalizeImageTransferCommand finalizeQcow2UploadToStorageDriverExport(PrimaryDataStoreDriver driver,
            boolean cancel) throws Exception {
        ImageTransferVO transfer = new ImageTransferVO("tx-up", 601L, 11L, "/dev/drbd1002",
                ImageTransferVO.Phase.transferring, ImageTransfer.Direction.upload, 1L, 1L, 1L);
        ReflectionTestUtils.setField(transfer, "id", 502L);
        when(imageTransferDao.findById(502L)).thenReturn(transfer);
        when(volumeDetailsDao.findDetail(601L, KVMBackupExportServiceImpl.VOLUME_DETAIL_UPLOAD_STAGING))
                .thenReturn(new VolumeDetailVO(601L, KVMBackupExportServiceImpl.VOLUME_DETAIL_UPLOAD_STAGING, "upload-tx-up", false));
        when(volumeDao.findById(601L)).thenReturn(volume);
        when(volume.getPoolId()).thenReturn(7L);
        Mockito.lenient().when(volume.getPath()).thenReturn("vol-601");
        when(volume.getState()).thenReturn(Volume.State.Restoring);
        StoragePoolVO pool = mockStoragePool(7L, Storage.StoragePoolType.Linstor, driver);
        pool.setUuid("pool-7");
        ArgumentCaptor<Command> commands = ArgumentCaptor.forClass(Command.class);
        when(agentManager.send(eq(11L), commands.capture())).thenReturn(new Answer(null, true, null));

        if (cancel) {
            service.cancelImageTransfer(502L);
        } else {
            service.finalizeImageTransfer(502L);
        }

        verify(driver).deleteUploadStaging(pool, "upload-tx-up");
        verify(volumeDetailsDao).removeDetail(601L, KVMBackupExportServiceImpl.VOLUME_DETAIL_UPLOAD_STAGING);
        verify(imageTransferDao).remove(502L);
        return (FinalizeImageTransferCommand) commands.getAllValues().get(1);
    }

    @Test
    public void finalizeImageTransfer_qcow2UploadToStorageDriverExport_convertsIntoVolume() throws Exception {
        FinalizeImageTransferCommand finalizeCmd = finalizeQcow2UploadToStorageDriverExport(
                mock(PrimaryDataStoreDriver.class), false);

        assertEquals("/dev/drbd1002", finalizeCmd.getConvertSource());
        assertEquals(Storage.StoragePoolType.Linstor, finalizeCmd.getConvertPoolType());
        assertEquals("pool-7", finalizeCmd.getConvertPoolUuid());
        assertEquals("vol-601", finalizeCmd.getConvertVolumePath());
        assertEquals(10800, finalizeCmd.getWait());
    }

    @Test
    public void cancelImageTransfer_qcow2UploadToStorageDriverExport_deletesTemporaryDeviceWithoutConverting() throws Exception {
        FinalizeImageTransferCommand finalizeCmd = finalizeQcow2UploadToStorageDriverExport(
                mock(PrimaryDataStoreDriver.class), true);

        assert finalizeCmd.getConvertSource() == null;
    }
}
