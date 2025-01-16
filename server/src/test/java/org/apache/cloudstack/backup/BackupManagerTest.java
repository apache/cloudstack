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

import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    DiskOfferingDao diskOfferingDao;

    @Mock
    ServiceOfferingDao serviceOfferingDao;

    @Mock
    VMTemplateDao vmTemplateDao;

    @Mock
    UserVmJoinDao userVmJoinDao;

    private String[] hostPossibleValues = {"127.0.0.1", "hostname"};
    private String[] datastoresPossibleValues = {"e9804933-8609-4de3-bccc-6278072a496c", "datastore-name"};
    private AutoCloseable closeable;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        when(backupOfferingDao.findById(null)).thenReturn(null);
        when(backupOfferingDao.findById(123l)).thenReturn(null);

        BackupOfferingVO offering = Mockito.spy(BackupOfferingVO.class);
        when(offering.getName()).thenCallRealMethod();
        when(offering.getDescription()).thenCallRealMethod();
        when(offering.isUserDrivenBackupAllowed()).thenCallRealMethod();

        BackupOfferingVO offeringUpdate = Mockito.spy(BackupOfferingVO.class);

        when(backupOfferingDao.findById(1234l)).thenReturn(offering);
        when(backupOfferingDao.createForUpdate(1234l)).thenReturn(offeringUpdate);
        when(backupOfferingDao.update(1234l, offeringUpdate)).thenAnswer(answer -> {
            offering.setName("New name");
            offering.setDescription("New description");
            offering.setUserDrivenBackupAllowed(true);
            return true;
        });
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
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
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(1)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);
        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("datastore-name"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success2"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success2", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(2)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostNameAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState) )).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success3"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success3", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(3)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("datastore-name"),  Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success4"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success4", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(4)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
    }

    @Test
    public void tryRestoreVMTestRestoreSucceeded() throws NoTransitionException {
        BackupOffering offering = mock(BackupOffering.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        BackupVO backup = mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

            Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);

            Mockito.when(vm.getId()).thenReturn(1L);
            Mockito.when(offering.getProvider()).thenReturn("veeam");
            Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
            Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(true);

            backupManager.tryRestoreVM(backup, vm, offering, "Nothing to write here.");
        }
    }

    @Test
    public void tryRestoreVMTestRestoreFails() throws NoTransitionException {
        BackupOffering offering = mock(BackupOffering.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        BackupVO backup = mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

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

    @Test
    public void testGetVmDetailsForBackup() {
        Long vmId = 1L;
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        when(vm.getTemplateId()).thenReturn(1L);
        when(vm.getId()).thenReturn(vmId);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getUuid()).thenReturn("service-offering-uuid");
        when(serviceOfferingDao.findById(1L)).thenReturn(serviceOffering);

        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getUuid()).thenReturn("template-uuid");
        when(vmTemplateDao.findById(1L)).thenReturn(template);

        UserVmJoinVO userVmJoinVO = mock(UserVmJoinVO.class);
        when(userVmJoinVO.getNetworkUuid()).thenReturn("mocked-network-uuid");
        List<UserVmJoinVO> userVmJoinVOs = Collections.singletonList(userVmJoinVO);
        when(userVmJoinDao.searchByIds(vmId)).thenReturn(userVmJoinVOs);

        Map<String, String> details = backupManager.getVmDetailsForBackup(vm);

        assertEquals("KVM", details.get(ApiConstants.HYPERVISOR));
        assertEquals("service-offering-uuid", details.get(ApiConstants.SERVICE_OFFERING_ID));
        assertEquals("template-uuid", details.get(ApiConstants.TEMPLATE_ID));
        assertEquals("mocked-network-uuid", details.get(ApiConstants.NETWORK_IDS));
    }

    @Test
    public void testGetDiskOfferingDetailsForBackup() {
        Long vmId = 1L;
        VolumeVO volume = new VolumeVO(Volume.Type.DATADISK, null, 0, 0, 0, 0, null, 1024L, 100L, 1000L, null);
        volume.setDiskOfferingId(1L);
        volume.setSize(1024L);
        volume.setDeviceId(0L);
        volume.setMinIops(100L);
        volume.setMaxIops(200L);
        when(volumeDao.findByInstance(vmId)).thenReturn(Collections.singletonList(volume));

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid");
        when(diskOfferingDao.findById(1L)).thenReturn(diskOffering);

        Map<String, String> details = backupManager.getDiskOfferingDetailsForBackup(vmId);

        assertEquals("disk-offering-uuid", details.get(ApiConstants.DISK_OFFERING_IDS));
        assertEquals("1024", details.get(ApiConstants.DISK_SIZES));
        assertEquals("100", details.get(ApiConstants.MIN_IOPS));
        assertEquals("200", details.get(ApiConstants.MAX_IOPS));
        assertEquals("0", details.get(ApiConstants.DEVICE_IDS));
    }

    @Test
    public void getDataDiskOfferingListFromBackup() {
        Long size1 = 5L * 1024 * 1024 * 1024;
        Long size2 = 10L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("root-disk-offering-uuid,disk-offering-uuid-1,disk-offering-uuid-2");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("0,1,2");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("0," + size1 + "," + size2);
        when(backup.getDetail(ApiConstants.MIN_IOPS)).thenReturn("0,100,200");
        when(backup.getDetail(ApiConstants.MAX_IOPS)).thenReturn("0,300,400");

        DiskOfferingVO rootDiskOffering = mock(DiskOfferingVO.class);

        DiskOfferingVO diskOffering1 = mock(DiskOfferingVO.class);
        when(diskOffering1.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering1.isCustomizedIops()).thenReturn(true);

        DiskOfferingVO diskOffering2 = mock(DiskOfferingVO.class);
        when(diskOffering2.getUuid()).thenReturn("disk-offering-uuid-2");
        when(diskOffering2.isCustomizedIops()).thenReturn(true);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering1);
        when(diskOfferingDao.findByUuid("disk-offering-uuid-2")).thenReturn(diskOffering2);

        List<DiskOfferingInfo> diskOfferingInfoList = backupManager.getDataDiskOfferingListFromBackup(backup);

        assertEquals(2, diskOfferingInfoList.size());
        assertEquals("disk-offering-uuid-1", diskOfferingInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), diskOfferingInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), diskOfferingInfoList.get(0).getDeviceId());
        assertEquals(Long.valueOf(100), diskOfferingInfoList.get(0).getMinIops());
        assertEquals(Long.valueOf(300), diskOfferingInfoList.get(0).getMaxIops());

        assertEquals("disk-offering-uuid-2", diskOfferingInfoList.get(1).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(10), diskOfferingInfoList.get(1).getSize());
        assertEquals(Long.valueOf(2), diskOfferingInfoList.get(1).getDeviceId());
        assertEquals(Long.valueOf(200), diskOfferingInfoList.get(1).getMinIops());
        assertEquals(Long.valueOf(400), diskOfferingInfoList.get(1).getMaxIops());
    }

    @Test
    public void getDataDiskOfferingListFromBackupNullIops() {
        Long size = 5L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("disk-offering-uuid-1");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("1");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("" + size);
        when(backup.getDetail(ApiConstants.MIN_IOPS)).thenReturn("null");
        when(backup.getDetail(ApiConstants.MAX_IOPS)).thenReturn("null");

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering.isCustomizedIops()).thenReturn(true);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering);

        List<DiskOfferingInfo> diskOfferingInfoList = backupManager.getDataDiskOfferingListFromBackup(backup);

        assertEquals(1, diskOfferingInfoList.size());
        assertEquals("disk-offering-uuid-1", diskOfferingInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), diskOfferingInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), diskOfferingInfoList.get(0).getDeviceId());
        assertNull(diskOfferingInfoList.get(0).getMinIops());
        assertNull(diskOfferingInfoList.get(0).getMaxIops());
    }
}
