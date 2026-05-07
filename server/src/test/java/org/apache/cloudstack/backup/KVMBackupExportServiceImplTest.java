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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
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
    VirtualMachineManager virtualMachineManager;

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
}
