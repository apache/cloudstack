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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.FreezeThawVMAnswer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

/**
 * Unit tests for {@link OntapVMSnapshotStrategy}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>canHandle(VMSnapshot) — various conditions for Allocated and non-Allocated states</li>
 *   <li>canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory) — allocation-phase checks</li>
 *   <li>takeVMSnapshot — state transition failure scenarios</li>
 *   <li>Freeze/thaw behavior (freeze success/failure, thaw success/failure, agent errors)</li>
 *   <li>Quiesce behavior (honors user input; freeze/thaw only when quiesce=true)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OntapVMSnapshotStrategyTest {

    private static final long VM_ID = 100L;
    private static final long HOST_ID = 10L;
    private static final long SNAPSHOT_ID = 200L;
    private static final long VOLUME_ID_1 = 301L;
    private static final long VOLUME_ID_2 = 302L;
    private static final long POOL_ID_1 = 401L;
    private static final long POOL_ID_2 = 402L;
    private static final long GUEST_OS_ID = 50L;
    private static final String VM_INSTANCE_NAME = "i-2-100-VM";
    private static final String VM_UUID = "vm-uuid-123";

    @Spy
    private OntapVMSnapshotStrategy strategy;

    @Mock
    private UserVmDao userVmDao;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private PrimaryDataStoreDao storagePool;
    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Mock
    private VMSnapshotHelper vmSnapshotHelper;
    @Mock
    private VMSnapshotDao vmSnapshotDao;
    @Mock
    private AgentManager agentMgr;
    @Mock
    private GuestOSDao guestOSDao;
    @Mock
    private VolumeDataFactory volumeDataFactory;
    @Mock
    private VolumeDetailsDao volumeDetailsDao;

    @BeforeEach
    void setUp() throws Exception {
        // Inject mocks into the inherited fields via reflection
        // DefaultVMSnapshotStrategy fields
        setField(strategy, DefaultVMSnapshotStrategy.class, "vmSnapshotHelper", vmSnapshotHelper);
        setField(strategy, DefaultVMSnapshotStrategy.class, "guestOSDao", guestOSDao);
        setField(strategy, DefaultVMSnapshotStrategy.class, "userVmDao", userVmDao);
        setField(strategy, DefaultVMSnapshotStrategy.class, "vmSnapshotDao", vmSnapshotDao);
        setField(strategy, DefaultVMSnapshotStrategy.class, "agentMgr", agentMgr);
        setField(strategy, DefaultVMSnapshotStrategy.class, "volumeDao", volumeDao);

        // StorageVMSnapshotStrategy fields
        setField(strategy, StorageVMSnapshotStrategy.class, "storagePool", storagePool);
        setField(strategy, StorageVMSnapshotStrategy.class, "vmSnapshotDetailsDao", vmSnapshotDetailsDao);
        setField(strategy, StorageVMSnapshotStrategy.class, "volumeDataFactory", volumeDataFactory);

        // OntapVMSnapshotStrategy fields
        setField(strategy, OntapVMSnapshotStrategy.class, "storagePoolDetailsDao", storagePoolDetailsDao);
        setField(strategy, OntapVMSnapshotStrategy.class, "volumeDetailsDao", volumeDetailsDao);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: inject field via reflection into a specific declaring class
    // ──────────────────────────────────────────────────────────────────────────

    private void setField(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: create common mocks
    // ──────────────────────────────────────────────────────────────────────────

    private UserVmVO createMockUserVm(Hypervisor.HypervisorType hypervisorType, VirtualMachine.State state) {
        UserVmVO userVm = mock(UserVmVO.class);
        when(userVm.getHypervisorType()).thenReturn(hypervisorType);
        when(userVm.getState()).thenReturn(state);
        return userVm;
    }

    private VolumeVO createMockVolume(long volumeId, long poolId) {
        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getId()).thenReturn(volumeId);
        when(volume.getPoolId()).thenReturn(poolId);
        return volume;
    }

    private StoragePoolVO createOntapManagedPool(long poolId) {
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn(Constants.ONTAP_PLUGIN_NAME);
        return pool;
    }

    private VMSnapshotVO createMockVmSnapshot(VMSnapshot.State state, VMSnapshot.Type type) {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshot.getId()).thenReturn(SNAPSHOT_ID);
        when(vmSnapshot.getVmId()).thenReturn(VM_ID);
        when(vmSnapshot.getState()).thenReturn(state);
        lenient().when(vmSnapshot.getType()).thenReturn(type);
        return vmSnapshot;
    }

    private void setupAllVolumesOnOntap() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol1 = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        VolumeVO vol2 = createMockVolume(VOLUME_ID_2, POOL_ID_2);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Arrays.asList(vol1, vol2));

        StoragePoolVO pool1 = createOntapManagedPool(POOL_ID_1);
        StoragePoolVO pool2 = createOntapManagedPool(POOL_ID_2);
        when(storagePool.findById(POOL_ID_1)).thenReturn(pool1);
        when(storagePool.findById(POOL_ID_2)).thenReturn(pool2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: canHandle(VMSnapshot)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testCanHandle_AllocatedDiskType_AllVolumesOnOntap_ReturnsHighest() {
        setupAllVolumesOnOntap();
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    @Test
    void testCanHandle_AllocatedDiskAndMemoryType_ReturnsCantHandle() {
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.DiskAndMemory);
        when(vmSnapshot.getVmId()).thenReturn(VM_ID);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VmNotFound_ReturnsCantHandle() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VmxenHypervisor_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.XenServer, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VmNotRunning_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Stopped);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_NoVolumes_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.emptyList());
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VolumeOnNonManagedPool_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.singletonList(vol));

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(false);
        when(pool.getName()).thenReturn("non-managed-pool");
        when(storagePool.findById(POOL_ID_1)).thenReturn(pool);

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VolumeOnNonOntapManagedPool_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.singletonList(vol));

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn("SolidFire");
        when(pool.getName()).thenReturn("solidfire-pool");
        when(storagePool.findById(POOL_ID_1)).thenReturn(pool);

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_VolumeWithNullPoolId_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getPoolId()).thenReturn(null);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.singletonList(vol));

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_AllocatedDiskType_PoolNotFound_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.singletonList(vol));
        when(storagePool.findById(POOL_ID_1)).thenReturn(null);

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_NonAllocated_HasFlexVolSnapshotDetails_AllOnOntap_ReturnsHighest() {
        setupAllVolumesOnOntap();
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Ready, VMSnapshot.Type.Disk);

        List<VMSnapshotDetailsVO> details = new ArrayList<>();
        details.add(new VMSnapshotDetailsVO(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT,
                "flex-uuid::snap-uuid::vmsnap_200_123::401", true));
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT)).thenReturn(details);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    @Test
    void testCanHandle_NonAllocated_HasLegacyStorageSnapshotDetails_AllOnOntap_ReturnsHighest() {
        setupAllVolumesOnOntap();
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Ready, VMSnapshot.Type.Disk);

        // No FlexVol details
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT)).thenReturn(Collections.emptyList());
        // Has legacy details
        List<VMSnapshotDetailsVO> details = new ArrayList<>();
        details.add(new VMSnapshotDetailsVO(SNAPSHOT_ID, "kvmStorageSnapshot", "123", true));
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, "kvmStorageSnapshot")).thenReturn(details);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    @Test
    void testCanHandle_NonAllocated_NoDetails_ReturnsCantHandle() {
        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Ready, VMSnapshot.Type.Disk);
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT)).thenReturn(Collections.emptyList());
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, "kvmStorageSnapshot")).thenReturn(Collections.emptyList());

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_NonAllocated_HasFlexVolDetails_NotOnOntap_ReturnsCantHandle() {
        // VM has FlexVol details but volumes are now on non-ONTAP storage
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Collections.singletonList(vol));

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(false);
        when(pool.getName()).thenReturn("other-pool");
        when(storagePool.findById(POOL_ID_1)).thenReturn(pool);

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Ready, VMSnapshot.Type.Disk);
        List<VMSnapshotDetailsVO> flexVolDetails = new ArrayList<>();
        flexVolDetails.add(new VMSnapshotDetailsVO(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT,
                "flex-uuid::snap-uuid::vmsnap_200_123::401", true));
        when(vmSnapshotDetailsDao.findDetails(SNAPSHOT_ID, Constants.ONTAP_FLEXVOL_SNAPSHOT)).thenReturn(flexVolDetails);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandle_MixedPools_OneOntapOneNot_ReturnsCantHandle() {
        UserVmVO userVm = createMockUserVm(Hypervisor.HypervisorType.KVM, VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        VolumeVO vol1 = createMockVolume(VOLUME_ID_1, POOL_ID_1);
        VolumeVO vol2 = createMockVolume(VOLUME_ID_2, POOL_ID_2);
        when(volumeDao.findByInstance(VM_ID)).thenReturn(Arrays.asList(vol1, vol2));

        StoragePoolVO ontapPool = createOntapManagedPool(POOL_ID_1);
        StoragePoolVO otherPool = mock(StoragePoolVO.class);
        when(otherPool.isManaged()).thenReturn(true);
        when(otherPool.getStorageProviderName()).thenReturn("SolidFire");
        when(otherPool.getName()).thenReturn("sf-pool");
        when(storagePool.findById(POOL_ID_1)).thenReturn(ontapPool);
        when(storagePool.findById(POOL_ID_2)).thenReturn(otherPool);

        VMSnapshotVO vmSnapshot = createMockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testCanHandleByVmId_MemorySnapshot_ReturnsCantHandle() {
        StrategyPriority result = strategy.canHandle(VM_ID, POOL_ID_1, true);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    void testCanHandleByVmId_DiskOnly_AllOnOntap_ReturnsHighest() {
        setupAllVolumesOnOntap();

        StrategyPriority result = strategy.canHandle(VM_ID, POOL_ID_1, false);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    @Test
    void testCanHandleByVmId_DiskOnly_NotOnOntap_ReturnsCantHandle() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);

        StrategyPriority result = strategy.canHandle(VM_ID, POOL_ID_1, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: groupVolumesByFlexVol
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testGroupVolumesByFlexVol_SingleFlexVol_TwoVolumes() {
        VolumeObjectTO volumeTO1 = mock(VolumeObjectTO.class);
        when(volumeTO1.getId()).thenReturn(VOLUME_ID_1);
        VolumeObjectTO volumeTO2 = mock(VolumeObjectTO.class);
        when(volumeTO2.getId()).thenReturn(VOLUME_ID_2);

        VolumeVO vol1 = mock(VolumeVO.class);
        when(vol1.getId()).thenReturn(VOLUME_ID_1);
        when(vol1.getPoolId()).thenReturn(POOL_ID_1);
        VolumeVO vol2 = mock(VolumeVO.class);
        when(vol2.getId()).thenReturn(VOLUME_ID_2);
        when(vol2.getPoolId()).thenReturn(POOL_ID_1); // same pool → same FlexVol
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(vol1);
        when(volumeDao.findById(VOLUME_ID_2)).thenReturn(vol2);

        Map<String, String> poolDetails = new HashMap<>();
        poolDetails.put(Constants.VOLUME_UUID, "flexvol-uuid-1");
        when(storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID_1)).thenReturn(poolDetails);

        Map<String, OntapVMSnapshotStrategy.FlexVolGroupInfo> groups =
                strategy.groupVolumesByFlexVol(Arrays.asList(volumeTO1, volumeTO2));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get("flexvol-uuid-1").volumeIds.size());
    }

    @Test
    void testGroupVolumesByFlexVol_TwoFlexVols() {
        VolumeObjectTO volumeTO1 = mock(VolumeObjectTO.class);
        when(volumeTO1.getId()).thenReturn(VOLUME_ID_1);
        VolumeObjectTO volumeTO2 = mock(VolumeObjectTO.class);
        when(volumeTO2.getId()).thenReturn(VOLUME_ID_2);

        VolumeVO vol1 = mock(VolumeVO.class);
        when(vol1.getId()).thenReturn(VOLUME_ID_1);
        when(vol1.getPoolId()).thenReturn(POOL_ID_1);
        VolumeVO vol2 = mock(VolumeVO.class);
        when(vol2.getId()).thenReturn(VOLUME_ID_2);
        when(vol2.getPoolId()).thenReturn(POOL_ID_2); // different pool → different FlexVol
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(vol1);
        when(volumeDao.findById(VOLUME_ID_2)).thenReturn(vol2);

        Map<String, String> poolDetails1 = new HashMap<>();
        poolDetails1.put(Constants.VOLUME_UUID, "flexvol-uuid-1");
        Map<String, String> poolDetails2 = new HashMap<>();
        poolDetails2.put(Constants.VOLUME_UUID, "flexvol-uuid-2");
        when(storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID_1)).thenReturn(poolDetails1);
        when(storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID_2)).thenReturn(poolDetails2);

        Map<String, OntapVMSnapshotStrategy.FlexVolGroupInfo> groups =
                strategy.groupVolumesByFlexVol(Arrays.asList(volumeTO1, volumeTO2));

        assertEquals(2, groups.size());
        assertEquals(1, groups.get("flexvol-uuid-1").volumeIds.size());
        assertEquals(1, groups.get("flexvol-uuid-2").volumeIds.size());
    }

    @Test
    void testGroupVolumesByFlexVol_MissingFlexVolUuid_ThrowsException() {
        VolumeObjectTO volumeTO1 = mock(VolumeObjectTO.class);
        when(volumeTO1.getId()).thenReturn(VOLUME_ID_1);

        VolumeVO vol1 = mock(VolumeVO.class);
        when(vol1.getId()).thenReturn(VOLUME_ID_1);
        when(vol1.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(vol1);

        Map<String, String> poolDetails = new HashMap<>();
        // No VOLUME_UUID key
        when(storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID_1)).thenReturn(poolDetails);

        assertThrows(CloudRuntimeException.class,
                () -> strategy.groupVolumesByFlexVol(Collections.singletonList(volumeTO1)));
    }

    @Test
    void testGroupVolumesByFlexVol_VolumeNotFound_ThrowsException() {
        VolumeObjectTO volumeTO1 = mock(VolumeObjectTO.class);
        when(volumeTO1.getId()).thenReturn(VOLUME_ID_1);
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> strategy.groupVolumesByFlexVol(Collections.singletonList(volumeTO1)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: FlexVolSnapshotDetail parse/toString
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testFlexVolSnapshotDetail_ParseAndToString_NewFormat() {
        String value = "flexvol-uuid-1::snap-uuid-1::vmsnap_200_1234567890::root-disk.qcow2::401::NFS3";
        OntapVMSnapshotStrategy.FlexVolSnapshotDetail detail =
                OntapVMSnapshotStrategy.FlexVolSnapshotDetail.parse(value);

        assertEquals("flexvol-uuid-1", detail.flexVolUuid);
        assertEquals("snap-uuid-1", detail.snapshotUuid);
        assertEquals("vmsnap_200_1234567890", detail.snapshotName);
        assertEquals("root-disk.qcow2", detail.volumePath);
        assertEquals(401L, detail.poolId);
        assertEquals("NFS3", detail.protocol);
        assertEquals(value, detail.toString());
    }

    @Test
    void testFlexVolSnapshotDetail_ParseLegacy4FieldFormat() {
        // Legacy format without volumePath and protocol
        String value = "flexvol-uuid-1::snap-uuid-1::vmsnap_200_1234567890::401";
        OntapVMSnapshotStrategy.FlexVolSnapshotDetail detail =
                OntapVMSnapshotStrategy.FlexVolSnapshotDetail.parse(value);

        assertEquals("flexvol-uuid-1", detail.flexVolUuid);
        assertEquals("snap-uuid-1", detail.snapshotUuid);
        assertEquals("vmsnap_200_1234567890", detail.snapshotName);
        assertEquals(null, detail.volumePath);
        assertEquals(401L, detail.poolId);
        assertEquals(null, detail.protocol);
    }

    @Test
    void testFlexVolSnapshotDetail_ParseInvalidFormat_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
                () -> OntapVMSnapshotStrategy.FlexVolSnapshotDetail.parse("invalid-format"));
    }

    @Test
    void testFlexVolSnapshotDetail_ParseTooFewParts_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
                () -> OntapVMSnapshotStrategy.FlexVolSnapshotDetail.parse("a::b::c"));
    }

    @Test
    void testFlexVolSnapshotDetail_Parse5Parts_ThrowsException() {
        // 5 parts is neither legacy (4) nor current (6) format
        assertThrows(CloudRuntimeException.class,
                () -> OntapVMSnapshotStrategy.FlexVolSnapshotDetail.parse("a::b::c::d::e"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: buildSnapshotName
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testBuildSnapshotName_Format() {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshot.getId()).thenReturn(SNAPSHOT_ID);

        String name = strategy.buildSnapshotName(vmSnapshot);

        assertEquals(true, name.startsWith("vmsnap_200_"));
        assertEquals(true, name.length() <= Constants.MAX_SNAPSHOT_NAME_LENGTH);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: resolveVolumePathOnOntap
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testResolveVolumePathOnOntap_NFS_ReturnsVolumePath() {
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getPath()).thenReturn("abc123-def456.qcow2");
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(vol);

        String path = strategy.resolveVolumePathOnOntap(VOLUME_ID_1, "NFS3", new HashMap<>());

        assertEquals("abc123-def456.qcow2", path);
    }

    @Test
    void testResolveVolumePathOnOntap_ISCSI_ReturnsLunName() {
        VolumeDetailVO lunDetail = mock(VolumeDetailVO.class);
        when(lunDetail.getValue()).thenReturn("/vol/vol1/lun_301");
        when(volumeDetailsDao.findDetail(VOLUME_ID_1, Constants.LUN_DOT_NAME)).thenReturn(lunDetail);

        String path = strategy.resolveVolumePathOnOntap(VOLUME_ID_1, "ISCSI", new HashMap<>());

        assertEquals("/vol/vol1/lun_301", path);
    }

    @Test
    void testResolveVolumePathOnOntap_ISCSI_NoLunDetail_ThrowsException() {
        when(volumeDetailsDao.findDetail(VOLUME_ID_1, Constants.LUN_DOT_NAME)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> strategy.resolveVolumePathOnOntap(VOLUME_ID_1, "ISCSI", new HashMap<>()));
    }

    @Test
    void testResolveVolumePathOnOntap_NFS_VolumeNotFound_ThrowsException() {
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> strategy.resolveVolumePathOnOntap(VOLUME_ID_1, "NFS3", new HashMap<>()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: takeVMSnapshot — State transitions & Freeze/Thaw
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testTakeVMSnapshot_StateTransitionFails_ThrowsCloudRuntimeException() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        when(vmSnapshotHelper.pickRunningHost(VM_ID)).thenReturn(HOST_ID);
        UserVmVO userVm = mock(UserVmVO.class);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        // State transition fails
        doThrow(new NoTransitionException("Cannot transition")).when(vmSnapshotHelper)
                .vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.CreateRequested);

        assertThrows(CloudRuntimeException.class, () -> strategy.takeVMSnapshot(vmSnapshot));
    }

    @Test
    void testTakeVMSnapshot_FreezeFailure_ThrowsException() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        // Freeze failure
        FreezeThawVMAnswer freezeAnswer = mock(FreezeThawVMAnswer.class);
        when(freezeAnswer.getResult()).thenReturn(false);
        when(freezeAnswer.getDetails()).thenReturn("qemu-guest-agent not responding");
        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class))).thenReturn(freezeAnswer);

        // Cleanup mocks for finally block
        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> strategy.takeVMSnapshot(vmSnapshot));

        assertEquals(true, ex.getMessage().contains("Could not freeze VM"));
        assertEquals(true, ex.getMessage().contains("qemu-guest-agent"));
    }

    @Test
    void testTakeVMSnapshot_FreezeReturnsNull_ThrowsException() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        // Freeze returns null
        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class))).thenReturn(null);

        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        assertThrows(CloudRuntimeException.class, () -> strategy.takeVMSnapshot(vmSnapshot));
    }

    @Test
    void testTakeVMSnapshot_AgentUnavailable_ThrowsCloudRuntimeException() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class)))
                .thenThrow(new AgentUnavailableException(HOST_ID));

        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> strategy.takeVMSnapshot(vmSnapshot));
        assertEquals(true, ex.getMessage().contains("failed"));
    }

    @Test
    void testTakeVMSnapshot_OperationTimeout_ThrowsCloudRuntimeException() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class)))
                .thenThrow(new OperationTimedoutException(null, 0, 0, 0, false));

        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> strategy.takeVMSnapshot(vmSnapshot));
        assertEquals(true, ex.getMessage().contains("timed out"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: Quiesce Behavior
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testTakeVMSnapshot_QuiesceFalse_SkipsFreezeThaw() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        // Explicitly set quiesce to false
        VMSnapshotOptions options = mock(VMSnapshotOptions.class);
        when(options.needQuiesceVM()).thenReturn(false);
        when(vmSnapshot.getOptions()).thenReturn(options);

        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        // The FlexVolume snapshot flow will try to call Utility.getStrategyByStoragePoolDetails
        // which is a static method that makes real connections. We expect this to fail in unit tests.
        // The important thing is that freeze/thaw was NOT called before the failure.
        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        // Since Utility.getStrategyByStoragePoolDetails is static and creates real Feign clients,
        // this will fail. We just verify that freeze was never called.
        try {
            strategy.takeVMSnapshot(vmSnapshot);
        } catch (Exception e) {
            // Expected — static utility can't be mocked in unit test
        }

        // No freeze/thaw commands should be sent when quiesce is false
        verify(agentMgr, never()).send(eq(HOST_ID), any(FreezeThawVMCommand.class));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests: Parent snapshot chain
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void testTakeVMSnapshot_WithParentSnapshot_SetsParentId() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        // Has a current (parent) snapshot
        VMSnapshotVO currentSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshotDao.findCurrentSnapshotByVmId(VM_ID)).thenReturn(currentSnapshot);
        VMSnapshotTO parentTO = mock(VMSnapshotTO.class);
        when(parentTO.getId()).thenReturn(199L);
        when(vmSnapshotHelper.getSnapshotWithParents(currentSnapshot)).thenReturn(parentTO);

        // Freeze success (since quiesce=true by default)
        FreezeThawVMAnswer freezeAnswer = mock(FreezeThawVMAnswer.class);
        when(freezeAnswer.getResult()).thenReturn(true);
        FreezeThawVMAnswer thawAnswer = mock(FreezeThawVMAnswer.class);
        when(thawAnswer.getResult()).thenReturn(true);
        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class)))
                .thenReturn(freezeAnswer)
                .thenReturn(thawAnswer);

        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        // FlexVol snapshot flow will fail on static method, but parent should already be set
        try {
            strategy.takeVMSnapshot(vmSnapshot);
        } catch (Exception e) {
            // Expected
        }

        // Verify parent was set on the VM snapshot before the FlexVol snapshot attempt
        verify(vmSnapshot).setParent(199L);
    }

    @Test
    void testTakeVMSnapshot_WithNoParentSnapshot_SetsParentNull() throws Exception {
        VMSnapshotVO vmSnapshot = createTakeSnapshotVmSnapshot();
        setupTakeSnapshotCommon(vmSnapshot);
        setupSingleVolumeForTakeSnapshot();

        when(vmSnapshotDao.findCurrentSnapshotByVmId(VM_ID)).thenReturn(null);

        FreezeThawVMAnswer freezeAnswer = mock(FreezeThawVMAnswer.class);
        when(freezeAnswer.getResult()).thenReturn(true);
        FreezeThawVMAnswer thawAnswer = mock(FreezeThawVMAnswer.class);
        when(thawAnswer.getResult()).thenReturn(true);
        when(agentMgr.send(eq(HOST_ID), any(FreezeThawVMCommand.class)))
                .thenReturn(freezeAnswer)
                .thenReturn(thawAnswer);

        when(vmSnapshotDetailsDao.listDetails(SNAPSHOT_ID)).thenReturn(Collections.emptyList());
        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(any(), eq(VMSnapshot.Event.OperationFailed));

        try {
            strategy.takeVMSnapshot(vmSnapshot);
        } catch (Exception e) {
            // Expected
        }

        verify(vmSnapshot).setParent(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: Set up common mocks for takeVMSnapshot tests
    // ──────────────────────────────────────────────────────────────────────────

    private VMSnapshotVO createTakeSnapshotVmSnapshot() {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshot.getId()).thenReturn(SNAPSHOT_ID);
        when(vmSnapshot.getVmId()).thenReturn(VM_ID);
        lenient().when(vmSnapshot.getName()).thenReturn("vm-snap-1");
        lenient().when(vmSnapshot.getType()).thenReturn(VMSnapshot.Type.Disk);
        lenient().when(vmSnapshot.getDescription()).thenReturn("Test ONTAP VM Snapshot");
        lenient().when(vmSnapshot.getOptions()).thenReturn(new VMSnapshotOptions(true));
        return vmSnapshot;
    }

    private UserVmVO setupTakeSnapshotCommon(VMSnapshotVO vmSnapshot) throws Exception {
        when(vmSnapshotHelper.pickRunningHost(VM_ID)).thenReturn(HOST_ID);

        UserVmVO userVm = mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(VM_ID);
        when(userVm.getGuestOSId()).thenReturn(GUEST_OS_ID);
        when(userVm.getInstanceName()).thenReturn(VM_INSTANCE_NAME);
        when(userVm.getUuid()).thenReturn(VM_UUID);
        when(userVm.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.findById(VM_ID)).thenReturn(userVm);

        GuestOSVO guestOS = mock(GuestOSVO.class);
        when(guestOS.getDisplayName()).thenReturn("CentOS 8");
        when(guestOSDao.findById(GUEST_OS_ID)).thenReturn(guestOS);

        when(vmSnapshotDao.findCurrentSnapshotByVmId(VM_ID)).thenReturn(null);

        doReturn(true).when(vmSnapshotHelper).vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.CreateRequested);

        return userVm;
    }

    private void setupSingleVolumeForTakeSnapshot() {
        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getId()).thenReturn(VOLUME_ID_1);
        when(volumeTO.getSize()).thenReturn(10737418240L);
        List<VolumeObjectTO> volumeTOs = Collections.singletonList(volumeTO);
        when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(volumeTOs);

        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getId()).thenReturn(VOLUME_ID_1);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID_1);
        when(volumeVO.getVmSnapshotChainSize()).thenReturn(null);
        when(volumeDao.findById(VOLUME_ID_1)).thenReturn(volumeVO);

        // Pool details for FlexVol grouping
        Map<String, String> poolDetails = new HashMap<>();
        poolDetails.put(Constants.VOLUME_UUID, "flexvol-uuid-1");
        poolDetails.put(Constants.USERNAME, "admin");
        poolDetails.put(Constants.PASSWORD, "pass");
        poolDetails.put(Constants.STORAGE_IP, "10.0.0.1");
        poolDetails.put(Constants.SVM_NAME, "svm1");
        poolDetails.put(Constants.SIZE, "107374182400");
        poolDetails.put(Constants.PROTOCOL, "NFS3");
        when(storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID_1)).thenReturn(poolDetails);

        VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getId()).thenReturn(VOLUME_ID_1);
        when(volumeInfo.getName()).thenReturn("vol-1");
        when(volumeDataFactory.getVolume(VOLUME_ID_1)).thenReturn(volumeInfo);
    }
}
