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
package org.apache.cloudstack.storage.vmsnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.api.ApiConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.alert.AlertManager;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.ResourceLimitService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

public class KvmFileBasedStorageVmSnapshotStrategyTest {

    private KvmFileBasedStorageVmSnapshotStrategy strategy;
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;
    private VMSnapshotDao vmSnapshotDao;
    private VMSnapshotHelper vmSnapshotHelper;
    private AgentManager agentMgr;
    private SnapshotDataStoreDao snapshotDataStoreDao;
    private HostDetailsDao hostDetailsDao;

    @Before
    public void setup() {
        strategy = Mockito.spy(new KvmFileBasedStorageVmSnapshotStrategy());
        vmSnapshotDetailsDao = mock(VMSnapshotDetailsDao.class);
        vmSnapshotDao = mock(VMSnapshotDao.class);
        vmSnapshotHelper = mock(VMSnapshotHelper.class);
        agentMgr = mock(AgentManager.class);
        snapshotDataStoreDao = mock(SnapshotDataStoreDao.class);
        hostDetailsDao = mock(HostDetailsDao.class);

        strategy.vmSnapshotDetailsDao = vmSnapshotDetailsDao;
        strategy.vmSnapshotDao = vmSnapshotDao;
        strategy.vmSnapshotHelper = vmSnapshotHelper;
        strategy.agentMgr = agentMgr;
        strategy.snapshotDataStoreDao = snapshotDataStoreDao;
        strategy.resourceLimitManager = mock(ResourceLimitService.class);
        strategy.snapshotDataFactory = mock(SnapshotDataFactory.class);
        strategy.userVmDao = mock(UserVmDao.class);
        strategy.volumeDao = mock(VolumeDao.class);
        strategy.vmInstanceDetailsDao = mock(VMInstanceDetailsDao.class);
        strategy.hostDetailsDao = hostDetailsDao;
        strategy.alertManager = mock(AlertManager.class);
        doNothing().when(strategy).publishUsageEvent(anyString(), any(VMSnapshot.class), any(UserVm.class), anyLong(), anyLong());
        doNothing().when(strategy).publishUsageEvent(anyString(), any(VMSnapshot.class), any(UserVm.class), any(VolumeObjectTO.class));
    }

    @Test
    public void testProcessCreateVmSnapshotAnswerPersistsNvramPath() throws Exception {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        UserVm userVm = mock(UserVm.class);
        VMSnapshotVO vmSnapshotVO = mock(VMSnapshotVO.class);

        when(vmSnapshot.getId()).thenReturn(42L);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(answer.getMapVolumeToSnapshotSizeAndNewVolumePath()).thenReturn(Collections.emptyMap());
        when(answer.getNvramSnapshotPath()).thenReturn("nvram/42.fd");

        Method method = KvmFileBasedStorageVmSnapshotStrategy.class.getDeclaredMethod("processCreateVmSnapshotAnswer", VMSnapshot.class, java.util.Map.class,
                CreateDiskOnlyVmSnapshotAnswer.class, UserVm.class, VMSnapshotVO.class, long.class, VMSnapshotVO.class);
        method.setAccessible(true);
        method.invoke(strategy, vmSnapshot, Collections.emptyMap(), answer, userVm, vmSnapshotVO, 0L, null);

        verify(vmSnapshotDetailsDao).addDetail(42L, "kvmFileBasedStorageSnapshotNvram", "nvram/42.fd", false);
    }

