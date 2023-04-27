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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;

import java.util.Collections;

public class BackupManagerTest {
    @Spy
    @InjectMocks
    BackupManagerImpl backupManager = new BackupManagerImpl();

    @Mock
    BackupOfferingDao backupOfferingDao;

    @Mock
    BackupProvider backupProvider;

    @Mock
    VirtualMachineManager virtualMachineManager;

    @Mock
    VolumeApiService volumeApiService;

    @Mock
    VolumeDao volumeDao;

    private String[] hostPossibleValues = {"127.0.0.1", "hostname"};
    private String[] datastoresPossibleValues = {"e9804933-8609-4de3-bccc-6278072a496c", "datastore-name"};

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(backupOfferingDao.findById(null)).thenReturn(null);
        when(backupOfferingDao.findById(123l)).thenReturn(null);

        BackupOfferingVO offering = Mockito.spy(BackupOfferingVO.class);
        when(offering.getId()).thenReturn(1234l);
        when(offering.getName()).thenCallRealMethod();
        when(offering.getDescription()).thenCallRealMethod();
        when(offering.isUserDrivenBackupAllowed()).thenCallRealMethod();

        BackupOfferingVO offeringUpdate = Mockito.spy(BackupOfferingVO.class);
        when(offeringUpdate.getId()).thenReturn(1234l);
        when(offeringUpdate.getName()).thenReturn("Old name");
        when(offeringUpdate.getDescription()).thenReturn("Old description");

        when(backupOfferingDao.findById(1234l)).thenReturn(offering);
        when(backupOfferingDao.createForUpdate(1234l)).thenReturn(offeringUpdate);
        when(backupOfferingDao.update(1234l, offeringUpdate)).thenAnswer(answer -> {
            offering.setName("New name");
            offering.setDescription("New description");
            offering.setUserDrivenBackupAllowed(true);
            return true;
        });
    }

    @Test
    public void testExceptionWhenUpdateWithNullId() {
        try {
            Long id = null;

            UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
            when(cmd.getId()).thenReturn(id);

            backupManager.updateBackupOffering(cmd);
        } catch (InvalidParameterValueException e) {
            assertEquals("Unable to find Backup Offering with id: [null].", e.getMessage());
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void testExceptionWhenUpdateWithNonExistentId() {
        Long id = 123l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);

        backupManager.updateBackupOffering(cmd);
    }

    @Test (expected = ServerApiException.class)
    public void testExceptionWhenUpdateWithoutChanges() {
        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getDescription()).thenReturn(null);
        when(cmd.getAllowUserDrivenBackups()).thenReturn(null);

        Mockito.doCallRealMethod().when(cmd).execute();

        cmd.execute();
    }

    @Test
    public void testUpdateBackupOfferingSuccess() {
        Long id = 1234l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getName()).thenReturn("New name");
        when(cmd.getDescription()).thenReturn("New description");
        when(cmd.getAllowUserDrivenBackups()).thenReturn(true);

        BackupOffering updated = backupManager.updateBackupOffering(cmd);
        assertEquals("New name", updated.getName());
        assertEquals("New description", updated.getDescription());
        assertEquals(true, updated.isUserDrivenBackupAllowed());
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(1)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("datastore-name"))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success2"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success2", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(2)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void restoreBackedUpVolumeTestHostNameAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success3"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success3", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(3)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void restoreBackedUpVolumeTestHostAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("datastore-name"))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success4"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success4", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(4)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void tryRestoreVMTestRestoreSucceeded() throws NoTransitionException {
        BackupOffering offering = Mockito.mock(BackupOffering.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        BackupVO backup = Mockito.mock(BackupVO.class);

        Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
        Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
        Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);



        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(offering.getProvider()).thenReturn("veeam");
        Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
        Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(true);

        backupManager.tryRestoreVM(backup, vm, offering, "Nothing to write here.");
    }

    @Test
    public void tryRestoreVMTestRestoreFails() throws NoTransitionException {
        BackupOffering offering = Mockito.mock(BackupOffering.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        BackupVO backup = Mockito.mock(BackupVO.class);

        Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
        Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
        Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);
        Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringFailed), Mockito.any())).thenReturn(true);
        Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreFailed))).thenReturn(true);

        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(offering.getProvider()).thenReturn("veeam");
        Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
        Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(false);
        try {
            backupManager.tryRestoreVM(backup, vm, offering, "Checking message error.");
            fail("An exception is needed.");
        } catch (CloudRuntimeException e) {
            assertEquals("Error restoring VM from backup [Checking message error.].", e.getMessage());
        }
    }
}
