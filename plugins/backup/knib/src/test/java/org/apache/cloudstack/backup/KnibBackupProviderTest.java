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
import org.apache.cloudstack.backup.dao.NativeBackupDataStoreDao;
import org.apache.cloudstack.backup.dao.NativeBackupJoinDao;
import org.apache.cloudstack.backup.dao.NativeBackupStoragePoolDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.KnibTO;
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
public class KnibBackupProviderTest {

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
    private NativeBackupJoinDao nativeBackupJoinDaoMock;

    @Mock
    private NativeBackupJoinVO nativeBackupJoinVoMock;

    @Mock
    private NativeBackupDataStoreDao nativeBackupDataStoreDaoMock;

    @Mock
    private NativeBackupDataStoreVO nativeBackupDataStoreVoMock;

    @Mock
    private NativeBackupStoragePoolDao nativeBackupStoragePoolDaoMock;

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
    private KnibBackupProvider knibBackupProviderSpy;

    private long vmId = 319832;
    private long volumeId = 41;

    @Before
    public void setup() {
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(vmId).when(backupVoMock).getVmId();
        doReturn(vmId).when(nativeBackupJoinVoMock).getVmId();
    }


    @Test
    public void assignVMToBackupOfferingTestNotKvm() {
        doReturn(Hypervisor.HypervisorType.Any).when(virtualMachineMock).getHypervisorType();
        boolean result = knibBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
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

        boolean result = knibBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
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

        boolean result = knibBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
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

        boolean result = knibBackupProviderSpy.assignVMToBackupOffering(virtualMachineMock, backupOfferingMock);
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestNoActiveChain() {
        doReturn(VirtualMachine.State.Running).when(virtualMachineMock).getState();

        boolean result = knibBackupProviderSpy.removeVMFromBackupOffering(virtualMachineMock);

        verify(knibBackupProviderSpy, Mockito.never()).mergeCurrentBackupDeltas(Mockito.any());
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestWithActiveChain() {
        doReturn(nativeBackupJoinVoMock).when(nativeBackupJoinDaoMock).findCurrent(vmId);
        doReturn(true).when(knibBackupProviderSpy).mergeCurrentBackupDeltas(Mockito.any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();

        boolean result = knibBackupProviderSpy.removeVMFromBackupOffering(virtualMachineMock);

        verify(knibBackupProviderSpy, Mockito.times(1)).mergeCurrentBackupDeltas(Mockito.any());
        assertTrue(result);
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedEmptyList() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        doReturn(new ArrayList<>()).when(nativeBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<NativeBackupJoinVO> result = knibBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorIsEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        doReturn(true).when(nativeBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(nativeBackupJoinVoMock)).when(nativeBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<NativeBackupJoinVO> result = knibBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorMultipleAncestors() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock1).getEndOfChain();
        NativeBackupJoinVO nativeBackupJoinVoMock2 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock2).getEndOfChain();
        doReturn(true).when(nativeBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2, nativeBackupJoinVoMock)).when(nativeBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<NativeBackupJoinVO> result = knibBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertEquals(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2), result);
    }

    @Test
    public void getBackupJoinParentsTestIncludeRemovedAncestorMultipleAncestorsNoEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock1).getEndOfChain();
        NativeBackupJoinVO nativeBackupJoinVoMock2 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock2).getEndOfChain();
        doReturn(false).when(nativeBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2, nativeBackupJoinVoMock)).when(nativeBackupJoinDaoMock).listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(vmId, date);

        List<NativeBackupJoinVO> result = knibBackupProviderSpy.getBackupJoinParents(backupVoMock, true);

        assertEquals(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2, nativeBackupJoinVoMock), result);
    }

    @Test
    public void getBackupJoinParentsTestNoRemovedAncestorMultipleAncestorsNoEndOfChain() {
        Date date = DateUtil.now();
        doReturn(date).when(backupVoMock).getDate();
        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock1).getEndOfChain();
        NativeBackupJoinVO nativeBackupJoinVoMock2 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(false).when(nativeBackupJoinVoMock2).getEndOfChain();
        doReturn(false).when(nativeBackupJoinVoMock).getEndOfChain();
        doReturn(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2, nativeBackupJoinVoMock)).when(nativeBackupJoinDaoMock).listByBackedUpAndVmIdAndDateBeforeOrAfterOrderBy(vmId, date, true,
                false);

        List<NativeBackupJoinVO> result = knibBackupProviderSpy.getBackupJoinParents(backupVoMock, false);

        assertEquals(List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2, nativeBackupJoinVoMock), result);
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeLowerThanOneAndConfigIsZero() {
        int chainSize = 0;
        knibBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(-1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.never()).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeLowerThanOneAndConfigBiggerThanZero() {
        int chainSize = 1;
        knibBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(-1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeBiggerThanOne() {
        knibBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(2, 0, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.never()).persist(Mockito.any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeIsOne() {
        int chainSize = 2;
        knibBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsEmpty() {
        int chainSize = 2;
        doReturn(chainSize).when(knibBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(knibBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        NativeBackupJoinVO result = knibBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(), null);

        assertNull(result);
        verify(knibBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsBiggerThanChainSize() {
        int chainSize = 2;
        doReturn(chainSize).when(knibBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(knibBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(nativeBackupJoinVoMock1).getStatus();
        NativeBackupJoinVO nativeBackupJoinVoMock2 = Mockito.mock(NativeBackupJoinVO.class);
        NativeBackupJoinVO result = knibBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2), null);

        assertEquals(nativeBackupJoinVoMock1, result);
        verify(knibBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsSmallerThanChainSize() {
        int chainSize = 3;
        doReturn(chainSize).when(knibBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(knibBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(nativeBackupJoinVoMock1).getStatus();
        NativeBackupJoinVO nativeBackupJoinVoMock2 = Mockito.mock(NativeBackupJoinVO.class);
        NativeBackupJoinVO result = knibBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(nativeBackupJoinVoMock1, nativeBackupJoinVoMock2), null);

        assertEquals(nativeBackupJoinVoMock1, result);
        verify(knibBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsNotEmptyParentIsRemoved() {
        int chainSize = 2;
        doReturn(chainSize).when(knibBackupProviderSpy).getChainSizeForBackup(Mockito.any(), Mockito.anyLong());
        doNothing().when(knibBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());

        NativeBackupJoinVO nativeBackupJoinVoMock1 = Mockito.mock(NativeBackupJoinVO.class);
        doReturn(Backup.Status.Removed).when(nativeBackupJoinVoMock1).getStatus();
        NativeBackupJoinVO result = knibBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(nativeBackupJoinVoMock1), null);

        assertNull(result);
        verify(knibBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getImageStoreForBackupTestNoHeuristic() {
        long zoneId = 2;
        doReturn(null).when(heuristicRuleHelperMock).getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicType.BACKUP, backupVoMock);
        doReturn(dataStoreMock).when(dataStoreManagerMock).getImageStoreWithFreeCapacity(zoneId);

        DataStore result = knibBackupProviderSpy.getImageStoreForBackup(zoneId, backupVoMock);

        assertEquals(dataStoreMock, result);
    }

    @Test
    public void getImageStoreForBackupTestWithHeuristic() {
        long zoneId = 2;
        doReturn(dataStoreMock).when(heuristicRuleHelperMock).getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicType.BACKUP, backupVoMock);

        DataStore result = knibBackupProviderSpy.getImageStoreForBackup(zoneId, backupVoMock);

        assertEquals(dataStoreMock, result);
        verify(dataStoreManagerMock, Mockito.never()).getImageStoreWithFreeCapacity(Mockito.anyLong());
    }

    @Test (expected = CloudRuntimeException.class)
    public void getImageStoreForBackupTestNoStorageFound() {
        knibBackupProviderSpy.getImageStoreForBackup(0L, backupVoMock);
    }

    @Test
    public void getSucceedingVmSnapshotListTestBackupIsNull() {
        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestNoCurrentSnapshotVo() {
        doReturn(null).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);

        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(nativeBackupJoinVoMock);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestCurrentCreatedBeforeBackup() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime()-10000);
        Date now = DateUtil.now();
        doReturn(before).when(vmSnapshotVoMock).getCreated();
        doReturn(now).when(nativeBackupJoinVoMock).getDate();

        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(nativeBackupJoinVoMock);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getSucceedingVmSnapshotListTestCurrentVmSnapshotHasNoParent() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime()-10000);
        Date now = DateUtil.now();
        doReturn(now).when(vmSnapshotVoMock).getCreated();
        doReturn(before).when(nativeBackupJoinVoMock).getDate();

        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(nativeBackupJoinVoMock);

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
        doReturn(before).when(nativeBackupJoinVoMock).getDate();
        long snapParentId = 909;
        doReturn(snapParentId).when(vmSnapshotVoMock).getParent();
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        doReturn(now).when(vmSnapshotVoMock1).getCreated();
        doReturn(vmSnapshotVoMock1).when(vmSnapshotDaoMock).findById(snapParentId);

        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(nativeBackupJoinVoMock);

        assertEquals(List.of(vmSnapshotVoMock1, vmSnapshotVoMock), result);
    }


    @Test
    public void getSucceedingVmSnapshotListTestCurrentVmSnapshotHasParentsCreatedBefore() {
        doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findCurrentSnapshotByVmId(vmId);
        Date before = DateUtil.now();
        before.setTime(before.getTime() - 10000);
        Date now = DateUtil.now();
        doReturn(now).when(vmSnapshotVoMock).getCreated();
        doReturn(before).when(nativeBackupJoinVoMock).getDate();
        long snapParentId = 909;
        doReturn(snapParentId).when(vmSnapshotVoMock).getParent();
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        Date evenBefore = new Date(before.getTime() - 10000);
        doReturn(evenBefore).when(vmSnapshotVoMock1).getCreated();
        doReturn(vmSnapshotVoMock1).when(vmSnapshotDaoMock).findById(snapParentId);

        List<VMSnapshotVO> result = knibBackupProviderSpy.getSucceedingVmSnapshotList(nativeBackupJoinVoMock);

        assertEquals(List.of(vmSnapshotVoMock), result);
    }

    @Test
    public void mapVolumesToVmSnapshotReferencesTestVmSnapshotVOListIsEmpty() {
        knibBackupProviderSpy.mapVolumesToVmSnapshotReferences(List.of(), List.of());

        verify(vmSnapshotHelperMock, Mockito.never()).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(1);
    }

    @Test
    public void mapVolumesToVmSnapshotReferencesTestVmSnapshotVOListHasTwoElements() {
        VMSnapshotVO vmSnapshotVoMock1 = Mockito.mock(VMSnapshotVO.class);
        doReturn(1L).when(vmSnapshotVoMock).getId();
        doReturn(2L).when(vmSnapshotVoMock1).getId();
        doNothing().when(knibBackupProviderSpy).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());

        knibBackupProviderSpy.mapVolumesToVmSnapshotReferences(List.of(), List.of(vmSnapshotVoMock, vmSnapshotVoMock1));

        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(1);
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(2);
        verify(knibBackupProviderSpy, times(1)).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());
    }

    @Test
    public void createDeltaReferencesTestFullBackupEndOfChain() {
        doReturn(nativeBackupDataStoreVoMock).when(nativeBackupDataStoreDaoMock).persist(Mockito.any());

        knibBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KnibTO(volumeObjectToMock, List.of()));

        verify(nativeBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void createDeltaReferencesTestIsolatedBackup() {
        doReturn(nativeBackupDataStoreVoMock).when(nativeBackupDataStoreDaoMock).persist(Mockito.any());

        knibBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KnibTO(volumeObjectToMock, List.of()));

        verify(nativeBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
        verify(knibBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(Mockito.any(), Mockito.any(), Mockito.any());
        verify(knibBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(Mockito.any(), Mockito.any(), Mockito.any());
        verify(nativeBackupStoragePoolDaoMock, Mockito.times(0)).persist(Mockito.any());
    }

    @Test
    public void createDeltaReferencesTestNotFullBackupEndOfChain() {
        doReturn(nativeBackupDataStoreVoMock).when(nativeBackupDataStoreDaoMock).persist(Mockito.any());
        KnibTO knibTO = new KnibTO(volumeObjectToMock, List.of());
        doReturn(null).when(knibBackupProviderSpy).createDeltaMergeTreeForVolume(false, true, List.of(), null, knibTO);
        doNothing().when(knibBackupProviderSpy).findAndSetParentBackupPath(List.of(), null, knibTO);

        knibBackupProviderSpy.createDeltaReferences(false, true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, knibTO);

        verify(nativeBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
        verify(knibBackupProviderSpy, Mockito.times(1)).findAndSetParentBackupPath(List.of(), null, knibTO);
    }

    @Test
    public void createDeltaReferencesTestFullBackupNotEndOfChainDoesNotHaveVmSnapshotSucceedingLastBackup() {
        doReturn(nativeBackupDataStoreVoMock).when(nativeBackupDataStoreDaoMock).persist(Mockito.any());

        knibBackupProviderSpy.createDeltaReferences(true,
                false, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KnibTO(volumeObjectToMock, List.of()));

        verify(nativeBackupDataStoreDaoMock, Mockito.times(1)).persist(Mockito.any());
    }

    @Test
    public void orchestrateTakeBackupTestHostIsDownReturnFalse() {

        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Down);

        Pair<Boolean, Long> result = knibBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertFalse(result.first());
    }

    @Test
    public void orchestrateTakeBackupTestHostIsDisconnectedReturnFalse() {

        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Disconnected);

        Pair<Boolean, Long> result = knibBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertFalse(result.first());
    }

    @Test
    public void setBackupAsIsolatedTestPersistIsolatedDetail() {
        knibBackupProviderSpy.setBackupAsIsolated(backupVoMock);
        verify(backupDetailDaoMock, Mockito.times(1)).persist(Mockito.any());
    }
}
