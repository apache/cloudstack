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

import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.InternalBackupDataStoreDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.apache.cloudstack.backup.dao.InternalBackupStoragePoolDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.KbossTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KbossBackupProviderTest {

    @Mock
    private VirtualMachine virtualMachineMock;

    @Mock
    private BackupOffering backupOfferingMock;

    @Mock
    private VolumeDao volumeDaoMock;

    @Mock
    private VolumeVO volumeVoMock;

    @Mock
    private SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    private SnapshotDataStoreVO snapshotDataStoreVoMock;

    @Mock
    private VMSnapshotDao vmSnapshotDaoMock;

    @Mock
    private VMSnapshotVO vmSnapshotVoMock;

    @Mock
    private VMSnapshotDetailsDao vmSnapshotDetailsDaoMock;

    @Mock
    private VMSnapshotDetailsVO vmSnapshotDetailsVoMock;

    @Mock
    private InternalBackupJoinDao internalBackupJoinDaoMock;

    @Mock
    private InternalBackupJoinVO internalBackupJoinVoMock;

    @Mock
    private InternalBackupDataStoreDao internalBackupDataStoreDaoMock;

    @Mock
    private InternalBackupDataStoreVO internalBackupDataStoreVoMock;

    @Mock
    private InternalBackupStoragePoolDao internalBackupStoragePoolDaoMock;

    @Mock
    private BackupVO backupVoMock;

    @Mock
    private BackupDetailsDao backupDetailDaoMock;

    @Mock
    private ConfigKey<Integer> backupChainSize;

    @Mock
    private DataStoreManager dataStoreManagerMock;

    @Mock
    private DataStore dataStoreMock;

    @Mock
    private HeuristicRuleHelper heuristicRuleHelperMock;

    @Mock
    private VMSnapshotHelper vmSnapshotHelperMock;

    @Mock
    private BackupDao backupDaoMock;

    @Mock
    private VolumeObjectTO volumeObjectToMock;

    @Mock
    private VirtualMachineManager virtualMachineManagerMock;

    @Mock
    private HostDao hostDaoMock;

    @Mock
    private HostVO hostVOMock;

    @Spy
    @InjectMocks
    private KbossBackupProvider kbossBackupProviderSpy;

    private long vmId = 319832;
    private long volumeId = 41;

    @Before
    public void setup() {
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(vmId).when(backupVoMock).getVmId();
        doReturn(vmId).when(internalBackupJoinVoMock).getVmId();
    }


    @Test
    public void assignVMToBackupOfferingTestNotKvm() {
        doReturn(Hypervisor.HypervisorType.Any).when(virtualMachineMock).getHypervisorType();
        boolean result = kbossBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
        assertFalse(result);
    }

    @Test
    public void assignVMToBackupOfferingTestKvmWithUnsupportedDiskOnlyVmSnapshot() {
        doReturn(Hypervisor.HypervisorType.KVM).when(virtualMachineMock).getHypervisorType();
        doReturn(List.of(vmSnapshotVoMock)).when(vmSnapshotDaoMock).findByVmAndByType(vmId, VMSnapshot.Type.Disk);
        long vmSnapId = 921;
        doReturn(vmSnapId).when(vmSnapshotVoMock).getId();
        doReturn(List.of(vmSnapshotDetailsVoMock)).when(vmSnapshotDetailsDaoMock).listDetails(vmSnapId);
        doReturn("Anything").when(vmSnapshotDetailsVoMock).getName();

        boolean result = kbossBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
        assertFalse(result);
    }

    @Test
    public void assignVMToBackupOfferingTestKvmWithSupportedDiskOnlyVmSnapshotAndDiskAndMemoryVmSnapshot() {
        doReturn(Hypervisor.HypervisorType.KVM).when(virtualMachineMock).getHypervisorType();
        doReturn(List.of(vmSnapshotVoMock)).when(vmSnapshotDaoMock).findByVmAndByType(vmId, VMSnapshot.Type.Disk);
        long vmSnapId = 921;
        doReturn(vmSnapId).when(vmSnapshotVoMock).getId();
        doReturn(List.of(vmSnapshotDetailsVoMock)).when(vmSnapshotDetailsDaoMock).listDetails(vmSnapId);
        doReturn(VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT).when(vmSnapshotDetailsVoMock).getName();
        doReturn(List.of(vmSnapshotVoMock)).when(vmSnapshotDaoMock).findByVmAndByType(vmId, VMSnapshot.Type.DiskAndMemory);

        boolean result = kbossBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
        assertFalse(result);
    }


    @Test
    public void assignVMToBackupOfferingTestKvmWithSupportedDiskOnlyVmSnapshotAndNoDiskAndMemoryVmSnapshot() {
        doReturn(Hypervisor.HypervisorType.KVM).when(virtualMachineMock).getHypervisorType();
        doReturn(List.of(vmSnapshotVoMock)).when(vmSnapshotDaoMock).findByVmAndByType(vmId, VMSnapshot.Type.Disk);
        long vmSnapId = 921;
        doReturn(vmSnapId).when(vmSnapshotVoMock).getId();
        doReturn(List.of(vmSnapshotDetailsVoMock)).when(vmSnapshotDetailsDaoMock).listDetails(vmSnapId);
        doReturn(VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT).when(vmSnapshotDetailsVoMock).getName();

        boolean result = kbossBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestNoActiveChain() {
        doReturn(VirtualMachine.State.Running).when(virtualMachineMock).getState();

        boolean result = kbossBackupProviderSpy.removeVMFromBackupOffering(virtualMachineMock);

        verify(kbossBackupProviderSpy, Mockito.never()).mergeCurrentBackupDeltas(Mockito.any());
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestWithActiveChain() {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(Mockito.any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();

        boolean result = kbossBackupProviderSpy.removeVMFromBackupOffering(virtualMachineMock);

        verify(kbossBackupProviderSpy, Mockito.times(1)).mergeCurrentBackupDeltas(Mockito.any());
        assertTrue(result);
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedEmptyList() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        doReturn(new ArrayList<>()).when(internalBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<InternalBackupJoinVO> result = kbossBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorIsEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        doReturn(true).when(internalBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(internalBackupJoinVoMock)).when(internalBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<InternalBackupJoinVO> result = kbossBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorMultipleAncestors() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock1).getEndOfChain();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock2).getEndOfChain();
        doReturn(true).when(internalBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2, internalBackupJoinVoMock)).when(internalBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<InternalBackupJoinVO> result = kbossBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertEquals(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2), result);
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorMultipleAncestorsNoEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock1).getEndOfChain();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock2).getEndOfChain();
        doReturn(false).when(internalBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2, internalBackupJoinVoMock)).when(internalBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<InternalBackupJoinVO> result = kbossBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertEquals(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2, internalBackupJoinVoMock), result);
    }

    @Test
    public void getBackupJoinParentsTestNoRemovedAncestorMultipleAncestorsNoEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock1).getEndOfChain();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(false).when(internalBackupJoinVoMock2).getEndOfChain();
        doReturn(false).when(internalBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2, internalBackupJoinVoMock)).when(internalBackupJoinDaoMock).listByBackedUpAndVmIdAndDateBeforeOrAfterOrderBy(vmId, date, true,
                false);

        List<InternalBackupJoinVO> result = kbossBackupProviderSpy.getBackupJoinParents(backupVoMock, false);

        assertEquals(List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2, internalBackupJoinVoMock), result);
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeLowerThanOneAndConfigIsZero() {
        int chainSize = 0;
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(-1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.never()).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeLowerThanOneAndConfigBiggerThanZero() {
        int chainSize = 1;
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(-1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeBiggerThanOne() {
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(2, 0, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.never()).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeIsOne() {
        int chainSize = 2;
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsEmpty() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(), null);

        assertNull(result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsBiggerThanChainSize() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2), null);

        assertEquals(internalBackupJoinVoMock1, result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsSmallerThanChainSize() {
        int chainSize = 3;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2), null);

        assertEquals(internalBackupJoinVoMock1, result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsNotEmptyParentIsRemoved() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.Removed).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1), null);

        assertNull(result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getImageStoreForBackupTestNoHeuristic() {
        long zoneId = 2;
        doReturn(null).when(heuristicRuleHelperMock).getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicType.BACKUP, backupVoMock);
        doReturn(dataStoreMock).when(dataStoreManagerMock).getImageStoreWithFreeCapacity(zoneId);

        DataStore result = kbossBackupProviderSpy.getImageStoreForBackup(zoneId, backupVoMock);

        assertEquals(dataStoreMock, result);
    }

    @Test
    public void getImageStoreForBackupTestWithHeuristic() {
        long zoneId = 2;
        doReturn(dataStoreMock).when(heuristicRuleHelperMock).getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicType.BACKUP, backupVoMock);

        DataStore result = kbossBackupProviderSpy.getImageStoreForBackup(zoneId, backupVoMock);

        assertEquals(dataStoreMock, result);
        verify(dataStoreManagerMock, Mockito.never()).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }

    @Test (expected = CloudRuntimeException.class)
    public void getImageStoreForBackupTestNoStorageFound() {
        kbossBackupProviderSpy.getImageStoreForBackup(0L, backupVoMock);
    }

    @Test
    public void getSucceedingVmSnapshotListTestBackupIsNull() {
        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestNoCurrentSnapshotVo() {
        doReturn(null).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);

        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(internalBackupJoinVoMock);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestCurrentCreatedBeforeBackup() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime()-10000);
        Date now = DateUtil.now();
        doReturn(before).when(vmSnapshotVoMock).getCreated();
        doReturn(now).when(internalBackupJoinVoMock).getDate();

        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(internalBackupJoinVoMock);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestCurrentVmSnapshotHasNoParent() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime()-10000);
        Date now = DateUtil.now();
        doReturn(now).when(vmSnapshotVoMock).getCreated();
        doReturn(before).when(internalBackupJoinVoMock).getDate();

        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(internalBackupJoinVoMock);

        assertEquals(1, result.size());
        assertEquals(vmSnapshotVoMock, result.get(0));
    }

    @Test
    public void getSucceedingVmSnapshotListTestCurrentVmSnapshotHasParentsCreatedAfter() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime()-10000);
        Date now = DateUtil.now();
        doReturn(now).when(vmSnapshotVoMock).getCreated();
        doReturn(before).when(internalBackupJoinVoMock).getDate();
        long snapParentId = 909;
        doReturn(snapParentId).when(vmSnapshotVoMock).getParent();
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        doReturn(now).when(vmSnapshotVoMock1).getCreated();
        doReturn(vmSnapshotVoMock1).when(vmSnapshotDaoMock).findById(snapParentId);

        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(internalBackupJoinVoMock);

        assertEquals(List.of(vmSnapshotVoMock1, vmSnapshotVoMock), result);
    }


    @Test
    public void getSucceedingVmSnapshotListTestCurrentVmSnapshotHasParentsCreatedBefore() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime() - 10000);
        Date now = DateUtil.now();
        doReturn(now).when(vmSnapshotVoMock).getCreated();
        doReturn(before).when(internalBackupJoinVoMock).getDate();
        long snapParentId = 909;
        doReturn(snapParentId).when(vmSnapshotVoMock).getParent();
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        Date evenBefore = new Date(before.getTime() - 10000);
        doReturn(evenBefore).when(vmSnapshotVoMock1).getCreated();
        doReturn(vmSnapshotVoMock1).when(vmSnapshotDaoMock).findById(snapParentId);

        List<VMSnapshotVO> result = kbossBackupProviderSpy.getSucceedingVmSnapshotList(internalBackupJoinVoMock);

        assertEquals(List.of(vmSnapshotVoMock), result);
    }

    @Test
    public void mapVolumesToVmSnapshotReferencesTestVmSnapshotVOListIsEmpty() {
        kbossBackupProviderSpy.mapVolumesToVmSnapshotReferences(List.of(), List.of());

        verify(vmSnapshotHelperMock, Mockito.never()).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(1);
    }

    @Test
    public void mapVolumesToVmSnapshotReferencesTestVmSnapshotVOListHasTwoElements() {
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        doReturn(1L).when(vmSnapshotVoMock).getId();
        doReturn(2L).when(vmSnapshotVoMock1).getId();
        doNothing().when(kbossBackupProviderSpy).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());

        kbossBackupProviderSpy.mapVolumesToVmSnapshotReferences(List.of(), List.of(vmSnapshotVoMock, vmSnapshotVoMock1));

        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(1);
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(2);
        verify(kbossBackupProviderSpy, times(1)).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());
    }

    @Test
    public void createDeltaReferencesTestFullBackupEndOfChain() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(Mockito.any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void createDeltaReferencesTestIsolatedBackup() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(Mockito.any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
        verify(kbossBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(Mockito.any(), Mockito.any(), Mockito.any());
        verify(kbossBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(Mockito.any(), Mockito.any(), Mockito.any());
        verify(internalBackupStoragePoolDaoMock, Mockito.times(0)).persist(Mockito.any());
    }

    @Test
    public void createDeltaReferencesTestNotFullBackupEndOfChain() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(Mockito.any());
        KbossTO kbossTO = new KbossTO(volumeObjectToMock, List.of());
        doReturn(null).when(kbossBackupProviderSpy).createDeltaMergeTreeForVolume(false, true, List.of(), null, kbossTO);
        doNothing().when(kbossBackupProviderSpy).findAndSetParentBackupPath(List.of(), null, kbossTO);

        kbossBackupProviderSpy.createDeltaReferences(false, true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, kbossTO);

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).findAndSetParentBackupPath(List.of(), null, kbossTO);
    }

    @Test
    public void createDeltaReferencesTestFullBackupNotEndOfChainDoesNotHaveVmSnapshotSucceedingLastBackup() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(Mockito.any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                false, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void orchestrateTakeBackupTestHostIsDownReturnFalse() {

        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Down);

        Pair<Boolean, Long> result = kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertFalse(result.first());
    }

    @Test
    public void orchestrateTakeBackupTestHostIsDisconnectedReturnFalse() {

        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Disconnected);

        Pair<Boolean, Long> result = kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertFalse(result.first());
    }

    @Test
    public void setBackupAsIsolatedTestPersistIsolatedDetail() {
        kbossBackupProviderSpy.setBackupAsIsolated(backupVoMock);
        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }
}