    @Test
    public void testRevertVMSnapshotPassesNvramPathToAgentCommand() {
        long vmId = 10L;
        long snapshotId = 20L;
        long dataStoreId = 30L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VMSnapshotVO currentVmSnapshot = mock(VMSnapshotVO.class);
        SnapshotDataStoreVO snapshotDataStoreVO = mock(SnapshotDataStoreVO.class);
        SnapshotInfo snapshotInfo = mock(SnapshotInfo.class);
        SnapshotObjectTO snapshotObjectTO = mock(SnapshotObjectTO.class);
        VMSnapshotDetailsVO volumeSnapshotDetail = new VMSnapshotDetailsVO(snapshotId, "kvmFileBasedStorageSnapshot", String.valueOf(snapshotId), true);
        VMSnapshotDetailsVO nvramDetail = new VMSnapshotDetailsVO(snapshotId, "kvmFileBasedStorageSnapshotNvram", "nvram/42.fd", false);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(vmSnapshot.getId()).thenReturn(snapshotId);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(userVm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(userVm.getName()).thenReturn("i-10-VM");
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(userVm.getId()).thenReturn(vmId);
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(strategy.vmInstanceDetailsDao.findDetail(vmId, ApiConstants.BootType.UEFI.toString()))
                .thenReturn(new VMInstanceDetailVO(vmId, ApiConstants.BootType.UEFI.toString(), "SECURE", true));
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_UEFI_ENABLE))
                .thenReturn(new DetailVO(hostId, Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString()));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM))
                .thenReturn(new DetailVO(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM, Boolean.TRUE.toString()));
        when(vmSnapshotDetailsDao.findDetails(snapshotId, "kvmFileBasedStorageSnapshot")).thenReturn(List.of(volumeSnapshotDetail));
        when(vmSnapshotDetailsDao.findDetail(snapshotId, "kvmFileBasedStorageSnapshotNvram")).thenReturn(nvramDetail);
        when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(snapshotId, DataStoreRole.Primary)).thenReturn(snapshotDataStoreVO);
        when(snapshotDataStoreVO.getSnapshotId()).thenReturn(snapshotId);
        when(snapshotDataStoreVO.getDataStoreId()).thenReturn(dataStoreId);
        when(strategy.snapshotDataFactory.getSnapshot(snapshotId, dataStoreId, DataStoreRole.Primary)).thenReturn(snapshotInfo);
        when(snapshotInfo.getTO()).thenReturn(snapshotObjectTO);
        when(vmSnapshotDao.findCurrentSnapshotByVmId(vmId)).thenReturn(currentVmSnapshot);
        when(agentMgr.easySend(eq(hostId), any())).thenAnswer(invocation ->
                new RevertDiskOnlyVmSnapshotAnswer((RevertDiskOnlyVmSnapshotCommand) invocation.getArgument(1), Collections.emptyList()));

        ArgumentCaptor<RevertDiskOnlyVmSnapshotCommand> commandCaptor = ArgumentCaptor.forClass(RevertDiskOnlyVmSnapshotCommand.class);

        strategy.revertVMSnapshot(vmSnapshot);

        verify(agentMgr).easySend(eq(hostId), commandCaptor.capture());
        verify(userVm).setLastHostId(hostId);
        verify(strategy.userVmDao).update(vmId, userVm);
        assertEquals("vm-uuid", commandCaptor.getValue().getVmUuid());
        assertEquals(true, commandCaptor.getValue().isUefiEnabled());
        assertEquals("nvram/42.fd", commandCaptor.getValue().getNvramSnapshotPath());
    }

    @Test
    public void testDeleteNvramSnapshotIfNeededPassesPrimaryDataStoreToAgentCommand() {
        long hostId = 40L;

        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VMSnapshotDetailsVO nvramDetail = new VMSnapshotDetailsVO(20L, "kvmFileBasedStorageSnapshotNvram", "nvram/42.fd", false);
        PrimaryDataStoreTO primaryDataStore = mock(PrimaryDataStoreTO.class);

        when(vmSnapshot.getId()).thenReturn(20L);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(vmSnapshotDetailsDao.findDetail(20L, "kvmFileBasedStorageSnapshotNvram")).thenReturn(nvramDetail);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM))
                .thenReturn(new DetailVO(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM, Boolean.TRUE.toString()));
        when(agentMgr.easySend(eq(hostId), any())).thenReturn(new Answer(null, true, null));

        strategy.deleteNvramSnapshotIfNeeded(vmSnapshot, hostId, primaryDataStore);

        verify(agentMgr).easySend(eq(hostId), argThat(command -> {
            if (!(command instanceof DeleteDiskOnlyVmSnapshotCommand)) {
                return false;
            }

            DeleteDiskOnlyVmSnapshotCommand deleteCommand = (DeleteDiskOnlyVmSnapshotCommand) command;
            return deleteCommand.getSnapshots().isEmpty()
                    && "nvram/42.fd".equals(deleteCommand.getNvramSnapshotPath())
                    && deleteCommand.getPrimaryDataStore() == primaryDataStore;
        }));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteNvramSnapshotIfNeededFailsWhenHostLacksNvramAwareCleanupCapability() {
        long hostId = 40L;

        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VMSnapshotDetailsVO nvramDetail = new VMSnapshotDetailsVO(20L, "kvmFileBasedStorageSnapshotNvram", "nvram/42.fd", false);
        PrimaryDataStoreTO primaryDataStore = mock(PrimaryDataStoreTO.class);

        when(vmSnapshot.getId()).thenReturn(20L);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(vmSnapshotDetailsDao.findDetail(20L, "kvmFileBasedStorageSnapshotNvram")).thenReturn(nvramDetail);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM)).thenReturn(null);

        try {
            strategy.deleteNvramSnapshotIfNeeded(vmSnapshot, hostId, primaryDataStore);
        } finally {
            verify(agentMgr, never()).easySend(eq(hostId), any());
        }
    }

    @Test
    public void testGetRootVolumePrimaryDataStoreForCleanupReturnsNullWhenRootVolumeIsMissing() {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VolumeObjectTO dataVolume = mock(VolumeObjectTO.class);

        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(dataVolume.getVolumeType()).thenReturn(Volume.Type.DATADISK);

        PrimaryDataStoreTO primaryDataStore = strategy.getRootVolumePrimaryDataStoreForCleanup(vmSnapshot, List.of(dataVolume));

        assertNull(primaryDataStore);
    }

    @Test
    public void testGetPrimaryDataStoreForNvramCleanupPrefersRootSnapshotPrimaryDataStore() {
        long vmSnapshotId = 20L;
        long rootSnapshotId = 30L;
        long dataStoreId = 40L;

        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VolumeObjectTO rootVolume = mock(VolumeObjectTO.class);
        PrimaryDataStoreTO liveRootVolumePrimaryDataStore = mock(PrimaryDataStoreTO.class);
        PrimaryDataStoreTO rootSnapshotPrimaryDataStore = mock(PrimaryDataStoreTO.class);
        SnapshotDataStoreVO rootSnapshotDataStore = mock(SnapshotDataStoreVO.class);
        SnapshotInfo rootSnapshotInfo = mock(SnapshotInfo.class);
        SnapshotObjectTO rootSnapshotObjectTo = mock(SnapshotObjectTO.class);
        VolumeObjectTO rootSnapshotVolume = mock(VolumeObjectTO.class);
        VMSnapshotDetailsVO volumeSnapshotDetail = new VMSnapshotDetailsVO(vmSnapshotId, "kvmFileBasedStorageSnapshot", String.valueOf(rootSnapshotId), true);

        when(vmSnapshot.getId()).thenReturn(vmSnapshotId);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(vmSnapshotDetailsDao.findDetails(vmSnapshotId, "kvmFileBasedStorageSnapshot")).thenReturn(List.of(volumeSnapshotDetail));
        when(snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(rootSnapshotId, DataStoreRole.Primary)).thenReturn(rootSnapshotDataStore);
        when(rootSnapshotDataStore.getSnapshotId()).thenReturn(rootSnapshotId);
        when(rootSnapshotDataStore.getDataStoreId()).thenReturn(dataStoreId);
        when(strategy.snapshotDataFactory.getSnapshot(rootSnapshotId, dataStoreId, DataStoreRole.Primary)).thenReturn(rootSnapshotInfo);
        when(rootSnapshotInfo.getTO()).thenReturn(rootSnapshotObjectTo);
        when(rootSnapshotObjectTo.getVolume()).thenReturn(rootSnapshotVolume);
        when(rootSnapshotVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(rootSnapshotObjectTo.getDataStore()).thenReturn(rootSnapshotPrimaryDataStore);
        when(rootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(rootVolume.getDataStore()).thenReturn(liveRootVolumePrimaryDataStore);

        PrimaryDataStoreTO primaryDataStore = strategy.getPrimaryDataStoreForNvramCleanup(vmSnapshot, List.of(rootVolume));

        assertSame(rootSnapshotPrimaryDataStore, primaryDataStore);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testTakeVmSnapshotInternalFailsWhenHostLacksNvramAwareSnapshotCapabilityForUefiVm() throws Exception {
        long vmId = 10L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(userVm.getId()).thenReturn(vmId);
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(strategy.vmInstanceDetailsDao.findDetail(vmId, ApiConstants.BootType.UEFI.toString()))
                .thenReturn(new VMInstanceDetailVO(vmId, ApiConstants.BootType.UEFI.toString(), "SECURE", true));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_UEFI_ENABLE))
                .thenReturn(new DetailVO(hostId, Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString()));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM)).thenReturn(null);

        strategy.takeVmSnapshotInternal(vmSnapshot, Collections.emptyMap());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteVMSnapshotFailsWhenHostLacksNvramAwareCleanupCapabilityForSidecarSnapshot() {
        long vmId = 10L;
        long vmSnapshotId = 20L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        VMSnapshotDetailsVO nvramDetail = new VMSnapshotDetailsVO(vmSnapshotId, "kvmFileBasedStorageSnapshotNvram", "nvram/42.fd", false);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(vmSnapshot.getId()).thenReturn(vmSnapshotId);
        when(vmSnapshot.getUuid()).thenReturn("vm-snapshot");
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(vmSnapshotDetailsDao.findDetail(vmSnapshotId, "kvmFileBasedStorageSnapshotNvram")).thenReturn(nvramDetail);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM)).thenReturn(null);

        strategy.deleteVMSnapshot(vmSnapshot);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testTakeVmSnapshotInternalFailsWhenHostLacksUefiCapabilityForUefiVm() throws Exception {
        long vmId = 10L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(userVm.getId()).thenReturn(vmId);
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(strategy.vmInstanceDetailsDao.findDetail(vmId, ApiConstants.BootType.UEFI.toString()))
                .thenReturn(new VMInstanceDetailVO(vmId, ApiConstants.BootType.UEFI.toString(), "SECURE", true));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_UEFI_ENABLE)).thenReturn(null);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM))
                .thenReturn(new DetailVO(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM, Boolean.TRUE.toString()));

        strategy.takeVmSnapshotInternal(vmSnapshot, Collections.emptyMap());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRevertVMSnapshotFailsWhenHostLacksNvramAwareSnapshotCapabilityForUefiVm() {
        long vmId = 10L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(userVm.getId()).thenReturn(vmId);
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(userVm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(strategy.vmInstanceDetailsDao.findDetail(vmId, ApiConstants.BootType.UEFI.toString()))
                .thenReturn(new VMInstanceDetailVO(vmId, ApiConstants.BootType.UEFI.toString(), "SECURE", true));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_UEFI_ENABLE))
                .thenReturn(new DetailVO(hostId, Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString()));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM)).thenReturn(null);

        strategy.revertVMSnapshot(vmSnapshot);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRevertVMSnapshotFailsWhenHostLacksUefiCapabilityForUefiVm() {
        long vmId = 10L;
        long hostId = 40L;

        UserVmVO userVm = mock(UserVmVO.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);

        when(vmSnapshot.getVmId()).thenReturn(vmId);
        when(userVm.getId()).thenReturn(vmId);
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(userVm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(strategy.userVmDao.findById(vmId)).thenReturn(userVm);
        when(vmSnapshotHelper.pickRunningHost(vmId)).thenReturn(hostId);
        when(strategy.vmInstanceDetailsDao.findDetail(vmId, ApiConstants.BootType.UEFI.toString()))
                .thenReturn(new VMInstanceDetailVO(vmId, ApiConstants.BootType.UEFI.toString(), "SECURE", true));
        when(hostDetailsDao.findDetail(hostId, Host.HOST_UEFI_ENABLE)).thenReturn(null);
        when(hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM))
                .thenReturn(new DetailVO(hostId, Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM, Boolean.TRUE.toString()));

        strategy.revertVMSnapshot(vmSnapshot);
    }

    @Test
    public void testNotifyGuestRecoveryIssueIfNeededSendsAlert() {
        CreateDiskOnlyVmSnapshotAnswer answer = mock(CreateDiskOnlyVmSnapshotAnswer.class);
        UserVm userVm = mock(UserVm.class);
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);

        when(answer.getDetails()).thenReturn("VM could not be thawed");
        when(userVm.getUuid()).thenReturn("vm-uuid");
        when(userVm.getDataCenterId()).thenReturn(1L);
        when(userVm.getPodIdToDeployIn()).thenReturn(2L);
        when(vmSnapshot.getUuid()).thenReturn("snapshot-uuid");

        strategy.notifyGuestRecoveryIssueIfNeeded(answer, userVm, vmSnapshot);

        verify(strategy.alertManager).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_VM_SNAPSHOT), eq(1L), eq(2L), anyString(), anyString());
    }
}
