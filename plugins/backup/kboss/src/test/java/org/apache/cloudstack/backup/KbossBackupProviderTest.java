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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDetailsDao;
import org.apache.cloudstack.backup.dao.InternalBackupDataStoreDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.apache.cloudstack.backup.dao.InternalBackupServiceJobDao;
import org.apache.cloudstack.backup.dao.InternalBackupStoragePoolDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.command.BackupDeleteAnswer;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KbossTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.BackupException;
import com.cloud.utils.exception.BackupProviderException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class KbossBackupProviderTest {

    @Mock
    private VirtualMachine virtualMachineMock;

    @Mock
    private BackupOfferingVO backupOfferingMock;

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
    private BackupDetailVO backupDetailVoMock;

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

    @Mock
    private UserVmDao userVmDaoMock;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDaoMock;

    @Mock
    private VMInstanceDetailVO vmInstanceDetailVoMock;

    @Mock
    private UserVmVO userVmVOMock;

    @Mock
    private BackupOfferingDao backupOfferingDaoMock;

    @Mock
    private AgentManager agentManagerMock;

    @Mock
    private TakeKbossBackupAnswer takeKbossBackupAnswerMock;

    @Mock
    private EndPointSelector endPointSelectorMock;

    @Mock
    private EndPoint endPointMock;

    @Mock
    private Backup.VolumeInfo backupVolumeInfoMock;

    @Mock
    private VolumeInfo volumeInfoMock;

    @Mock
    private VolumeApiService volumeApiServiceMock;

    @Mock
    private VolumeDataFactory volumeDataFactoryMock;

    @Mock
    private BackupOfferingDetailsDao backupOfferingDetailsDaoMock;

    @Mock
    private BackupOfferingDetailsVO backupOfferingDetailsVoMock;

    @Mock
    private Answer answerMock;

    @Mock
    private InternalBackupServiceJobDao internalBackupServiceJobDaoMock;

    @Mock
    private UserVmManager userVmManagerMock;

    @Mock
    private HypervisorGuruManager hypervisorGuruManagerMock;
    @Mock
    private HypervisorGuru hypervisorGuruMock;
    @Mock
    private VirtualMachineTO virtualMachineToMock;
    @Mock
    private ImageStoreDao imageStoreDaoMock;

    @Mock
    private VolumeObject volumeObjectMock;

    @Mock
    private InternalBackupStoragePoolVO internalBackupStoragePoolVoMock;

    @Mock
    private BackupDeltaTO backupDeltaToMock;

    @Mock
    private DeltaMergeTreeTO deltaMergeTreeToMock;

    @Spy
    @InjectMocks
    private KbossBackupProvider kbossBackupProviderSpy;

    private long vmId = 319832;
    private long volumeId = 41;

    Long backupId = 312L;

    @Before
    public void setup() {
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(vmId).when(backupVoMock).getVmId();
        doReturn(vmId).when(internalBackupJoinVoMock).getVmId();
        doReturn(vmId).when(userVmVOMock).getId();
        doReturn(backupId).when(backupVoMock).getId();
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

        verify(kbossBackupProviderSpy, Mockito.never()).mergeCurrentBackupDeltas(any());
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestWithActiveChain() {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();

        boolean result = kbossBackupProviderSpy.removeVMFromBackupOffering(virtualMachineMock);

        verify(kbossBackupProviderSpy, Mockito.times(1)).mergeCurrentBackupDeltas(any());
        assertTrue(result);
    }

    @Test
    public void removeVMFromBackupOfferingTestFailedToEndChain() {
        doReturn(VirtualMachine.State.Stopped).when(userVmVOMock).getState();
        doReturn(false).when(kbossBackupProviderSpy).endBackupChain(any());
        doReturn(userVmVOMock).when(userVmDaoMock).findById(any());
        doNothing().when(vmInstanceDetailsDaoMock).addDetail(Mockito.anyLong(), any(), any(), Mockito.anyBoolean());

        boolean result = kbossBackupProviderSpy.removeVMFromBackupOffering(userVmVOMock);

        verify(vmInstanceDetailsDaoMock, Mockito.times(1)).addDetail(Mockito.anyLong(), any(), any(), Mockito.anyBoolean());
        verify(userVmDaoMock, Mockito.times(1)).update(Mockito.anyLong(), any());
        assertFalse(result);
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

        verify(backupDetailDaoMock, Mockito.never()).persist(any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeLowerThanOneAndConfigBiggerThanZero() {
        int chainSize = 1;
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(-1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeBiggerThanOne() {
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(2, 0, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.never()).persist(any());
    }

    @Test
    public void setEndOfChainTrueIfRemainingChainSizeIsOneTestChainSizeIsOne() {
        int chainSize = 2;
        kbossBackupProviderSpy.setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(1, chainSize, 1, "uuid");

        verify(backupDetailDaoMock, Mockito.times(1)).persist(any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsEmpty() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());

        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(), null);

        assertNull(result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(),
                any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsBiggerThanChainSize() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2), null);

        assertEquals(internalBackupJoinVoMock1, result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsSmallerThanChainSize() {
        int chainSize = 3;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO internalBackupJoinVoMock2 = Mockito.mock(InternalBackupJoinVO.class);
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1, internalBackupJoinVoMock2), null);

        assertEquals(internalBackupJoinVoMock1, result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());
    }

    @Test
    public void getParentAndSetEndOfChainTestBackupChainIsNotEmptyParentIsRemoved() {
        int chainSize = 2;
        doReturn(chainSize).when(kbossBackupProviderSpy).getChainSizeForBackup(any(), Mockito.anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());

        InternalBackupJoinVO internalBackupJoinVoMock1 = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.Removed).when(internalBackupJoinVoMock1).getStatus();
        InternalBackupJoinVO result = kbossBackupProviderSpy.getParentAndSetEndOfChain(backupVoMock, List.of(internalBackupJoinVoMock1), null);

        assertNull(result);
        verify(kbossBackupProviderSpy, Mockito.times(1)).setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong(), any());
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
        doNothing().when(kbossBackupProviderSpy).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), anyMap());

        kbossBackupProviderSpy.mapVolumesToVmSnapshotReferences(List.of(), List.of(vmSnapshotVoMock, vmSnapshotVoMock1));

        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(1);
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(2);
        verify(kbossBackupProviderSpy, times(1)).mapVolumesToSnapshotReferences(Mockito.anyList(), Mockito.anyList(), anyMap());
    }

    @Test
    public void createDeltaReferencesTestFullBackupEndOfChain() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(any());
    }

    @Test
    public void createDeltaReferencesTestIsolatedBackup() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(any());
        verify(kbossBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(any(), any(), any());
        verify(kbossBackupProviderSpy, Mockito.times(0)).findAndSetParentBackupPath(any(), any(), any());
        verify(internalBackupStoragePoolDaoMock, Mockito.times(1)).persist(any());
    }

    @Test
    public void createDeltaReferencesTestNotFullBackupEndOfChain() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(any());
        KbossTO kbossTO = new KbossTO(volumeObjectToMock, List.of());
        doReturn(null).when(kbossBackupProviderSpy).createDeltaMergeTreeForVolume(false, true, List.of(), null, kbossTO);
        doNothing().when(kbossBackupProviderSpy).findAndSetParentBackupPath(List.of(), null, kbossTO);

        kbossBackupProviderSpy.createDeltaReferences(false, true, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, kbossTO);

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).findAndSetParentBackupPath(List.of(), null, kbossTO);
    }

    @Test
    public void createDeltaReferencesTestFullBackupNotEndOfChainDoesNotHaveVmSnapshotSucceedingLastBackup() {
        doReturn(internalBackupDataStoreVoMock).when(internalBackupDataStoreDaoMock).persist(any());

        kbossBackupProviderSpy.createDeltaReferences(true,
                false, true, backupVoMock, List.of(), List.of(), new HashMap<>(), new HashMap<>(), null, new KbossTO(volumeObjectToMock, List.of()));

        verify(internalBackupDataStoreDaoMock, Mockito.times(1)).persist(any());
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

    @Test (expected = BackupProviderException.class)
    public void orchestrateTakeBackupTestInitialValidationThrowException() {
        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostVOMock.getResourceState()).thenReturn(ResourceState.Enabled);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any());
        Mockito.doThrow(new BackupProviderException("tst")).when(kbossBackupProviderSpy).validateStorages(any(), any());

        kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertEquals(Backup.Status.Failed, backupVoMock.getStatus());
        Mockito.verify(backupDaoMock, Mockito.times(1)).update(Mockito.anyLong(), any());
    }

    @Test
    public void orchestrateTakeBackupTestIsolatedBackupFailed() {
        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostVOMock.getResourceState()).thenReturn(ResourceState.Enabled);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any());

        VolumeObjectTO vol1 = Mockito.mock(VolumeObjectTO.class);
        VolumeObjectTO vol2 = Mockito.mock(VolumeObjectTO.class);
        doReturn(List.of(vol1, vol2)).when(vmSnapshotHelperMock).getVolumeTOList(Mockito.anyLong());

        doNothing().when(kbossBackupProviderSpy).validateStorages(any(), any());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(any());
        doReturn(dataStoreMock).when(kbossBackupProviderSpy).getImageStoreForBackup(any(), any());

        Pair<Boolean, Long> result = kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, true);
        assertFalse(result.first());
        assertNull(result.second());
        verify(kbossBackupProviderSpy, Mockito.times(1)).setBackupAsIsolated(backupVoMock);
        verify(kbossBackupProviderSpy, Mockito.times(2)).createDeltaReferences(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).processBackupFailure(any(), any(), Mockito.anyLong(), Mockito.anyBoolean(), any());
    }

    @Test
    public void orchestrateTakeBackupTestIsolatedBackupSuccessWithCompression() {
        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostVOMock.getResourceState()).thenReturn(ResourceState.Enabled);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any());

        VolumeObjectTO vol1 = Mockito.mock(VolumeObjectTO.class);
        VolumeObjectTO vol2 = Mockito.mock(VolumeObjectTO.class);
        doReturn(List.of(vol1, vol2)).when(vmSnapshotHelperMock).getVolumeTOList(Mockito.anyLong());

        doNothing().when(kbossBackupProviderSpy).validateStorages(any(), any());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(any());
        doReturn(dataStoreMock).when(kbossBackupProviderSpy).getImageStoreForBackup(any(), any());
        doReturn(takeKbossBackupAnswerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(takeKbossBackupAnswerMock).getResult();
        doNothing().when(kbossBackupProviderSpy).processBackupSuccess(anyBoolean(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                anyLong(), anyBoolean(), anyBoolean());
        doReturn(true).when(kbossBackupProviderSpy).offeringSupportsCompression(internalBackupJoinVoMock);
        doNothing().when(kbossBackupProviderSpy).compressBackupAsync(internalBackupJoinVoMock, 0, 0);

        Pair<Boolean, Long> result = kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, true);
        assertTrue(result.first());
        assertEquals(backupId, result.second());
        verify(kbossBackupProviderSpy, Mockito.times(1)).setBackupAsIsolated(backupVoMock);
        verify(kbossBackupProviderSpy, Mockito.times(2)).createDeltaReferences(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).processBackupSuccess(anyBoolean(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                anyLong(), anyBoolean(), anyBoolean());
        verify(kbossBackupProviderSpy, Mockito.times(1)).compressBackupAsync(internalBackupJoinVoMock, 0, 0);
    }

    @Test
    public void orchestrateTakeBackupTestBackupSuccessWithValidation() {
        Mockito.when(virtualMachineManagerMock.findById(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(vmSnapshotHelperMock.pickRunningHost(Mockito.anyLong())).thenReturn(1L);
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(hostVOMock);
        Mockito.when(hostVOMock.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostVOMock.getResourceState()).thenReturn(ResourceState.Enabled);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any());

        VolumeObjectTO vol1 = Mockito.mock(VolumeObjectTO.class);
        VolumeObjectTO vol2 = Mockito.mock(VolumeObjectTO.class);
        doReturn(List.of(vol1, vol2)).when(vmSnapshotHelperMock).getVolumeTOList(Mockito.anyLong());

        doNothing().when(kbossBackupProviderSpy).validateStorages(any(), any());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(any());
        InternalBackupJoinVO parentMock = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentMock).when(kbossBackupProviderSpy).getParentAndSetEndOfChain(any(), any(), any());
        doReturn(dataStoreMock).when(kbossBackupProviderSpy).getImageStoreForBackup(any(), any());
        doReturn(takeKbossBackupAnswerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(takeKbossBackupAnswerMock).getResult();
        doNothing().when(kbossBackupProviderSpy).processBackupSuccess(anyBoolean(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                anyLong(), anyBoolean(), anyBoolean());
        doReturn(false).when(kbossBackupProviderSpy).offeringSupportsCompression(internalBackupJoinVoMock);
        doNothing().when(kbossBackupProviderSpy).validateBackupAsyncIfHasOfferingSupport(any(), anyLong(), anyLong());

        Pair<Boolean, Long> result = kbossBackupProviderSpy.orchestrateTakeBackup(backupVoMock, false, false);
        assertTrue(result.first());
        assertEquals(backupId, result.second());
        verify(internalBackupStoragePoolDaoMock).listByBackupId(0);
        verify(internalBackupDataStoreDaoMock).listByBackupId(0);
        verify(kbossBackupProviderSpy, Mockito.times(2)).createDeltaReferences(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).processBackupSuccess(anyBoolean(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                anyLong(), anyBoolean(), anyBoolean());
        verify(kbossBackupProviderSpy, Mockito.times(1)).validateBackupAsyncIfHasOfferingSupport(internalBackupJoinVoMock, 0, 0);
    }

    @Test
    public void orchestrateDeleteBackupTestBackupStateIsNotOk() {
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertFalse(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
    }

    @Test
    public void orchestrateDeleteBackupTestDeleteFailedBackup() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(true).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertTrue(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        verify(kbossBackupProviderSpy, never()).sendBackupCommands(anyLong(), any());
    }

    @Test
    public void orchestrateDeleteBackupTestDeleteBackupWithLiveChildren() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findByParentId(anyLong());
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock).getStatus();

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertTrue(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(backupVoMock).setStatus(Backup.Status.Removed);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy, never()).sendBackupCommands(anyLong(), any());
    }

    @Test
    public void orchestrateDeleteBackupTestDeleteCurrentBackupWithNoChildrenFailedToMerge() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(false).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(internalBackupJoinVoMock);

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertFalse(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(kbossBackupProviderSpy, never()).sendBackupCommands(anyLong(), any());
        verify(kbossBackupProviderSpy).mergeCurrentBackupDeltas(any());
    }

    @Test (expected = CloudRuntimeException.class)
    public void orchestrateDeleteBackupTestDeleteCurrentBackupWithNoChildrenWithParentNoEndPoint() throws OperationTimedoutException, AgentUnavailableException {
        long parentBackupId = 12;
        doReturn(parentBackupId).when(internalBackupJoinVoMock).getParentId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(internalBackupJoinVoMock);
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentBackupId);
        doReturn(Backup.Status.BackedUp).when(parentVo).getStatus();
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        doReturn(null).when(kbossBackupProviderSpy).getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(any(), any());
        doReturn(null).when(endPointSelectorMock).select((DataStore)null);

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertFalse(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(backupDetailDaoMock).persist(any());
        verify(kbossBackupProviderSpy, never()).sendBackupCommands(anyLong(), any());
    }

    @Test (expected = CloudRuntimeException.class)
    public void orchestrateDeleteBackupTestDeleteCurrentBackupWithNoChildrenWithParentTimedoutException() throws OperationTimedoutException, AgentUnavailableException {
        long parentBackupId = 12;
        doReturn(parentBackupId).when(internalBackupJoinVoMock).getParentId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(internalBackupJoinVoMock);
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentBackupId);
        doReturn(Backup.Status.BackedUp).when(parentVo).getStatus();
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        doReturn(null).when(kbossBackupProviderSpy).getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(any(), any());
        doReturn(endPointMock).when(endPointSelectorMock).select((DataStore)null);
        doThrow(OperationTimedoutException.class).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());

        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertFalse(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(backupDetailDaoMock).persist(any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).sendBackupCommands(anyLong(), any());
    }

    @Test
    public void orchestrateDeleteBackupTestDeleteCurrentBackupWithNoChildrenWithParentFailedSetNotEmpty() throws OperationTimedoutException, AgentUnavailableException {
        long parentBackupId = 12;
        doReturn(parentBackupId).when(internalBackupJoinVoMock).getParentId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(internalBackupJoinVoMock);
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentBackupId);
        doReturn(Backup.Status.BackedUp).when(parentVo).getStatus();
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        doReturn(new Pair<>(List.of(), parentVo)).when(kbossBackupProviderSpy).getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(any(), any());
        doReturn(endPointMock).when(endPointSelectorMock).select((DataStore)null);
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());
        doReturn(false).when(kbossBackupProviderSpy).processRemoveBackupFailures(anyBoolean(), any(), any(), any());
        doNothing().when(kbossBackupProviderSpy).processRemovedBackups(any());


        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertFalse(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(backupDetailDaoMock, Mockito.times(2)).persist(any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).sendBackupCommands(anyLong(), any());
    }

    @Test
    public void orchestrateDeleteBackupTestDeleteCurrentBackupWithNoChildrenWithParentFailedSetEmpty() throws OperationTimedoutException, AgentUnavailableException {
        long parentBackupId = 12;
        doReturn(parentBackupId).when(internalBackupJoinVoMock).getParentId();
        doReturn(virtualMachineMock).when(virtualMachineManagerMock).findById(vmId);
        doNothing().when(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForRemoval(backupId);
        doReturn(false).when(kbossBackupProviderSpy).deleteFailedBackup(backupVoMock);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(true).when(kbossBackupProviderSpy).mergeCurrentBackupDeltas(internalBackupJoinVoMock);
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentBackupId);
        doReturn(Backup.Status.BackedUp).when(parentVo).getStatus();
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        doReturn(new Pair<>(List.of(), parentVo)).when(kbossBackupProviderSpy).getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(any(), any());
        doReturn(endPointMock).when(endPointSelectorMock).select((DataStore)null);
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());
        doReturn(true).when(kbossBackupProviderSpy).processRemoveBackupFailures(anyBoolean(), any(), any(), any());
        doNothing().when(kbossBackupProviderSpy).processRemovedBackups(any());


        Boolean result = kbossBackupProviderSpy.orchestrateDeleteBackup(backupVoMock, false);

        assertTrue(result);
        verify(kbossBackupProviderSpy).validateVmState(any(), any(), any());
        verify(backupDetailDaoMock, Mockito.times(2)).persist(any());
        verify(kbossBackupProviderSpy, Mockito.times(1)).sendBackupCommands(anyLong(), any());
    }

    @Test
    public void orchestrateRestoreVMFromBackupTestInvalidState() {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(false, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);

        boolean result = kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, false, null, false);

        assertFalse(result);
    }

    @Test (expected = CloudRuntimeException.class)
    public void orchestrateRestoreVMFromBackupTestCurrentBackupNoHostToRestore() throws AgentUnavailableException {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        long currentBackupId = 39;
        doThrow(AgentUnavailableException.class).when(kbossBackupProviderSpy).getHostToRestore(virtualMachineMock, false, null);

        kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, false, null, false);

        verify(internalBackupStoragePoolDaoMock).listByBackupId(currentBackupId);
    }

    @Test (expected = CloudRuntimeException.class)
    public void orchestrateRestoreVMFromBackupTestSameVmCurrentBackupTimeOut() throws AgentUnavailableException, OperationTimedoutException {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        long currentBackupId = 39;
        InternalBackupJoinVO currentBackup = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(currentBackupId).when(currentBackup).getId();
        doReturn(currentBackup).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(hostVOMock).when(kbossBackupProviderSpy).getHostToRestore(virtualMachineMock, false, null);
        doNothing().when(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), anyBoolean());
        doReturn(List.of()).when(kbossBackupProviderSpy).getVolumesThatAreNotPartOfTheBackup(any(), any());
        doReturn(List.of()).when(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        doThrow(OperationTimedoutException.class).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());

        boolean result = kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, false, null, true);

        verify(internalBackupStoragePoolDaoMock).listByBackupId(currentBackupId);
        verify(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        verify(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
    }

    @Test
    public void orchestrateRestoreVMFromBackupTestSameVmCurrentBackupNullAnswers() throws AgentUnavailableException, OperationTimedoutException {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        long currentBackupId = 39;
        InternalBackupJoinVO currentBackup = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(currentBackupId).when(currentBackup).getId();
        doReturn(currentBackup).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(hostVOMock).when(kbossBackupProviderSpy).getHostToRestore(virtualMachineMock, false, null);
        doNothing().when(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), anyBoolean());
        doReturn(List.of()).when(kbossBackupProviderSpy).getVolumesThatAreNotPartOfTheBackup(any(), any());
        doReturn(List.of()).when(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());

        boolean result = kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, false, null, true);

        verify(internalBackupStoragePoolDaoMock).listByBackupId(currentBackupId);
        verify(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        verify(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        assertFalse(result);
    }

    @Test
    public void orchestrateRestoreVMFromBackupTestSameVmCurrentBackupAnswerFalse() throws AgentUnavailableException, OperationTimedoutException {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        long currentBackupId = 39;
        InternalBackupJoinVO currentBackup = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(currentBackupId).when(currentBackup).getId();
        doReturn(currentBackup).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(hostVOMock).when(kbossBackupProviderSpy).getHostToRestore(virtualMachineMock, false, null);
        doNothing().when(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), anyBoolean());
        doReturn(List.of()).when(kbossBackupProviderSpy).getVolumesThatAreNotPartOfTheBackup(any(), any());
        doReturn(List.of()).when(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        doReturn(new Answer[]{Mockito.mock(Answer.class)}).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());
        doReturn(false).when(kbossBackupProviderSpy).processRestoreAnswers(any(), any(), anyBoolean());

        boolean result = kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, false, null, true);

        verify(internalBackupStoragePoolDaoMock).listByBackupId(currentBackupId);
        verify(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        verify(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        assertFalse(result);
    }

    @Test
    public void orchestrateRestoreVMFromBackupTestSameVmQuickRestoreCurrentBackupAnswerTrue() throws AgentUnavailableException, OperationTimedoutException {
        doNothing().when(kbossBackupProviderSpy).validateNoVmSnapshots(virtualMachineMock);
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        long currentBackupId = 39;
        InternalBackupJoinVO currentBackup = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(currentBackupId).when(currentBackup).getId();
        doReturn(currentBackup).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(hostVOMock).when(kbossBackupProviderSpy).getHostToRestore(virtualMachineMock, true, null);
        doNothing().when(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), anyBoolean());
        doReturn(List.of()).when(kbossBackupProviderSpy).getVolumesThatAreNotPartOfTheBackup(any(), any());
        doReturn(List.of()).when(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        doReturn(new Answer[]{Mockito.mock(Answer.class)}).when(kbossBackupProviderSpy).sendBackupCommands(anyLong(), any());
        doReturn(true).when(kbossBackupProviderSpy).processRestoreAnswers(any(), any(), anyBoolean());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(currentBackup);
        doReturn(List.of()).when(kbossBackupProviderSpy).getVolumesToConsolidate(any(), any(), any(), anyLong(), anyBoolean());
        doReturn(true).when(kbossBackupProviderSpy).finalizeQuickRestore(any(), anyList(), anyLong());

        boolean result = kbossBackupProviderSpy.orchestrateRestoreVMFromBackup(backupVoMock, virtualMachineMock, true, null, true);

        verify(internalBackupStoragePoolDaoMock).listByBackupId(currentBackupId);
        verify(kbossBackupProviderSpy).createAndAttachVolumes(any(), any(), any(), any());
        verify(kbossBackupProviderSpy).populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(any(), any(), any(), any(), any());
        verify(kbossBackupProviderSpy).updateVolumePathsAndSizeIfNeeded(any(), any(), anyList(), anyList(), anyBoolean());
        verify(internalBackupStoragePoolDaoMock).expungeByBackupId(currentBackupId);
        verify(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(currentBackup);
        verify(kbossBackupProviderSpy).finalizeQuickRestore(any(), anyList(), anyLong());
        assertTrue(result);
    }

    @Test
    public void orchestrateRestoreBackedUpVolumeTestInvalidState() {
        doReturn(new Pair<>(false, null)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);

        Pair<Boolean, String> result = kbossBackupProviderSpy.orchestrateRestoreBackedUpVolume(backupVoMock, virtualMachineMock, null, null, false);

        assertFalse(result.first());
        verify(kbossBackupProviderSpy, Mockito.never()).sendBackupCommand(anyLong(), any());
    }

    @Test (expected = CloudRuntimeException.class)
    public void orchestrateRestoreBackedUpVolumeTestAnswerFalse() {
        doReturn(new Pair<>(true, null)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        doReturn(volumeVoMock).when(volumeDaoMock).findByUuidIncludingRemoved(any());
        doReturn(hostVOMock).when(hostDaoMock).findByIp(any());
        doReturn(volumeInfoMock).when(kbossBackupProviderSpy).duplicateAndCreateVolume(virtualMachineMock, hostVOMock, backupVolumeInfoMock);
        doReturn(volumeObjectToMock).when(volumeInfoMock).getTO();
        doReturn(new Pair<BackupDeltaTO, VolumeObjectTO>(null, null)).when(kbossBackupProviderSpy).generateBackupAndVolumePairForSingleNewVolume(any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).getParentSecondaryStorageUrls(backupVoMock);
        doReturn(false).when(kbossBackupProviderSpy).processRestoreAnswers(any(), any(), anyBoolean());

        Pair<Boolean, String> result = kbossBackupProviderSpy.orchestrateRestoreBackedUpVolume(backupVoMock, virtualMachineMock, backupVolumeInfoMock, null, false);

        verify(kbossBackupProviderSpy, Mockito.times(1)).sendBackupCommand(anyLong(), any());
    }

    @Test
    public void orchestrateRestoreBackedUpVolumeTestQuickRestoreAnswerTrue() {
        doReturn(new Pair<>(true, null)).when(kbossBackupProviderSpy).validateCompressionStateForRestoreAndGetBackup(backupId);
        doReturn(volumeVoMock).when(volumeDaoMock).findByUuidIncludingRemoved(any());
        doReturn(hostVOMock).when(hostDaoMock).findByIp(any());
        doReturn(volumeInfoMock).when(kbossBackupProviderSpy).duplicateAndCreateVolume(virtualMachineMock, hostVOMock, backupVolumeInfoMock);
        doReturn(volumeObjectToMock).when(volumeInfoMock).getTO();
        doReturn(new Pair<BackupDeltaTO, VolumeObjectTO>(null, null)).when(kbossBackupProviderSpy).generateBackupAndVolumePairForSingleNewVolume(any(), any(), any());
        doReturn(Set.of()).when(kbossBackupProviderSpy).getParentSecondaryStorageUrls(backupVoMock);
        doReturn(true).when(kbossBackupProviderSpy).processRestoreAnswers(any(), any(), anyBoolean());
        doReturn(volumeVoMock).when(volumeInfoMock).getVolume();
        doReturn(volumeVoMock).when(volumeApiServiceMock).attachVolumeToVM(anyLong(), anyLong(), any(), anyBoolean(), anyBoolean());
        doReturn(true).when(kbossBackupProviderSpy).finalizeQuickRestore(any(), anyList(), anyLong());

        Pair<Boolean, String> result = kbossBackupProviderSpy.orchestrateRestoreBackedUpVolume(backupVoMock, virtualMachineMock, backupVolumeInfoMock, null, true);

        assertTrue(result.first());

        verify(kbossBackupProviderSpy, Mockito.times(1)).sendBackupCommand(anyLong(), any());
        verify(volumeApiServiceMock).attachVolumeToVM(anyLong(), anyLong(), any(), anyBoolean(), anyBoolean());
        verify(kbossBackupProviderSpy).finalizeQuickRestore(any(), anyList(), anyLong());
    }

    @Test
    public void startBackupCompressionTestInvalidState() {
        doReturn(new Pair<>(false, null)).when(kbossBackupProviderSpy).validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);

        boolean result = kbossBackupProviderSpy.startBackupCompression(backupId, 0);

        assertFalse(result);
    }

    @Test
    public void startBackupCompressionTestNullAnswer() {
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        long parentId = 1332;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of(internalBackupDataStoreVoMock)).when(internalBackupDataStoreDaoMock).listByBackupId(backupId);
        InternalBackupDataStoreVO parentDelta = mock(InternalBackupDataStoreVO.class);
        doReturn(List.of(parentDelta)).when(internalBackupDataStoreDaoMock).listByBackupId(parentId);
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), any());
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(anyLong());
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(anyLong(), any());
        doReturn("zstd").when(backupOfferingDetailsVoMock).getValue();
        doReturn(List.of(parentVo)).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);
        doReturn(null).when(kbossBackupProviderSpy).getChainImageStoreUrls(any());
        doReturn(null).when(agentManagerMock).easySend(anyLong(), any());

        boolean result = kbossBackupProviderSpy.startBackupCompression(backupId, 0);

        assertFalse(result);
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.CompressionError);
        verify(backupDaoMock).update(backupId, backupVoMock);
    }

    @Test
    public void startBackupCompressionTestSuccess() {
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        long parentId = 1332;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        InternalBackupJoinVO parentVo = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(parentVo).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of(internalBackupDataStoreVoMock)).when(internalBackupDataStoreDaoMock).listByBackupId(backupId);
        InternalBackupDataStoreVO parentDelta = mock(InternalBackupDataStoreVO.class);
        doReturn(List.of(parentDelta)).when(internalBackupDataStoreDaoMock).listByBackupId(parentId);
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), any());
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(anyLong());
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(anyLong(), any());
        doReturn("zstd").when(backupOfferingDetailsVoMock).getValue();
        doReturn(List.of(parentVo)).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);
        doReturn(null).when(kbossBackupProviderSpy).getChainImageStoreUrls(any());
        doReturn(answerMock).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(true).when(answerMock).getResult();

        boolean result = kbossBackupProviderSpy.startBackupCompression(backupId, 0);

        assertTrue(result);
        verify(internalBackupServiceJobDaoMock).persist(any());
    }

    @Test
    public void finalizeBackupCompressionTestInvalidState() {
        doReturn(new Pair<>(false, null)).when(kbossBackupProviderSpy).validateBackupStateForFinalizeCompression(backupId);

        boolean result = kbossBackupProviderSpy.finalizeBackupCompression(backupId, 0);

        assertFalse(result);
    }

    @Test
    public void finalizeBackupCompressionTestNullAnswer() {
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateBackupStateForFinalizeCompression(backupId);
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(hostVOMock).when(hostDaoMock).findById(0L);
        doReturn(null).when(agentManagerMock).easySend(anyLong(), any());

        boolean result = kbossBackupProviderSpy.finalizeBackupCompression(backupId, 0);

        assertFalse(result);
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.CompressionError);
        verify(backupDaoMock).update(backupId, backupVoMock);
    }

    @Test
    public void finalizeBackupCompressionTestCleanup() {
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateBackupStateForFinalizeCompression(backupId);
        doReturn(Backup.Status.Removed).when(backupVoMock).getStatus();
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(hostVOMock).when(hostDaoMock).findById(0L);
        doReturn(answerMock).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(true).when(answerMock).getResult();

        boolean result = kbossBackupProviderSpy.finalizeBackupCompression(backupId, 0);

        assertTrue(result);
        verify(backupVoMock, Mockito.never()).setCompressionStatus(Backup.CompressionStatus.Compressed);
        verify(backupDaoMock, Mockito.never()).update(backupId, backupVoMock);
    }

    @Test
    public void finalizeBackupCompressionTestSuccess() {
        doReturn(new Pair<>(true, backupVoMock)).when(kbossBackupProviderSpy).validateBackupStateForFinalizeCompression(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(hostVOMock).when(hostDaoMock).findById(0L);
        doReturn(answerMock).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn("1").when(answerMock).getDetails();
        doNothing().when(kbossBackupProviderSpy).validateBackupAsyncIfHasOfferingSupport(any(), anyLong(), anyLong());

        boolean result = kbossBackupProviderSpy.finalizeBackupCompression(backupId, 0);

        assertTrue(result);
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.Compressed);
        verify(backupDaoMock).update(backupId, backupVoMock);
    }

    @Test
    public void validateBackupTestInvalidState() {
        doReturn(false).when(kbossBackupProviderSpy).validateBackupStateForValidation(backupId);

        boolean result = kbossBackupProviderSpy.validateBackup(backupId, 0);

        assertFalse(result);
    }

    @Test
    public void validateBackupTestValidateWithHash() {
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForValidation(backupId);
        doReturn(backupVoMock).when(backupDaoMock).findById(backupId);
        doReturn(backupDetailVoMock).when(backupDetailDaoMock).findDetail(backupId, BackupDetailsDao.BACKUP_HASH);
        doReturn(true).when(kbossBackupProviderSpy).validateWithHash(backupId, backupVoMock, backupDetailVoMock);

        boolean result = kbossBackupProviderSpy.validateBackup(backupId, 0);

        assertTrue(result);
        verify(kbossBackupProviderSpy).validateWithHash(backupId, backupVoMock, backupDetailVoMock);
    }

    @Test
    public void validateBackupTestValidateWithValidationVm() {
        doReturn(true).when(kbossBackupProviderSpy).validateBackupStateForValidation(backupId);
        doReturn(backupVoMock).when(backupDaoMock).findById(backupId);
        doReturn(null).when(backupDetailDaoMock).findDetail(backupId, BackupDetailsDao.BACKUP_HASH);
        doReturn(true).when(kbossBackupProviderSpy).validateWithValidationVm(backupId, 0, backupVoMock);

        boolean result = kbossBackupProviderSpy.validateBackup(backupId, 0);

        assertTrue(result);
        verify(kbossBackupProviderSpy).validateWithValidationVm(backupId, 0, backupVoMock);
    }

    @Test
    public void finishBackupChainTestInvalidState() {
        doReturn(userVmVOMock).when(userVmDaoMock).findById(vmId);
        doReturn(VirtualMachine.State.Migrating).when(userVmVOMock).getState();

        boolean result = kbossBackupProviderSpy.finishBackupChain(virtualMachineMock);

        assertFalse(result);
    }

    @Test
    public void finishBackupChainTestRunningVm() {
        doReturn(userVmVOMock).when(userVmDaoMock).findById(vmId);
        doReturn(VirtualMachine.State.Running).when(userVmVOMock).getState();
        doReturn(true).when(kbossBackupProviderSpy).endBackupChain(userVmVOMock);

        boolean result = kbossBackupProviderSpy.finishBackupChain(virtualMachineMock);

        assertTrue(result);
        verify(kbossBackupProviderSpy).endBackupChain(userVmVOMock);
    }

    @Test
    public void finishBackupChainTestBackupError() {
        doReturn(userVmVOMock).when(userVmDaoMock).findById(vmId);
        doReturn(VirtualMachine.State.BackupError).when(userVmVOMock).getState();
        doReturn(true).when(kbossBackupProviderSpy).normalizeBackupErrorAndFinishChain(userVmVOMock);

        boolean result = kbossBackupProviderSpy.finishBackupChain(virtualMachineMock);

        assertTrue(result);
        verify(kbossBackupProviderSpy).normalizeBackupErrorAndFinishChain(userVmVOMock);
    }

    @Test
    public void prepareVmForSnapshotRevertTestNoCurrentBackup() {
        doReturn(null).when(internalBackupJoinDaoMock).findCurrent(vmId);

        kbossBackupProviderSpy.prepareVmForSnapshotRevert(vmSnapshotVoMock, virtualMachineMock);

        verify(kbossBackupProviderSpy, never()).getSucceedingVmSnapshot(any());
    }

    @Test
    public void prepareVmForSnapshotRevertTestCurrentBackupBeforeVmSnapshot() {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(Date.from(Instant.EPOCH)).when(internalBackupJoinVoMock).getDate();
        doReturn(Date.from(Instant.now())).when(vmSnapshotVoMock).getCreated();

        kbossBackupProviderSpy.prepareVmForSnapshotRevert(vmSnapshotVoMock, virtualMachineMock);

        verify(kbossBackupProviderSpy, never()).getSucceedingVmSnapshot(any());
    }

    @Test (expected = CloudRuntimeException.class)
    public void prepareVmForSnapshotRevertTestCurrentBackupAfterVmSnapshotTimeout() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(Date.from(Instant.now())).when(internalBackupJoinVoMock).getDate();
        doReturn(Date.from(Instant.EPOCH)).when(vmSnapshotVoMock).getCreated();
        doReturn(List.of()).when(vmSnapshotHelperMock).getVolumeTOList(vmId);
        doReturn(vmSnapshotVoMock).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doNothing().when(kbossBackupProviderSpy).createDeleteCommandsAndMergeTrees(any(), any(), any(), any(), anyList());
        doThrow(OperationTimedoutException.class).when(kbossBackupProviderSpy).sendBackupCommands(any(), any());

        kbossBackupProviderSpy.prepareVmForSnapshotRevert(vmSnapshotVoMock, virtualMachineMock);

        verify(kbossBackupProviderSpy, never()).updateReferencesAfterPrepareForSnapshotRevert(any(), any(), any(), any());
    }

    @Test (expected = CloudRuntimeException.class)
    public void prepareVmForSnapshotRevertTestCurrentBackupAfterVmSnapshotNullAnswer() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(Date.from(Instant.now())).when(internalBackupJoinVoMock).getDate();
        doReturn(Date.from(Instant.EPOCH)).when(vmSnapshotVoMock).getCreated();
        doReturn(List.of()).when(vmSnapshotHelperMock).getVolumeTOList(vmId);
        doReturn(vmSnapshotVoMock).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doNothing().when(kbossBackupProviderSpy).createDeleteCommandsAndMergeTrees(any(), any(), any(), any(), anyList());
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommands(any(), any());

        kbossBackupProviderSpy.prepareVmForSnapshotRevert(vmSnapshotVoMock, virtualMachineMock);

        verify(kbossBackupProviderSpy, never()).updateReferencesAfterPrepareForSnapshotRevert(any(), any(), any(), any());
    }

    @Test
    public void prepareVmForSnapshotRevertTestCurrentBackupAfterVmSnapshotSuccess() throws OperationTimedoutException, AgentUnavailableException {
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findCurrent(vmId);
        doReturn(Date.from(Instant.now())).when(internalBackupJoinVoMock).getDate();
        doReturn(Date.from(Instant.EPOCH)).when(vmSnapshotVoMock).getCreated();
        doReturn(List.of()).when(vmSnapshotHelperMock).getVolumeTOList(vmId);
        doReturn(vmSnapshotVoMock).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doNothing().when(kbossBackupProviderSpy).createDeleteCommandsAndMergeTrees(any(), any(), any(), any(), anyList());
        doReturn(new Answer[]{}).when(kbossBackupProviderSpy).sendBackupCommands(any(), any());
        doNothing().when(kbossBackupProviderSpy).updateReferencesAfterPrepareForSnapshotRevert(any(), any(), any(), any());

        kbossBackupProviderSpy.prepareVmForSnapshotRevert(vmSnapshotVoMock, virtualMachineMock);

        verify(kbossBackupProviderSpy).updateReferencesAfterPrepareForSnapshotRevert(any(), any(), any(), any());
    }

    @Test (expected = BackupException.class)
    public void finalizeQuickRestoreTestStoppedVmStartException() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        doReturn(userVmVOMock).when(userVmDaoMock).findById(vmId);
        doReturn(VirtualMachine.State.Stopped).when(userVmVOMock).getState();
        doThrow(CloudRuntimeException.class).when(userVmManagerMock).startVirtualMachine(anyLong(), any(), any(), any(), anyBoolean());

        kbossBackupProviderSpy.finalizeQuickRestore(virtualMachineMock, List.of(), 0);
    }

    @Test
    public void finalizeQuickRestoreTestStoppedVmStartSuccess() {
        doReturn(userVmVOMock).when(userVmDaoMock).findById(vmId);
        doReturn(true).when(kbossBackupProviderSpy).consolidateVolumes(any(), anyLong(), anyList());

        boolean result = kbossBackupProviderSpy.finalizeQuickRestore(virtualMachineMock, List.of(), 0);

        assertTrue(result);
        verify(kbossBackupProviderSpy).consolidateVolumes(any(), anyLong(), anyList());
    }

    @Test
    public void validateWithHashTestNoHosts() {
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(null).when(hostDaoMock).listAllHostsUpByZoneAndHypervisor(anyLong(), any());
        doNothing().when(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithHash(backupId, backupVoMock, null);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(any(), any());
    }

    @Test
    public void validateWithHashTestAnswerResultFalse() {
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(List.of(hostVOMock)).when(hostDaoMock).listAllHostsUpByZoneAndHypervisor(anyLong(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(false).when(answerMock).getResult();
        doNothing().when(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithHash(backupId, backupVoMock, null);

        assertFalse(result);
        verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        verify(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(any(), any());
    }

    @Test
    public void validateWithHashTestDifferentHash() {
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(List.of(hostVOMock)).when(hostDaoMock).listAllHostsUpByZoneAndHypervisor(anyLong(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn("wrongHash").when(answerMock).getDetails();
        doReturn("correctHash").when(backupDetailVoMock).getValue();
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithHash(backupId, backupVoMock, backupDetailVoMock);

        assertFalse(result);
        verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(any(), any());
        verify(backupDaoMock, Mockito.never()).update(anyLong(), any());
    }

    @Test
    public void validateWithHashTestSameHash() {
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupDeltaTOList(backupId);
        doReturn(List.of(hostVOMock)).when(hostDaoMock).listAllHostsUpByZoneAndHypervisor(anyLong(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn("correctHash").when(answerMock).getDetails();
        doReturn("correctHash").when(backupDetailVoMock).getValue();

        boolean result = kbossBackupProviderSpy.validateWithHash(backupId, backupVoMock, backupDetailVoMock);

        assertTrue(result);
        verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        verify(kbossBackupProviderSpy, never()).setBackupAsInvalidAndSendAlert(any(), any());
        verify(kbossBackupProviderSpy, never()).setBackupUnableToValidateAndSendAlert(any(), any());
        verify(backupDaoMock).update(anyLong(), any());
    }


    @Test
    public void validateWithValidationVmTestValidationVmIsNull() {
        doReturn(null).when(kbossBackupProviderSpy).allocateValidationVm(anyLong(), any());

        boolean result = kbossBackupProviderSpy.validateWithValidationVm(backupId, 2L, backupVoMock);

        assertFalse(result);
        verify(kbossBackupProviderSpy, never()).cleanupValidation(anyBoolean(), any(), any(), any(), anyBoolean());
    }

    @Test
    public void validateWithValidationVmTestPrepareForValidationFails() throws NoTransitionException {
        doReturn(userVmVOMock).when(kbossBackupProviderSpy).allocateValidationVm(anyLong(), any());
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(List.of()).when(volumeDaoMock).findByInstance(anyLong());
        doNothing().when(kbossBackupProviderSpy).createValidationVolumesOnPrimaryStorage(any(), any(), any(), any(), any());
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), eq(false));
        doReturn(false).when(kbossBackupProviderSpy).prepareForValidation(anyLong(), any(), any(), any());

        boolean result = kbossBackupProviderSpy.validateWithValidationVm(backupId, 2L, backupVoMock);

        assertFalse(result);
        verify(kbossBackupProviderSpy).cleanupValidation(eq(false), eq(userVmVOMock), eq(backupVoMock), any(), eq(false));
    }

    @Test
    public void validateWithValidationVmTestValidateBackupFails() throws NoTransitionException {
        doReturn(userVmVOMock).when(kbossBackupProviderSpy).allocateValidationVm(anyLong(), any());
        doReturn(2L).when(userVmVOMock).getHostId();
        doReturn(Hypervisor.HypervisorType.KVM).when(userVmVOMock).getHypervisorType();
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(userVmVOMock).when(userVmDaoMock).findById(anyLong());
        doReturn(List.of()).when(volumeDaoMock).findByInstance(anyLong());
        doNothing().when(kbossBackupProviderSpy).createValidationVolumesOnPrimaryStorage(any(), any(), any(), any(), any());
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), eq(false));
        doReturn(true).when(kbossBackupProviderSpy).prepareForValidation(anyLong(), any(), any(), any());
        doReturn(hypervisorGuruMock).when(hypervisorGuruManagerMock).getGuru(any());
        doReturn(virtualMachineToMock).when(hypervisorGuruMock).implement(any());
        doReturn(false).when(kbossBackupProviderSpy).validateBackup(anyLong(), any(), any(), any(), any(), any());
        doNothing().when(kbossBackupProviderSpy).sendCleanupFailedEmail(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithValidationVm(backupId, 2L, backupVoMock);

        assertFalse(result);
        verify(kbossBackupProviderSpy).endBackupChainIfConfigured(backupVoMock);
        verify(kbossBackupProviderSpy).cleanupValidation(eq(true), eq(userVmVOMock), eq(backupVoMock), any(), eq(true));
    }

    @Test
    public void validateWithValidationVmTestSuccessfulValidation() throws NoTransitionException {
        doReturn(userVmVOMock).when(kbossBackupProviderSpy).allocateValidationVm(anyLong(), any());
        doReturn(2L).when(userVmVOMock).getHostId();
        doReturn(Hypervisor.HypervisorType.KVM).when(userVmVOMock).getHypervisorType();
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(2L).when(hostVOMock).getId();
        doReturn(userVmVOMock).when(userVmDaoMock).findById(anyLong());
        doReturn(List.of()).when(volumeDaoMock).findByInstance(anyLong());
        doNothing().when(kbossBackupProviderSpy).createValidationVolumesOnPrimaryStorage(any(), any(), any(), any(), any());
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(Set.of()).when(kbossBackupProviderSpy).generateBackupAndVolumePairsToRestore(any(), any(), any(), eq(false));
        doReturn(true).when(kbossBackupProviderSpy).prepareForValidation(anyLong(), any(), any(), any());
        doReturn(hypervisorGuruMock).when(hypervisorGuruManagerMock).getGuru(any());
        doReturn(virtualMachineToMock).when(hypervisorGuruMock).implement(any());
        doReturn(true).when(kbossBackupProviderSpy).validateBackup(anyLong(), any(), any(), any(), any(), any());
        doNothing().when(kbossBackupProviderSpy).calculateAndSaveHash(any(), any(), anyLong());
        doNothing().when(kbossBackupProviderSpy).sendCleanupFailedEmail(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithValidationVm(backupId, 2L, backupVoMock);

        assertTrue(result);
        verify(kbossBackupProviderSpy).calculateAndSaveHash(any(), eq(backupVoMock), anyLong());
        verify(kbossBackupProviderSpy).cleanupValidation(eq(true), eq(userVmVOMock), eq(backupVoMock), any(), eq(true));
    }

    @Test
    public void validateWithValidationVmTestExceptionHandling() throws NoTransitionException {
        doReturn(userVmVOMock).when(kbossBackupProviderSpy).allocateValidationVm(anyLong(), any());
        doReturn(hostVOMock).when(hostDaoMock).findById(anyLong());
        doReturn(List.of()).when(volumeDaoMock).findByInstance(anyLong());
        doThrow(new RuntimeException("boom")).when(kbossBackupProviderSpy).createValidationVolumesOnPrimaryStorage(any(), any(), any(), any(), any());
        doNothing().when(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(any(), any());

        boolean result = kbossBackupProviderSpy.validateWithValidationVm(backupId, 2L, backupVoMock);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupUnableToValidateAndSendAlert(eq(backupVoMock), contains("boom"));
        verify(kbossBackupProviderSpy).cleanupValidation(eq(false), eq(userVmVOMock), eq(backupVoMock), any(), eq(false));
    }


    @Test
    public void setBackupAsIsolatedTestPersistIsolatedDetail() {
        kbossBackupProviderSpy.setBackupAsIsolated(backupVoMock);
        verify(backupDetailDaoMock, Mockito.times(1)).persist(any());
    }

    @Test
    public void endBackupChainIfConfiguredTestFeatureDisabled() {
        doReturn(false).when(kbossBackupProviderSpy).getValidationEndChainOnFail(backupVoMock);

        kbossBackupProviderSpy.endBackupChainIfConfigured(backupVoMock);

        verify(kbossBackupProviderSpy, never()).endBackupChain(any());
    }

    @Test
    public void endBackupChainIfConfiguredTestNotCurrentAndNoCurrentChildren() {
        doReturn(true).when(kbossBackupProviderSpy).getValidationEndChainOnFail(backupVoMock);
        doReturn(false).when(internalBackupJoinVoMock).getCurrent();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        InternalBackupJoinVO child = mock(InternalBackupJoinVO.class);
        doReturn(false).when(child).getCurrent();
        doReturn(List.of(child)).when(kbossBackupProviderSpy).getBackupJoinChildren(any());

        kbossBackupProviderSpy.endBackupChainIfConfigured(backupVoMock);

        verify(kbossBackupProviderSpy, never()).endBackupChain(any());
    }

    @Test
    public void endBackupChainIfConfiguredTestBackupIsCurrent() {
        doReturn(true).when(kbossBackupProviderSpy).getValidationEndChainOnFail(backupVoMock);
        doReturn(true).when(internalBackupJoinVoMock).getCurrent();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupJoinChildren(any());
        doReturn(userVmVOMock).when(userVmDaoMock).findById(anyLong());
        doReturn(true).when(kbossBackupProviderSpy).endBackupChain(any());

        kbossBackupProviderSpy.endBackupChainIfConfigured(backupVoMock);

        verify(kbossBackupProviderSpy, times(1)).endBackupChain(userVmVOMock);
    }

    @Test
    public void endBackupChainIfConfiguredTestLastChildIsCurrent() {
        doReturn(true).when(kbossBackupProviderSpy).getValidationEndChainOnFail(backupVoMock);
        doReturn(false).when(internalBackupJoinVoMock).getCurrent();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        InternalBackupJoinVO child = mock(InternalBackupJoinVO.class);
        doReturn(true).when(child).getCurrent();
        doReturn(List.of(child)).when(kbossBackupProviderSpy).getBackupJoinChildren(any());
        doReturn(userVmVOMock).when(userVmDaoMock).findById(anyLong());
        doReturn(true).when(kbossBackupProviderSpy).endBackupChain(any());

        kbossBackupProviderSpy.endBackupChainIfConfigured(backupVoMock);

        verify(kbossBackupProviderSpy, times(1)).endBackupChain(userVmVOMock);
    }


    @Test
    public void normalizeBackupErrorAndFinishChainTestAnswerNull() {
        doReturn(null).when(vmInstanceDetailsDaoMock).findDetail(anyLong(), any());
        doReturn(backupVoMock).when(backupDaoMock).findLatestByStatusAndVmId(any(), anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(mock(ImageStoreVO.class)).when(imageStoreDaoMock).findById(anyLong());
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(anyLong());
        long parentId = 9382;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        doReturn(null).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doNothing().when(kbossBackupProviderSpy).configureKbossTosForCleanup(any(), any(), any(), anyBoolean(), any(), any());
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());

        boolean result = kbossBackupProviderSpy.normalizeBackupErrorAndFinishChain(userVmVOMock);

        assertFalse(result);
    }

    @Test
    public void normalizeBackupErrorAndFinishChainTestAnswerFailed() {
        doReturn(null).when(vmInstanceDetailsDaoMock).findDetail(anyLong(), any());
        doReturn(backupVoMock).when(backupDaoMock).findLatestByStatusAndVmId(any(), anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(mock(ImageStoreVO.class)).when(imageStoreDaoMock).findById(anyLong());
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(anyLong());
        long parentId = 9382;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        doReturn(null).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doNothing().when(kbossBackupProviderSpy).configureKbossTosForCleanup(any(), any(), any(), anyBoolean(), any(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(false).when(answerMock).getResult();

        boolean result = kbossBackupProviderSpy.normalizeBackupErrorAndFinishChain(userVmVOMock);

        assertFalse(result);
    }

    @Test
    public void normalizeBackupErrorAndFinishChainTestSuccessCallsEndChain() {
        doReturn(null).when(vmInstanceDetailsDaoMock).findDetail(anyLong(), any());
        doReturn(backupVoMock).when(backupDaoMock).findLatestByStatusAndVmId(any(), anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(mock(ImageStoreVO.class)).when(imageStoreDaoMock).findById(anyLong());
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(anyLong());
        long parentId = 9382;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        doReturn(null).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doNothing().when(kbossBackupProviderSpy).configureKbossTosForCleanup(any(), any(), any(), anyBoolean(), any(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn(false).when(kbossBackupProviderSpy).processCleanupBackupErrorAnswer(any(), any());
        doReturn(true).when(kbossBackupProviderSpy).endBackupChain(any());

        boolean result = kbossBackupProviderSpy.normalizeBackupErrorAndFinishChain(userVmVOMock);

        assertTrue(result);
        verify(kbossBackupProviderSpy).endBackupChain(userVmVOMock);
    }


    @Test
    public void normalizeBackupErrorAndFinishChainTestChainAlreadyEnded() {
        doReturn(null).when(vmInstanceDetailsDaoMock).findDetail(anyLong(), any());
        doReturn(backupVoMock).when(backupDaoMock).findLatestByStatusAndVmId(any(), anyLong());
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(anyLong());
        doReturn(mock(ImageStoreVO.class)).when(imageStoreDaoMock).findById(anyLong());
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(anyLong());
        long parentId = 9382;
        doReturn(parentId).when(internalBackupJoinVoMock).getParentId();
        doReturn(null).when(internalBackupJoinDaoMock).findById(parentId);
        doReturn(List.of()).when(internalBackupDataStoreDaoMock).listByBackupId(anyLong());
        doNothing().when(kbossBackupProviderSpy).configureKbossTosForCleanup(any(), any(), any(), anyBoolean(), any(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn(true).when(kbossBackupProviderSpy).processCleanupBackupErrorAnswer(any(), any());
        InternalBackupJoinVO current = mock(InternalBackupJoinVO.class);
        doReturn(current).when(internalBackupJoinDaoMock).findCurrent(anyLong());
        doNothing().when(internalBackupStoragePoolDaoMock).expungeByBackupId(anyLong());
        doNothing().when(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(any());

        boolean result = kbossBackupProviderSpy.normalizeBackupErrorAndFinishChain(userVmVOMock);

        assertTrue(result);
        verify(internalBackupStoragePoolDaoMock).expungeByBackupId(anyLong());
        verify(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(any());
    }

    @Test
    public void cleanupValidationTestStartedVmFalseAndValidationNotPreparedAndFailedToDestroyVolume() throws ResourceUnavailableException {
        VolumeVO dataVolume = Mockito.mock(VolumeVO.class);
        doReturn(Volume.Type.DATADISK).when(dataVolume).getVolumeType();
        doReturn(52L).when(dataVolume).getId();
        doNothing().when(kbossBackupProviderSpy).sendCleanupFailedEmail(any(), any());

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            kbossBackupProviderSpy.cleanupValidation(false, userVmVOMock, backupVoMock, List.of(dataVolume), false);

            verify(userVmManagerMock, Mockito.never()).stopVirtualMachine(anyLong(), anyBoolean());
            verify(userVmManagerMock, Mockito.times(1)).destroyVm(any(DestroyVMCmd.class), Mockito.eq(false));
            verify(volumeApiServiceMock, Mockito.times(1)).destroyVolume(Mockito.eq(52L), any(), Mockito.eq(true), Mockito.eq(true), Mockito.isNull());
            verify(agentManagerMock, Mockito.never()).easySend(anyLong(), any());
        }
    }

    @Test
    public void cleanupValidationTestDestroyVmThrowsAndCleanupMailIsSent() throws ResourceUnavailableException {
        VolumeVO dataVolume = Mockito.mock(VolumeVO.class);
        doReturn(Volume.Type.DATADISK).when(dataVolume).getVolumeType();
        doReturn(88L).when(userVmVOMock).getHostId();
        doReturn(Set.of("secondary-storage-1")).when(kbossBackupProviderSpy).getSecondaryStorageUrls(userVmVOMock);
        doThrow(new RuntimeException("boom")).when(userVmManagerMock).destroyVm(any(DestroyVMCmd.class), Mockito.eq(false));
        doReturn(answerMock).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doNothing().when(kbossBackupProviderSpy).sendCleanupFailedEmail(any(), any());

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            kbossBackupProviderSpy.cleanupValidation(false, userVmVOMock, backupVoMock, List.of(dataVolume), true);

            verify(userVmManagerMock, Mockito.times(1)).destroyVm(any(DestroyVMCmd.class), Mockito.eq(false));
            verify(kbossBackupProviderSpy, Mockito.times(1)).sendCleanupFailedEmail(eq(backupVoMock), contains("Got an unexpected exception while trying to destroy validation VM."));
            verify(agentManagerMock, Mockito.times(1)).easySend(Mockito.eq(88L), any(CleanupKbossValidationCommand.class));
        }
    }

    @Test
    public void cleanupValidationTestCleanupCommandFails() {
        VolumeVO dataVolume = Mockito.mock(VolumeVO.class);
        doReturn(Volume.Type.DATADISK).when(dataVolume).getVolumeType();
        doReturn(88L).when(userVmVOMock).getHostId();
        doReturn(Set.of("secondary-storage-1")).when(kbossBackupProviderSpy).getSecondaryStorageUrls(userVmVOMock);
        doReturn(null).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(hostVOMock).when(hostDaoMock).findById(88L);
        doNothing().when(kbossBackupProviderSpy).sendCleanupFailedEmail(any(), any());

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            kbossBackupProviderSpy.cleanupValidation(false, userVmVOMock, backupVoMock, List.of(dataVolume), true);

            verify(agentManagerMock, Mockito.times(1)).easySend(Mockito.eq(88L), any(CleanupKbossValidationCommand.class));
            verify(hostDaoMock, Mockito.times(1)).findById(88L);
        }
    }

    @Test
    public void cleanupValidationTestNoEmailSentAndAllStepsExecuted() throws ResourceUnavailableException {
        VolumeVO rootVolume = Mockito.mock(VolumeVO.class);
        doReturn(Volume.Type.ROOT).when(rootVolume).getVolumeType();

        VolumeVO dataVolume = Mockito.mock(VolumeVO.class);
        doReturn(Volume.Type.DATADISK).when(dataVolume).getVolumeType();
        doReturn(52L).when(dataVolume).getId();

        doReturn(77L).when(userVmVOMock).getId();
        doReturn(88L).when(userVmVOMock).getHostId();
        doReturn(Set.of("secondary-storage-1")).when(kbossBackupProviderSpy).getSecondaryStorageUrls(userVmVOMock);
        doReturn(answerMock).when(agentManagerMock).easySend(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn(volumeVoMock).when(volumeApiServiceMock).destroyVolume(Mockito.eq(52L), any(), Mockito.eq(true), Mockito.eq(true), Mockito.isNull());

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            kbossBackupProviderSpy.cleanupValidation(true, userVmVOMock, backupVoMock, List.of(rootVolume, dataVolume), true);

            verify(userVmManagerMock, Mockito.times(1)).stopVirtualMachine(77L, true);
            verify(userVmManagerMock, Mockito.times(1)).destroyVm(any(DestroyVMCmd.class), Mockito.eq(false));
            verify(volumeApiServiceMock, Mockito.times(1)).destroyVolume(Mockito.eq(52L), any(), Mockito.eq(true), Mockito.eq(true), Mockito.isNull());
            verify(agentManagerMock, Mockito.times(1)).easySend(Mockito.eq(88L), any(CleanupKbossValidationCommand.class));
            verify(kbossBackupProviderSpy, Mockito.never()).sendCleanupFailedEmail(any(), any());
            verify(hostDaoMock, Mockito.never()).findById(anyLong());
        }
    }

    @Test
    public void getVolumesToConsolidateTestSameVmAsBackupFalse() {
        VolumeObjectTO volumeObjectTO1 = Mockito.mock(VolumeObjectTO.class);
        doReturn(11L).when(volumeObjectTO1).getVolumeId();
        VolumeInfo volumeInfo1 = Mockito.mock(VolumeInfo.class);
        doReturn(volumeVoMock).when(volumeInfo1).getVolume();
        doReturn(volumeInfo1).when(volumeDataFactoryMock).getVolume(11L);

        VolumeObjectTO volumeObjectTO2 = Mockito.mock(VolumeObjectTO.class);
        doReturn(22L).when(volumeObjectTO2).getVolumeId();
        VolumeInfo volumeInfo2 = Mockito.mock(VolumeInfo.class);
        doReturn(Mockito.mock(VolumeVO.class)).when(volumeInfo2).getVolume();
        doReturn(volumeInfo2).when(volumeDataFactoryMock).getVolume(22L);

        doNothing().when(kbossBackupProviderSpy).transitVmStateWithoutThrow(any(), any(), anyLong());
        doNothing().when(kbossBackupProviderSpy).transitVolumeStateWithoutThrow(any(), any());

        List<VolumeInfo> result = kbossBackupProviderSpy.getVolumesToConsolidate(virtualMachineMock, List.of(), List.of(volumeObjectTO1, volumeObjectTO2), 99L, false);

        assertEquals(List.of(volumeInfo1, volumeInfo2), result);
        verify(kbossBackupProviderSpy, times(1)).transitVmStateWithoutThrow(virtualMachineMock, VirtualMachine.Event.RestoringSuccess, 99L);
        verify(volumeDataFactoryMock, times(1)).getVolume(11L);
        verify(volumeDataFactoryMock, times(1)).getVolume(22L);
        verify(kbossBackupProviderSpy, times(2)).transitVolumeStateWithoutThrow(any(), eq(Volume.Event.RestoreSucceeded));
    }

    @Test
    public void getVolumesToConsolidateTestSameVmAsBackupTrueFiltersBySecondaryDeltas() {
        VolumeObjectTO volumeObjectTO1 = Mockito.mock(VolumeObjectTO.class);
        doReturn(11L).when(volumeObjectTO1).getVolumeId();
        VolumeInfo volumeInfo1 = Mockito.mock(VolumeInfo.class);
        doReturn(volumeVoMock).when(volumeInfo1).getVolume();
        doReturn(volumeInfo1).when(volumeDataFactoryMock).getVolume(11L);

        VolumeObjectTO volumeObjectTO2 = Mockito.mock(VolumeObjectTO.class);
        doReturn(22L).when(volumeObjectTO2).getVolumeId();
        VolumeInfo volumeInfo2 = Mockito.mock(VolumeInfo.class);
        doReturn(Mockito.mock(VolumeVO.class)).when(volumeInfo2).getVolume();
        doReturn(volumeInfo2).when(volumeDataFactoryMock).getVolume(22L);

        InternalBackupDataStoreVO delta = Mockito.mock(InternalBackupDataStoreVO.class);
        doReturn(33L).when(delta).getVolumeId();

        doNothing().when(kbossBackupProviderSpy).transitVmStateWithoutThrow(any(), any(), anyLong());
        doNothing().when(kbossBackupProviderSpy).transitVolumeStateWithoutThrow(any(), any());

        List<VolumeInfo> result = kbossBackupProviderSpy.getVolumesToConsolidate(virtualMachineMock, List.of(delta), List.of(volumeObjectTO1, volumeObjectTO2), 99L, true);

        assertEquals(List.of(), result);
        verify(kbossBackupProviderSpy, times(1)).transitVmStateWithoutThrow(virtualMachineMock, VirtualMachine.Event.RestoringSuccess, 99L);
        verify(volumeDataFactoryMock, times(1)).getVolume(11L);
        verify(volumeDataFactoryMock, times(1)).getVolume(22L);
        verify(kbossBackupProviderSpy, times(2)).transitVolumeStateWithoutThrow(any(), eq(Volume.Event.RestoreSucceeded));
    }

    @Test
    public void checkErrorBackupTestNonErrorStatusDoesNothing() {
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();

        kbossBackupProviderSpy.checkErrorBackup(backupVoMock, virtualMachineMock);

        verify(backupVoMock, never()).setStatus(Backup.Status.Failed);
    }

    @Test
    public void checkErrorBackupTestErrorStatusAndVmIsNullSetsBackupFailed() {
        doReturn(Backup.Status.Error).when(backupVoMock).getStatus();

        kbossBackupProviderSpy.checkErrorBackup(backupVoMock, null);

        verify(backupVoMock).setStatus(Backup.Status.Failed);
    }

    @Test
    public void checkErrorBackupTestErrorStatusAndVmNotBackupErrorSetsBackupFailed() {
        doReturn(Backup.Status.Error).when(backupVoMock).getStatus();
        doReturn(VirtualMachine.State.Running).when(virtualMachineMock).getState();

        kbossBackupProviderSpy.checkErrorBackup(backupVoMock, virtualMachineMock);

        verify(backupVoMock).setStatus(Backup.Status.Failed);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkErrorBackupTestErrorStatusAndVmInBackupErrorThrows() {
        doReturn(Backup.Status.Error).when(backupVoMock).getStatus();
        doReturn(VirtualMachine.State.BackupError).when(virtualMachineMock).getState();

        kbossBackupProviderSpy.checkErrorBackup(backupVoMock, virtualMachineMock);

        verify(backupVoMock, never()).setStatus(Backup.Status.Failed);
    }

    @Test
    public void deleteFailedBackupTestFailedBackupIsExpungedAndCleanedUp() {
        long backupId = 123L;
        doReturn(backupId).when(backupVoMock).getId();
        doReturn(Backup.Status.Failed).when(backupVoMock).getStatus();

        boolean result = kbossBackupProviderSpy.deleteFailedBackup(backupVoMock);

        assertTrue(result);
        verify(backupVoMock).setStatus(Backup.Status.Expunged);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(internalBackupStoragePoolDaoMock).expungeByBackupId(backupId);
        verify(internalBackupDataStoreDaoMock).expungeByBackupId(backupId);
        verify(backupDetailDaoMock).removeDetails(backupId);
    }

    @Test
    public void deleteFailedBackupTestNonFailedBackupDoesNothing() {
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();

        boolean result = kbossBackupProviderSpy.deleteFailedBackup(backupVoMock);

        assertFalse(result);
        verify(backupVoMock, never()).setStatus(Backup.Status.Expunged);
        verify(backupDaoMock, never()).update(anyLong(), any());
        verify(internalBackupStoragePoolDaoMock, never()).expungeByBackupId(anyLong());
        verify(internalBackupDataStoreDaoMock, never()).expungeByBackupId(anyLong());
        verify(backupDetailDaoMock, never()).removeDetails(anyLong());
    }

    @Test
    public void mergeCurrentDeltaIntoVolumeTestNoDeltaDoesNothing() {
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn(null).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(volumeId);

        kbossBackupProviderSpy.mergeCurrentDeltaIntoVolume(volumeVoMock, virtualMachineMock, "detach", true);

        verify(internalBackupStoragePoolDaoMock, times(1)).findOneByVolumeId(volumeId);
        verify(internalBackupJoinDaoMock, never()).findById(anyLong());
    }

    @Test (expected = CloudRuntimeException.class)
    public void mergeCurrentDeltaIntoVolumeTestNullAnswer() {
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(volumeId);
        doReturn(backupId).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(null).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doReturn(deltaMergeTreeToMock).when(kbossBackupProviderSpy).createDeltaMergeTree(anyBoolean(), anyBoolean(), any(), any(), any());
        doReturn(null).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        try (MockedStatic<VolumeObject> volumeObjectMockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            when(VolumeObject.getVolumeObject(any(), any())).thenReturn(volumeObjectMock);

            kbossBackupProviderSpy.mergeCurrentDeltaIntoVolume(volumeVoMock, virtualMachineMock, "detach", true);

            verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
            verify(kbossBackupProviderSpy, never()).expungeOldDeltasAndUpdateVmSnapshotIfNeeded(anyList(), any());
        }
    }

    @Test
    public void mergeCurrentDeltaIntoVolumeTestNoSucceedingSnapshot() {
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(volumeId);
        doReturn(backupId).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(null).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doReturn(deltaMergeTreeToMock).when(kbossBackupProviderSpy).createDeltaMergeTree(anyBoolean(), anyBoolean(), any(), any(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doReturn(volumeVoMock).when(volumeDaoMock).findById(anyLong());
        doReturn(backupDeltaToMock).when(deltaMergeTreeToMock).getParent();
        doNothing().when(kbossBackupProviderSpy).expungeOldDeltasAndUpdateVmSnapshotIfNeeded(anyList(), any());
        doReturn(List.of()).when(internalBackupStoragePoolDaoMock).listByBackupId(backupId);
        doNothing().when(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(any());

        try (MockedStatic<VolumeObject> volumeObjectMockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            when(VolumeObject.getVolumeObject(any(), any())).thenReturn(volumeObjectMock);

            kbossBackupProviderSpy.mergeCurrentDeltaIntoVolume(volumeVoMock, virtualMachineMock, "detach", true);

            verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
            verify(volumeDaoMock).update(volumeId, volumeVoMock);
            verify(kbossBackupProviderSpy).expungeOldDeltasAndUpdateVmSnapshotIfNeeded(anyList(), any());
            verify(kbossBackupProviderSpy).setEndOfChainAndRemoveCurrentForBackup(any());
        }
    }

    @Test
    public void mergeCurrentDeltaIntoVolumeTestWithSucceedingSnapshotWithMoreDeltas() {
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn(internalBackupStoragePoolVoMock).when(internalBackupStoragePoolDaoMock).findOneByVolumeId(volumeId);
        doReturn(backupId).when(internalBackupStoragePoolVoMock).getBackupId();
        doReturn(internalBackupJoinVoMock).when(internalBackupJoinDaoMock).findById(backupId);
        doReturn(vmSnapshotVoMock).when(kbossBackupProviderSpy).getSucceedingVmSnapshot(internalBackupJoinVoMock);
        doReturn(deltaMergeTreeToMock).when(kbossBackupProviderSpy).createDeltaMergeTree(anyBoolean(), anyBoolean(), any(), any(), any());
        doReturn(answerMock).when(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
        doReturn(true).when(answerMock).getResult();
        doNothing().when(kbossBackupProviderSpy).expungeOldDeltasAndUpdateVmSnapshotIfNeeded(anyList(), any());
        doReturn(List.of(internalBackupStoragePoolVoMock)).when(internalBackupStoragePoolDaoMock).listByBackupId(backupId);

        try (MockedStatic<VolumeObject> volumeObjectMockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            when(VolumeObject.getVolumeObject(any(), any())).thenReturn(volumeObjectMock);

            kbossBackupProviderSpy.mergeCurrentDeltaIntoVolume(volumeVoMock, virtualMachineMock, "detach", true);

            verify(kbossBackupProviderSpy).sendBackupCommand(anyLong(), any());
            verify(volumeDaoMock, never()).update(volumeId, volumeVoMock);
            verify(kbossBackupProviderSpy).expungeOldDeltasAndUpdateVmSnapshotIfNeeded(anyList(), any());
            verify(kbossBackupProviderSpy, never()).setEndOfChainAndRemoveCurrentForBackup(any());
        }
    }

    @Test
    public void getHostToRestoreTestNonQuickRestoreUsesRunningHost() throws AgentUnavailableException {
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(77L).when(vmSnapshotHelperMock).pickRunningHost(vmId);
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(77L);

        HostVO result = kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, false, null);

        assertEquals(hostVOMock, result);
        verify(vmSnapshotHelperMock, times(1)).pickRunningHost(vmId);
        verify(hostDaoMock, times(1)).findByIdIncludingRemoved(77L);
    }

    @Test
    public void getHostToRestoreTestQuickRestoreUsesProvidedHostId() throws AgentUnavailableException {
        doReturn(Status.Up).when(hostVOMock).getStatus();
        doReturn(false).when(hostVOMock).isInMaintenanceStates();
        doReturn(ResourceState.Enabled).when(hostVOMock).getResourceState();
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(55L);

        HostVO result = kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, 55L);

        assertEquals(hostVOMock, result);
        verify(vmSnapshotHelperMock, never()).pickRunningHost(anyLong());
        verify(hostDaoMock, times(1)).findByIdIncludingRemoved(55L);
    }

    @Test
    public void getHostToRestoreTestQuickRestoreUsesVmLastHostIdWhenHostIdIsNull() throws AgentUnavailableException {
        doReturn(99L).when(virtualMachineMock).getLastHostId();
        doReturn(Status.Up).when(hostVOMock).getStatus();
        doReturn(false).when(hostVOMock).isInMaintenanceStates();
        doReturn(ResourceState.Enabled).when(hostVOMock).getResourceState();
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(99L);

        HostVO result = kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, null);

        assertEquals(hostVOMock, result);
        verify(hostDaoMock, times(1)).findByIdIncludingRemoved(99L);
    }

    @Test(expected = AgentUnavailableException.class)
    public void getHostToRestoreTestQuickRestoreWithNoHostIdAndNoLastHostThrows() throws AgentUnavailableException {
        doReturn(null).when(virtualMachineMock).getLastHostId();

        kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, null);
    }

    @Test(expected = AgentUnavailableException.class)
    public void getHostToRestoreTestQuickRestoreWithHostDownThrows() throws AgentUnavailableException {
        doReturn(Status.Down).when(hostVOMock).getStatus();
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(55L);

        kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, 55L);
    }

    @Test(expected = AgentUnavailableException.class)
    public void getHostToRestoreTestQuickRestoreWithHostInMaintenanceThrows() throws AgentUnavailableException {
        doReturn(Status.Up).when(hostVOMock).getStatus();
        doReturn(true).when(hostVOMock).isInMaintenanceStates();
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(55L);

        kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, 55L);
    }

    @Test(expected = AgentUnavailableException.class)
    public void getHostToRestoreTestQuickRestoreWithHostDisabledThrows() throws AgentUnavailableException {
        doReturn(Status.Up).when(hostVOMock).getStatus();
        doReturn(false).when(hostVOMock).isInMaintenanceStates();
        doReturn(ResourceState.Disabled).when(hostVOMock).getResourceState();
        doReturn(hostVOMock).when(hostDaoMock).findByIdIncludingRemoved(55L);

        kbossBackupProviderSpy.getHostToRestore(virtualMachineMock, true, 55L);
    }

    @Test
    public void gatherSnapshotReferencesOfChildrenSnapshotTestVmSnapshotIsNull() {
        List<VolumeObjectTO> volumeObjectTOs = List.of(volumeObjectToMock);

        Map<Long, List<SnapshotDataStoreVO>> result = kbossBackupProviderSpy.gatherSnapshotReferencesOfChildrenSnapshot(volumeObjectTOs, null);

        assertTrue(result.isEmpty());
        verify(vmSnapshotDaoMock, never()).listByParent(anyLong());
        verify(vmSnapshotHelperMock, never()).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(anyLong());
        verify(kbossBackupProviderSpy, never()).mapVolumesToSnapshotReferences(anyList(), anyList(), anyMap());
    }

    @Test
    public void gatherSnapshotReferencesOfChildrenSnapshotTestChildrenListIsEmpty() {
        doReturn(100L).when(vmSnapshotVoMock).getId();
        doReturn(List.of()).when(vmSnapshotDaoMock).listByParent(100L);

        List<VolumeObjectTO> volumeObjectTOs = List.of(volumeObjectToMock);

        Map<Long, List<SnapshotDataStoreVO>> result = kbossBackupProviderSpy.gatherSnapshotReferencesOfChildrenSnapshot(volumeObjectTOs, vmSnapshotVoMock);

        assertTrue(result.isEmpty());
        verify(vmSnapshotDaoMock, times(1)).listByParent(100L);
        verify(vmSnapshotHelperMock, never()).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(anyLong());
        verify(kbossBackupProviderSpy, never()).mapVolumesToSnapshotReferences(anyList(), anyList(), anyMap());
    }

    @Test
    public void gatherSnapshotReferencesOfChildrenSnapshotTestSingleChildWithSingleSnapshotReference() {
        VMSnapshotVO childSnapshot = Mockito.mock(VMSnapshotVO.class);
        SnapshotDataStoreVO snapshotDataStoreVO = Mockito.mock(SnapshotDataStoreVO.class);

        doReturn(100L).when(vmSnapshotVoMock).getId();
        doReturn(List.of(childSnapshot)).when(vmSnapshotDaoMock).listByParent(100L);
        doReturn(200L).when(childSnapshot).getId();
        doReturn(List.of(snapshotDataStoreVO)).when(vmSnapshotHelperMock).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(200L);
        doNothing().when(kbossBackupProviderSpy).mapVolumesToSnapshotReferences(anyList(), anyList(), anyMap());

        List<VolumeObjectTO> volumeObjectTOs = List.of(volumeObjectToMock);

        Map<Long, List<SnapshotDataStoreVO>> result =
                kbossBackupProviderSpy.gatherSnapshotReferencesOfChildrenSnapshot(volumeObjectTOs, vmSnapshotVoMock);

        assertTrue(result.isEmpty());
        verify(vmSnapshotDaoMock, times(1)).listByParent(100L);
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(200L);
        verify(kbossBackupProviderSpy, times(1)).mapVolumesToSnapshotReferences(eq(volumeObjectTOs), anyList(), anyMap());
    }

    @Test
    public void gatherSnapshotReferencesOfChildrenSnapshotTestMultipleChildrenAggregatesSnapshotReferences() {
        VMSnapshotVO childSnapshot1 = Mockito.mock(VMSnapshotVO.class);
        VMSnapshotVO childSnapshot2 = Mockito.mock(VMSnapshotVO.class);
        SnapshotDataStoreVO snapshotDataStoreVO1 = Mockito.mock(SnapshotDataStoreVO.class);
        SnapshotDataStoreVO snapshotDataStoreVO2 = Mockito.mock(SnapshotDataStoreVO.class);

        doReturn(100L).when(vmSnapshotVoMock).getId();
        doReturn(List.of(childSnapshot1, childSnapshot2)).when(vmSnapshotDaoMock).listByParent(100L);
        doReturn(201L).when(childSnapshot1).getId();
        doReturn(202L).when(childSnapshot2).getId();
        doReturn(List.of(snapshotDataStoreVO1)).when(vmSnapshotHelperMock)
                .getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(201L);
        doReturn(List.of(snapshotDataStoreVO2)).when(vmSnapshotHelperMock)
                .getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(202L);
        doNothing().when(kbossBackupProviderSpy).mapVolumesToSnapshotReferences(anyList(), anyList(), anyMap());

        List<VolumeObjectTO> volumeObjectTOs = List.of(volumeObjectToMock);

        Map<Long, List<SnapshotDataStoreVO>> result =
                kbossBackupProviderSpy.gatherSnapshotReferencesOfChildrenSnapshot(volumeObjectTOs, vmSnapshotVoMock);

        assertTrue(result.isEmpty());
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(201L);
        verify(vmSnapshotHelperMock, times(1)).getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(202L);
        verify(kbossBackupProviderSpy, times(1)).mapVolumesToSnapshotReferences(eq(volumeObjectTOs), anyList(), anyMap());
    }

    @Test
    public void createDeltaMergeTreeTestChildIsVolumeWithoutSucceedingSnapshot() {
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), eq(DataStoreRole.Primary));
        doReturn("parent-path").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();

        DeltaMergeTreeTO result = kbossBackupProviderSpy.createDeltaMergeTree(true, true, internalBackupStoragePoolVoMock,
                volumeObjectToMock, null);

        assertEquals(volumeObjectToMock, result.getVolumeObjectTO());
        assertTrue(result.getGrandChildren().isEmpty());
        assertEquals(volumeObjectToMock, result.getChild());
        assertEquals("parent-path", result.getParent().getPath());
    }

    @Test
    public void createDeltaMergeTreeTestChildIsDeltaWithoutSucceedingSnapshot() {
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), eq(DataStoreRole.Primary));
        doReturn("parent-path").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();
        doReturn("child-path").when(internalBackupStoragePoolVoMock).getBackupDeltaPath();

        DeltaMergeTreeTO result = kbossBackupProviderSpy.createDeltaMergeTree(false, true, internalBackupStoragePoolVoMock,
                volumeObjectToMock, null);

        assertEquals(volumeObjectToMock, result.getVolumeObjectTO());
        assertEquals("parent-path", result.getParent().getPath());
        assertEquals("child-path", result.getChild().getPath());
        assertTrue(result.getGrandChildren().isEmpty());
    }

    @Test
    public void createDeltaMergeTreeTestChildIsDeltaWithSucceedingSnapshotReferences() {
        SnapshotDataStoreVO snapshotRefMock = Mockito.mock(SnapshotDataStoreVO.class);

        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), eq(DataStoreRole.Primary));
        doReturn("parent-path").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();
        doReturn("child-path").when(internalBackupStoragePoolVoMock).getBackupDeltaPath();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();
        doReturn("snapshot-grandchild").when(snapshotRefMock).getInstallPath();

        doReturn(Map.of(volumeId, List.of(snapshotRefMock))).when(kbossBackupProviderSpy).gatherSnapshotReferencesOfChildrenSnapshot(List.of(volumeObjectToMock), vmSnapshotVoMock);

        DeltaMergeTreeTO result = kbossBackupProviderSpy.createDeltaMergeTree(false, true, internalBackupStoragePoolVoMock,
                volumeObjectToMock, vmSnapshotVoMock);

        assertEquals("child-path", result.getChild().getPath());
        assertEquals(1, result.getGrandChildren().size());
        assertEquals("snapshot-grandchild", result.getGrandChildren().get(0).getPath());
    }

    @Test
    public void createDeltaMergeTreeTestChildIsDeltaWithSucceedingSnapshotButNoReferencesRebasesToVolumePath() {
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(anyLong(), eq(DataStoreRole.Primary));
        doReturn("parent-path").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();
        doReturn("child-path").when(internalBackupStoragePoolVoMock).getBackupDeltaPath();
        doReturn("/volume/path").when(volumeObjectToMock).getPath();

        doReturn(Map.of()).when(kbossBackupProviderSpy)
                .gatherSnapshotReferencesOfChildrenSnapshot(List.of(volumeObjectToMock), vmSnapshotVoMock);

        DeltaMergeTreeTO result = kbossBackupProviderSpy.createDeltaMergeTree(false, false, internalBackupStoragePoolVoMock,
                volumeObjectToMock, vmSnapshotVoMock);

        assertEquals(1, result.getGrandChildren().size());
        assertEquals("/volume/path", result.getGrandChildren().get(0).getPath());
    }

    @Test
    public void generateBackupAndVolumePairsToRestoreTestSameVmAsBackupMatchesByVolumeId() {
        doReturn(77L).when(internalBackupJoinVoMock).getImageStoreId();
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(77L, DataStoreRole.Image);
        doReturn(10L).when(internalBackupDataStoreVoMock).getVolumeId();
        doReturn("delta-path").when(internalBackupDataStoreVoMock).getBackupPath();
        doReturn(10L).when(volumeObjectToMock).getVolumeId();

        Set<Pair<BackupDeltaTO, VolumeObjectTO>> result = kbossBackupProviderSpy.generateBackupAndVolumePairsToRestore(List.of(internalBackupDataStoreVoMock),
                List.of(volumeObjectToMock), internalBackupJoinVoMock, true);

        assertEquals(1, result.size());
        Pair<BackupDeltaTO, VolumeObjectTO> pair = result.iterator().next();
        assertEquals(volumeObjectToMock, pair.second());
        assertEquals("delta-path", pair.first().getPath());
    }

    @Test
    public void generateBackupAndVolumePairsToRestoreTestDifferentVmMatchesByDeviceId() {
        doReturn(77L).when(internalBackupJoinVoMock).getImageStoreId();
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(77L, DataStoreRole.Image);
        doReturn(5L).when(internalBackupDataStoreVoMock).getDeviceId();
        doReturn("delta-path").when(internalBackupDataStoreVoMock).getBackupPath();
        doReturn(5L).when(volumeObjectToMock).getDeviceId();

        Set<Pair<BackupDeltaTO, VolumeObjectTO>> result = kbossBackupProviderSpy.generateBackupAndVolumePairsToRestore(List.of(internalBackupDataStoreVoMock),
                List.of(volumeObjectToMock), internalBackupJoinVoMock, false);

        assertEquals(1, result.size());
        Pair<BackupDeltaTO, VolumeObjectTO> pair = result.iterator().next();
        assertEquals(volumeObjectToMock, pair.second());
        assertEquals("delta-path", pair.first().getPath());
    }

    @Test(expected = CloudRuntimeException.class)
    public void generateBackupAndVolumePairsToRestoreTestThrowsWhenNoMatchingVolumeExists() {
        doReturn(77L).when(internalBackupJoinVoMock).getImageStoreId();
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(77L, DataStoreRole.Image);
        doReturn(10L).when(internalBackupDataStoreVoMock).getVolumeId();
        doReturn(123L).when(internalBackupDataStoreVoMock).getId();

        kbossBackupProviderSpy.generateBackupAndVolumePairsToRestore(List.of(internalBackupDataStoreVoMock), List.of(volumeObjectToMock), internalBackupJoinVoMock, true);
    }

    @Test
    public void populateDeltasToRemoveAndToMergeAndUpdateVolumePathsTestVolumeIsPartOfBackupAddsDeltaToRemoveAndUpdatesPath() {
        doReturn(dataStoreMock).when(dataStoreManagerMock).getDataStore(11L, DataStoreRole.Primary);
        doReturn(10L).when(internalBackupStoragePoolVoMock).getVolumeId();
        doReturn(11L).when(internalBackupStoragePoolVoMock).getStoragePoolId();
        doReturn("delta-path").when(internalBackupStoragePoolVoMock).getBackupDeltaPath();
        doReturn("parent-path").when(internalBackupStoragePoolVoMock).getBackupDeltaParentPath();
        doReturn(10L).when(volumeObjectToMock).getVolumeId();

        Set<BackupDeltaTO> deltasToRemove = new java.util.HashSet<>();

        List<DeltaMergeTreeTO> result = kbossBackupProviderSpy.populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(List.of(internalBackupStoragePoolVoMock), deltasToRemove,
                List.of(volumeObjectToMock), List.of(), "vm-uuid");

        assertTrue(result.isEmpty());
        assertEquals(1, deltasToRemove.size());
        verify(volumeObjectToMock, times(1)).setPath("parent-path");
        verify(dataStoreManagerMock, times(1)).getDataStore(11L, DataStoreRole.Primary);
    }

    @Test
    public void populateDeltasToRemoveAndToMergeAndUpdateVolumePathsTestVolumeIsPartOfBackupCreatesMergeTree() {
        doReturn(volumeId).when(internalBackupStoragePoolVoMock).getVolumeId();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();

        Set<BackupDeltaTO> deltasToRemove = new java.util.HashSet<>();

        doReturn(deltaMergeTreeToMock).when(kbossBackupProviderSpy).createDeltaMergeTree(true, false, internalBackupStoragePoolVoMock, volumeObjectToMock, null);

        List<DeltaMergeTreeTO> result = kbossBackupProviderSpy.populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(List.of(internalBackupStoragePoolVoMock), deltasToRemove,
                List.of(volumeObjectToMock), List.of(volumeObjectToMock), "vm-uuid");

        assertEquals(List.of(deltaMergeTreeToMock), result);
        assertTrue(deltasToRemove.isEmpty());
        verify(kbossBackupProviderSpy, times(1)).createDeltaMergeTree(true, false, internalBackupStoragePoolVoMock, volumeObjectToMock, null);
        verify(dataStoreManagerMock, never()).getDataStore(anyLong(), eq(DataStoreRole.Primary));
    }

    @Test(expected = CloudRuntimeException.class)
    public void populateDeltasToRemoveAndToMergeAndUpdateVolumePathsTestThrowsWhenNoMatchingVolumeExists() {
        kbossBackupProviderSpy.populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(List.of(internalBackupStoragePoolVoMock), Set.of(), List.of(), List.of(), "vm-uuid");
    }

    @Test
    public void updateVolumePathsAndSizeIfNeededTestUpdatesPathAndSizeWhenVolumePathChanged() {
        doReturn(List.of(volumeVoMock)).when(volumeDaoMock).findByInstance(vmId);
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn("old-path").when(volumeVoMock).getPath();
        doReturn(5L).when(volumeVoMock).getDeviceId();
        doReturn(100L).when(volumeVoMock).getSize();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();
        doReturn("new-path").when(volumeObjectToMock).getPath();
        doReturn(150L).when(backupVolumeInfoMock).getSize();
        doReturn(5L).when(backupVolumeInfoMock).getDeviceId();

        kbossBackupProviderSpy.updateVolumePathsAndSizeIfNeeded(virtualMachineMock, List.of(volumeObjectToMock), List.of(backupVolumeInfoMock), List.of(), false);

        verify(volumeVoMock).setPath("new-path");
        verify(volumeVoMock).setSize(150L);
        verify(volumeDaoMock).update(volumeId, volumeVoMock);
    }

    @Test
    public void updateVolumePathsAndSizeIfNeededTestUsesMergeTreeParentPathWhenVolumePathDidNotChange() {
        doReturn(List.of(volumeVoMock)).when(volumeDaoMock).findByInstance(vmId);
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn("same-path").when(volumeVoMock).getPath();
        doReturn(5L).when(volumeVoMock).getDeviceId();
        doReturn(100L).when(volumeVoMock).getSize();
        doReturn("same-path").when(volumeObjectToMock).getPath();
        doReturn(volumeId).when(volumeObjectToMock).getId();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();
        doReturn(150L).when(backupVolumeInfoMock).getSize();
        doReturn(5L).when(backupVolumeInfoMock).getDeviceId();
        doReturn(volumeObjectToMock).when(deltaMergeTreeToMock).getChild();
        doReturn(backupDeltaToMock).when(deltaMergeTreeToMock).getParent();
        doReturn("parent-path").when(backupDeltaToMock).getPath();

        kbossBackupProviderSpy.updateVolumePathsAndSizeIfNeeded(virtualMachineMock, List.of(volumeObjectToMock), List.of(backupVolumeInfoMock), List.of(deltaMergeTreeToMock),
                false);

        verify(volumeVoMock).setPath("parent-path");
        verify(volumeVoMock).setSize(150L);
        verify(volumeDaoMock).update(volumeId, volumeVoMock);
    }

    @Test
    public void updateVolumePathsAndSizeIfNeededTestLeavesSizeUntouchedWhenRestoreSizeMatchesCurrentSize() {
        doReturn(List.of(volumeVoMock)).when(volumeDaoMock).findByInstance(vmId);
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn("same-path").when(volumeVoMock).getPath();
        doReturn(5L).when(volumeVoMock).getDeviceId();
        doReturn(100L).when(volumeVoMock).getSize();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();
        doReturn("same-path").when(volumeObjectToMock).getPath();
        doReturn(100L).when(backupVolumeInfoMock).getSize();
        doReturn(5L).when(backupVolumeInfoMock).getDeviceId();

        kbossBackupProviderSpy.updateVolumePathsAndSizeIfNeeded(virtualMachineMock, List.of(volumeObjectToMock), List.of(backupVolumeInfoMock), List.of(), false);

        verify(volumeVoMock, never()).setSize(anyLong());
        verify(volumeDaoMock).update(volumeId, volumeVoMock);
    }

    @Test
    public void updateVolumePathsAndSizeIfNeededTestMatchesByUuidWhenSameVmAsBackup() {
        doReturn(List.of(volumeVoMock)).when(volumeDaoMock).findByInstance(vmId);
        doReturn(vmId).when(virtualMachineMock).getId();
        doReturn(volumeId).when(volumeVoMock).getId();
        doReturn("same-path").when(volumeVoMock).getPath();
        doReturn("vm-uuid").when(volumeVoMock).getUuid();
        doReturn(100L).when(volumeVoMock).getSize();
        doReturn(volumeId).when(volumeObjectToMock).getVolumeId();
        doReturn("same-path").when(volumeObjectToMock).getPath();
        doReturn("vm-uuid").when(backupVolumeInfoMock).getUuid();
        doReturn(120L).when(backupVolumeInfoMock).getSize();

        kbossBackupProviderSpy.updateVolumePathsAndSizeIfNeeded(virtualMachineMock, List.of(volumeObjectToMock), List.of(backupVolumeInfoMock), List.of(), true);

        verify(volumeVoMock).setSize(120L);
        verify(volumeDaoMock).update(volumeId, volumeVoMock);
    }


    @Test
    public void processRemoveBackupFailuresTestNoFailuresReturnsTrueAndRemovesNothing() {
        doReturn(backupId).when(internalBackupJoinVoMock).getId();

        Answer[] deleteAnswers = new Answer[] {answerMock};
        doReturn(true).when(answerMock).getResult();

        List<Long> removedBackupIds = new ArrayList<>(List.of(backupId, 200L));

        boolean result = kbossBackupProviderSpy.processRemoveBackupFailures(false, deleteAnswers, removedBackupIds, internalBackupJoinVoMock);

        assertTrue(result);
        assertEquals(List.of(backupId, 200L), removedBackupIds);
        verify(backupDaoMock, never()).findByIdIncludingRemoved(anyLong());
        verify(backupDaoMock, never()).update(anyLong(), any());
    }

    @Test
    public void processRemoveBackupFailuresTestFailureOnCurrentBackupNotForcedSetsError() {
        doReturn(backupId).when(internalBackupJoinVoMock).getId();

        BackupDeleteAnswer failedCurrentBackupAnswer = Mockito.mock(BackupDeleteAnswer.class);
        doReturn(false).when(failedCurrentBackupAnswer).getResult();
        doReturn(backupId).when(failedCurrentBackupAnswer).getBackupId();
        doReturn("delete failed").when(failedCurrentBackupAnswer).getDetails();

        doReturn(backupId).when(backupVoMock).getId();
        doReturn(backupVoMock).when(backupDaoMock).findByIdIncludingRemoved(backupId);

        List<Long> removedBackupIds = new ArrayList<>(List.of(backupId, 200L));

        boolean result = kbossBackupProviderSpy.processRemoveBackupFailures(false, new Answer[]{failedCurrentBackupAnswer}, removedBackupIds, internalBackupJoinVoMock);

        assertFalse(result);
        assertEquals(List.of(200L), removedBackupIds);
        verify(backupVoMock).setStatus(Backup.Status.Error);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(backupDaoMock, never()).findByIdIncludingRemoved(200L);
    }

    @Test
    public void processRemoveBackupFailuresTestFailureOnCurrentBackupForcedSetBackupAsExpunged() {
        BackupDeleteAnswer failedCurrentBackupAnswer = Mockito.mock(BackupDeleteAnswer.class);
        doReturn(false).when(failedCurrentBackupAnswer).getResult();
        doReturn(backupId).when(failedCurrentBackupAnswer).getBackupId();
        doReturn("delete failed").when(failedCurrentBackupAnswer).getDetails();
        doReturn(backupVoMock).when(backupDaoMock).findByIdIncludingRemoved(backupId);

        List<Long> removedBackupIds = new ArrayList<>(List.of(backupId, 200L));

        boolean result = kbossBackupProviderSpy.processRemoveBackupFailures(true, new Answer[]{failedCurrentBackupAnswer}, removedBackupIds, internalBackupJoinVoMock);

        assertFalse(result);
        assertEquals(List.of(200L), removedBackupIds);
        verify(backupVoMock).setStatus(Backup.Status.Expunged);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(backupDaoMock, never()).findByIdIncludingRemoved(200L);
    }

    @Test
    public void processRemoveBackupFailuresTestFailureOnOtherBackupMarksItExpunged() {
        doReturn(backupId).when(internalBackupJoinVoMock).getId();

        BackupDeleteAnswer failedOtherBackupAnswer = Mockito.mock(BackupDeleteAnswer.class);
        doReturn(false).when(failedOtherBackupAnswer).getResult();
        doReturn(200L).when(failedOtherBackupAnswer).getBackupId();
        doReturn("delete failed").when(failedOtherBackupAnswer).getDetails();

        BackupVO failedBackup = Mockito.mock(BackupVO.class);
        doReturn(failedBackup).when(backupDaoMock).findByIdIncludingRemoved(200L);

        List<Long> removedBackupIds = new ArrayList<>(List.of(backupId, 200L));

        boolean result = kbossBackupProviderSpy.processRemoveBackupFailures(false, new Answer[]{failedOtherBackupAnswer}, removedBackupIds, internalBackupJoinVoMock);

        assertFalse(result);
        assertEquals(List.of(backupId), removedBackupIds);
        verify(failedBackup).setStatus(Backup.Status.Expunged);
        verify(backupDaoMock).update(200L, failedBackup);
        verify(backupDaoMock, never()).findByIdIncludingRemoved(backupId);
    }

    @Test
    public void processValidationAnswerTestNullAnswer() {
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), any());

        boolean result = kbossBackupProviderSpy.processValidationAnswer(null, backupVoMock, userVmVOMock, hostVOMock, null);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("Null answer from host"));
    }

    @Test
    public void processValidationAnswerTestFalseAnswer() {
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), any());

        doReturn(false).when(answerMock).getResult();
        doReturn("fail-reason").when(answerMock).getDetails();
        boolean result = kbossBackupProviderSpy.processValidationAnswer(answerMock, backupVoMock, userVmVOMock, hostVOMock, null);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("fail-reason"));
    }

    @Test
    public void processValidationAnswerTestBootNotValidated() {
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), any());

        ValidateKbossVmAnswer answer = Mockito.mock(ValidateKbossVmAnswer.class);
        doReturn(true).when(answer).getResult();
        doReturn(false).when(answer).isBootValidated();

        ValidateKbossVmCommand cmd = Mockito.mock(ValidateKbossVmCommand.class);
        doReturn(true).when(cmd).isWaitForBoot();

        boolean result = kbossBackupProviderSpy.processValidationAnswer(answer, backupVoMock, userVmVOMock, hostVOMock, cmd);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("The VM did not boot within the expected time"));
    }

    @Test
    public void processValidationAnswerTestBootNotValidatedScriptResultFalse() {
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), any());

        ValidateKbossVmAnswer answer = Mockito.mock(ValidateKbossVmAnswer.class);
        doReturn(true).when(answer).getResult();
        doReturn(false).when(answer).isBootValidated();
        doReturn("false").when(answer).getScriptResult();

        ValidateKbossVmCommand cmd = Mockito.mock(ValidateKbossVmCommand.class);
        doReturn(true).when(cmd).isWaitForBoot();
        doReturn(true).when(cmd).isExecuteScript();

        boolean result = kbossBackupProviderSpy.processValidationAnswer(answer, backupVoMock, userVmVOMock, hostVOMock, cmd);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("The VM did not boot within the expected time"));
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("The script did not output the expected output."));
    }

    @Test
    public void processValidationAnswerTestBootNotValidatedScriptResultFalseScreenshotPathNull() {
        doNothing().when(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), any());

        ValidateKbossVmAnswer answer = Mockito.mock(ValidateKbossVmAnswer.class);
        doReturn(true).when(answer).getResult();
        doReturn(false).when(answer).isBootValidated();
        doReturn("false").when(answer).getScriptResult();
        doReturn(null).when(answer).getScreenshotPath();

        ValidateKbossVmCommand cmd = Mockito.mock(ValidateKbossVmCommand.class);
        doReturn(true).when(cmd).isWaitForBoot();
        doReturn(true).when(cmd).isExecuteScript();
        doReturn(true).when(cmd).isTakeScreenshot();

        boolean result = kbossBackupProviderSpy.processValidationAnswer(answer, backupVoMock, userVmVOMock, hostVOMock, cmd);

        assertFalse(result);
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("The VM did not boot within the expected time"));
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("The script did not output the expected output."));
        verify(kbossBackupProviderSpy).setBackupAsInvalidAndSendAlert(eq(backupVoMock), contains("We were unable to take a screenshot of the VM."));
    }

    @Test
    public void processValidationAnswerTestBootValidatedScriptResultTrueScreenshotPathNotNull() {
        ValidateKbossVmAnswer answer = Mockito.mock(ValidateKbossVmAnswer.class);
        doReturn(true).when(answer).getResult();
        doReturn(true).when(answer).isBootValidated();
        doReturn(null).when(answer).getScriptResult();
        doReturn("snap-path").when(answer).getScreenshotPath();

        ValidateKbossVmCommand cmd = Mockito.mock(ValidateKbossVmCommand.class);
        doReturn(true).when(cmd).isWaitForBoot();
        doReturn(true).when(cmd).isExecuteScript();
        doReturn(true).when(cmd).isTakeScreenshot();

        boolean result = kbossBackupProviderSpy.processValidationAnswer(answer, backupVoMock, userVmVOMock, hostVOMock, cmd);

        assertTrue(result);
        verify(kbossBackupProviderSpy, never()).setBackupAsInvalidAndSendAlert(any(), any());
        verify(backupDetailDaoMock).addDetail(eq(backupId), eq(BackupDetailsDao.SCREENSHOT_PATH), eq("snap-path"), eq(false));
    }

    @Test
    public void getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommandsTestNoParents() {
        doReturn(List.of()).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);

        Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> result =
                kbossBackupProviderSpy.getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVoMock, new Commands(Command.OnError.Stop));

        assertEquals(List.of(), result.first());
        assertNull(result.second());
    }

    @Test
    public void getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommandsTestAliveParents() {
        doReturn(List.of(internalBackupJoinVoMock)).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock).getStatus();

        Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> result =
                kbossBackupProviderSpy.getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVoMock, new Commands(Command.OnError.Stop));

        assertEquals(List.of(), result.first());
        assertEquals(internalBackupJoinVoMock, result.second());
    }

    @Test
    public void getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommandsTestDeadParents() {
        InternalBackupJoinVO deadParent = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.Removed).when(deadParent).getStatus();
        doReturn(backupId).when(deadParent).getId();

        doReturn(List.of(deadParent)).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        Commands commands = new Commands(Command.OnError.Stop);

        Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> result =
                kbossBackupProviderSpy.getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVoMock, commands);

        assertEquals(List.of(deadParent), result.first());
        assertNull(result.second());
        verify(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(backupId, commands);
    }

    @Test
    public void getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommandsTestDeadAndAliveParents() {
        InternalBackupJoinVO deadParent = Mockito.mock(InternalBackupJoinVO.class);
        doReturn(Backup.Status.Removed).when(deadParent).getStatus();
        doReturn(backupId).when(deadParent).getId();

        doReturn(List.of(deadParent, internalBackupJoinVoMock)).when(kbossBackupProviderSpy).getBackupJoinParents(backupVoMock, true);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock).getStatus();
        doReturn(null).when(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(anyLong(), any());
        Commands commands = new Commands(Command.OnError.Stop);

        Pair<List<InternalBackupJoinVO>, InternalBackupJoinVO> result =
                kbossBackupProviderSpy.getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVoMock, commands);

        assertEquals(List.of(deadParent), result.first());
        assertEquals(internalBackupJoinVoMock, result.second());
        verify(kbossBackupProviderSpy).addBackupDeltasToDeleteCommand(backupId, commands);
    }

    @Test
    public void configureValidationStepsTestScreenshot() {
        long backupOfferingId = 32L;

        doReturn(backupOfferingId).when(backupVoMock).getBackupOfferingId();
        doReturn(backupOfferingId).when(backupOfferingMock).getId();
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(backupOfferingId);
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(backupOfferingId, ApiConstants.VALIDATION_STEPS);
        doReturn("screenshot").when(backupOfferingDetailsVoMock).getValue();

        ValidateKbossVmCommand cmd = new ValidateKbossVmCommand(null, null);

        kbossBackupProviderSpy.configureValidationSteps(cmd, backupVoMock);

        assertTrue(cmd.isTakeScreenshot());
        assertFalse(cmd.isWaitForBoot());
        assertFalse(cmd.isExecuteScript());
    }

    @Test
    public void configureValidationStepsTestWaitForBoot() {
        long backupOfferingId = 32L;

        doReturn(backupOfferingId).when(backupVoMock).getBackupOfferingId();
        doReturn(backupOfferingId).when(backupOfferingMock).getId();
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(backupOfferingId);
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(backupOfferingId, ApiConstants.VALIDATION_STEPS);
        doReturn("wait_for_boot").when(backupOfferingDetailsVoMock).getValue();

        ValidateKbossVmCommand cmd = new ValidateKbossVmCommand(null, null);

        kbossBackupProviderSpy.configureValidationSteps(cmd, backupVoMock);

        assertFalse(cmd.isTakeScreenshot());
        assertTrue(cmd.isWaitForBoot());
        assertFalse(cmd.isExecuteScript());
    }

    @Test
    public void configureValidationStepsTestExecuteCommand() {
        long backupOfferingId = 32L;

        doReturn(backupOfferingId).when(backupVoMock).getBackupOfferingId();
        doReturn(backupOfferingId).when(backupOfferingMock).getId();
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(backupOfferingId);
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(backupOfferingId, ApiConstants.VALIDATION_STEPS);
        doReturn("execute_command").when(backupOfferingDetailsVoMock).getValue();
        doReturn(vmInstanceDetailVoMock).when(vmInstanceDetailsDaoMock).findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND);
        doReturn(vmId).when(backupVoMock).getVmId();

        ValidateKbossVmCommand cmd = new ValidateKbossVmCommand(null, null);

        kbossBackupProviderSpy.configureValidationSteps(cmd, backupVoMock);

        assertFalse(cmd.isTakeScreenshot());
        assertFalse(cmd.isWaitForBoot());
        assertTrue(cmd.isExecuteScript());
    }

    @Test
    public void configureValidationStepsTestAllSteps() {
        long backupOfferingId = 32L;

        doReturn(backupOfferingId).when(backupVoMock).getBackupOfferingId();
        doReturn(backupOfferingId).when(backupOfferingMock).getId();
        doReturn(backupOfferingMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(backupOfferingId);
        doReturn(backupOfferingDetailsVoMock).when(backupOfferingDetailsDaoMock).findDetail(backupOfferingId, ApiConstants.VALIDATION_STEPS);
        doReturn("screenshot,wait_for_boot,execute_command").when(backupOfferingDetailsVoMock).getValue();
        doReturn(vmInstanceDetailVoMock).when(vmInstanceDetailsDaoMock).findDetail(vmId, VmDetailConstants.VALIDATION_COMMAND);
        doReturn(vmId).when(backupVoMock).getVmId();

        ValidateKbossVmCommand cmd = new ValidateKbossVmCommand(null, null);

        kbossBackupProviderSpy.configureValidationSteps(cmd, backupVoMock);

        assertTrue(cmd.isTakeScreenshot());
        assertTrue(cmd.isWaitForBoot());
        assertTrue(cmd.isExecuteScript());
    }

    @Test
    public void validateCompressionStateForRestoreAndGetBackupTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateCompressionStateForRestoreAndGetBackup(backupId);

        assertFalse(result.first());
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateCompressionStateForRestoreAndGetBackupTestFinalizingCompression() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.CompressionStatus.FinalizingCompression).when(backupVoMock).getCompressionStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateCompressionStateForRestoreAndGetBackup(backupId);

        assertFalse(result.first());
        verify(backupVoMock).getCompressionStatus();
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateCompressionStateForRestoreAndGetBackupTestValidState() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.CompressionStatus.Compressed).when(backupVoMock).getCompressionStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateCompressionStateForRestoreAndGetBackup(backupId);

        assertTrue(result.first());
        assertEquals(backupVoMock, result.second());
        verify(backupVoMock).getCompressionStatus();
        verify(backupVoMock).setStatus(Backup.Status.Restoring);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRemovalTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        boolean result = kbossBackupProviderSpy.validateBackupStateForRemoval(backupId);

        assertFalse(result);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRemovalTestInvalidState() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Restoring).when(backupVoMock).getStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForRemoval(backupId);

        assertFalse(result);
        verify(backupVoMock, Mockito.atLeast(1)).getStatus();
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRemovalTestCompressing() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(Backup.CompressionStatus.Compressing).when(backupVoMock).getCompressionStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForRemoval(backupId);

        assertFalse(result);
        verify(backupVoMock).getCompressionStatus();
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRemovalTestValidating() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(Backup.CompressionStatus.Compressed).when(backupVoMock).getCompressionStatus();
        doReturn(Backup.ValidationStatus.Validating).when(backupVoMock).getValidationStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForRemoval(backupId);

        assertFalse(result);
        verify(backupVoMock).getValidationStatus();
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRemovalTestValidStates() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(Backup.CompressionStatus.Compressed).when(backupVoMock).getCompressionStatus();
        doReturn(Backup.ValidationStatus.UnableToValidate).when(backupVoMock).getValidationStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForRemoval(backupId);

        assertTrue(result);
        verify(backupVoMock).getStatus();
        verify(backupVoMock).getValidationStatus();
        verify(backupVoMock).getCompressionStatus();
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForStartCompressionAndUpdateCompressionStatusTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForStartCompressionAndUpdateCompressionStatusTestInvalidState() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Error).when(backupVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);

        assertFalse(result.first());
        verify(backupVoMock, Mockito.atLeastOnce()).getStatus();
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForStartCompressionAndUpdateCompressionStatusTestValidStates() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForStartCompressionAndUpdateCompressionStatus(backupId);

        assertTrue(result.first());
        assertEquals(backupVoMock, result.second());
        verify(backupVoMock).getStatus();
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.Compressing);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForFinalizeCompressionTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForFinalizeCompression(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForFinalizeCompressionTestRestoringBackup() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Restoring).when(backupVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForFinalizeCompression(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForFinalizeCompressionTestRestoringChildBackup() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(List.of(internalBackupJoinVoMock)).when(kbossBackupProviderSpy).getBackupJoinChildren(backupVoMock);
        doReturn(Backup.Status.Restoring).when(internalBackupJoinVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForFinalizeCompression(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForFinalizeCompressionTestAllBackedUp() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();
        doReturn(List.of(internalBackupJoinVoMock)).when(kbossBackupProviderSpy).getBackupJoinChildren(backupVoMock);
        doReturn(Backup.Status.BackedUp).when(internalBackupJoinVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForFinalizeCompression(backupId);

        assertTrue(result.first());
        assertEquals(backupVoMock, result.second());
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.FinalizingCompression);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForFinalizeCompressionTestRemovedBackup() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Removed).when(backupVoMock).getStatus();

        Pair<Boolean, BackupVO> result = kbossBackupProviderSpy.validateBackupStateForFinalizeCompression(backupId);

        assertTrue(result.first());
        assertEquals(backupVoMock, result.second());
        verify(backupVoMock).setCompressionStatus(Backup.CompressionStatus.CompressionError);
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRestoreBackupToVMTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        Pair<Boolean, Backup.Status> result = kbossBackupProviderSpy.validateBackupStateForRestoreBackupToVM(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRestoreBackupToVMTestBackedUp() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();

        Pair<Boolean, Backup.Status> result = kbossBackupProviderSpy.validateBackupStateForRestoreBackupToVM(backupId);

        assertTrue(result.first());
        assertEquals(Backup.Status.BackedUp, result.second());
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRestoreBackupToVMTestRestoring() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Restoring).when(backupVoMock).getStatus();

        Pair<Boolean, Backup.Status> result = kbossBackupProviderSpy.validateBackupStateForRestoreBackupToVM(backupId);

        assertTrue(result.first());
        assertEquals(Backup.Status.Restoring, result.second());
        verify(backupDaoMock).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForRestoreBackupToVMTestError() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Error).when(backupVoMock).getStatus();

        Pair<Boolean, Backup.Status> result = kbossBackupProviderSpy.validateBackupStateForRestoreBackupToVM(backupId);

        assertFalse(result.first());
        verify(backupDaoMock, never()).update(backupId, backupVoMock);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }


    @Test
    public void validateBackupStateForValidationTestUnableToLock() {
        doReturn(null).when(kbossBackupProviderSpy).lockBackup(backupId);

        boolean result = kbossBackupProviderSpy.validateBackupStateForValidation(backupId);

        assertFalse(result);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForValidationTestInvalidState() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.Removed).when(backupVoMock).getStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForValidation(backupId);

        assertFalse(result);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

    @Test
    public void validateBackupStateForValidationTestValidState() {
        doReturn(backupVoMock).when(kbossBackupProviderSpy).lockBackup(backupId);
        doReturn(Backup.Status.BackedUp).when(backupVoMock).getStatus();

        boolean result = kbossBackupProviderSpy.validateBackupStateForValidation(backupId);

        assertTrue(result);
        verify(kbossBackupProviderSpy).releaseBackup(backupId);
    }

}
