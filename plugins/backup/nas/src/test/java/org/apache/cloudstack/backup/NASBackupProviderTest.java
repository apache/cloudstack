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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.configuration.Resource;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

import com.google.gson.Gson;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

@RunWith(MockitoJUnitRunner.class)
public class NASBackupProviderTest {
    @Spy
    @InjectMocks
    private NASBackupProvider nasBackupProvider;

    @Mock
    private BackupDao backupDao;

    @Mock
    private BackupRepositoryDao backupRepositoryDao;

    @Mock
    private BackupOfferingDao backupOfferingDao;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private AgentManager agentManager;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private HostDao hostDao;

    @Mock
    private BackupManager backupManager;

    @Mock
    private ResourceManager resourceManager;

    @Mock
    private PrimaryDataStoreDao storagePoolDao;

    @Mock
    private VMSnapshotDao vmSnapshotDaoMock;

    @Mock
    private BackupDetailsDao backupDetailsDao;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    private DataStoreManager dataStoreMgr;

    @Mock
    private ResourceLimitService resourceLimitMgr;

    @Test
    public void testDeleteBackup() throws OperationTimedoutException, AgentUnavailableException {
        Long hostId = 1L;
        BackupVO backup = new BackupVO();
        backup.setBackupOfferingId(1L);
        backup.setVmId(1L);
        backup.setExternalId("externalId");
        ReflectionTestUtils.setField(backup, "id", 1L);

        BackupRepositoryVO backupRepository = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(1L)).thenReturn(backupRepository);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(1L)).thenReturn(vm);
        Mockito.when(agentManager.send(anyLong(), Mockito.any(DeleteBackupCommand.class))).thenReturn(new BackupAnswer(new DeleteBackupCommand(null, null, null, null), true, "details"));
        Mockito.when(backupDao.remove(1L)).thenReturn(true);

        boolean result = nasBackupProvider.deleteBackup(backup, true);
        Assert.assertTrue(result);
    }

    @Test
    public void testSyncBackupStorageStats() throws AgentUnavailableException, OperationTimedoutException {
        BackupRepositoryVO backupRepository = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);

        HostVO host = mock(HostVO.class);
        Mockito.when(resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, 1L)).thenReturn(host);

        Mockito.when(backupRepositoryDao.listByZoneAndProvider(1L, "nas")).thenReturn(Collections.singletonList(backupRepository));
        GetBackupStorageStatsCommand command = new GetBackupStorageStatsCommand("nfs", "address", "sync");
        BackupStorageStatsAnswer answer = new BackupStorageStatsAnswer(command, true, null);
        answer.setTotalSize(100L);
        answer.setUsedSize(50L);
        Mockito.when(agentManager.send(anyLong(), Mockito.any(GetBackupStorageStatsCommand.class))).thenReturn(answer);

        nasBackupProvider.syncBackupStorageStats(1L);
        Mockito.verify(backupRepositoryDao, Mockito.times(1)).updateCapacity(backupRepository, 100L, 50L);
    }

    @Test
    public void testListBackupOfferings() {
        BackupRepositoryVO backupRepository = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        ReflectionTestUtils.setField(backupRepository, "uuid", "uuid");

        Mockito.when(backupRepositoryDao.listByZoneAndProvider(1L, "nas")).thenReturn(Collections.singletonList(backupRepository));

        List<BackupOffering> result = nasBackupProvider.listBackupOfferings(1L);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("test-repo", result.get(0).getName());
        Assert.assertEquals("uuid", result.get(0).getUuid());
    }

    @Test
    public void testGetBackupStorageStats() {
        BackupRepositoryVO backupRepository1 = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1000L, null);
        backupRepository1.setUsedBytes(500L);

        BackupRepositoryVO backupRepository2 = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 2000L, null);
        backupRepository2.setUsedBytes(600L);

        Mockito.when(backupRepositoryDao.listByZoneAndProvider(1L, "nas"))
                .thenReturn(List.of(backupRepository1, backupRepository2));

        Pair<Long, Long> result = nasBackupProvider.getBackupStorageStats(1L);
        Assert.assertEquals(Long.valueOf(1100L), result.first());
        Assert.assertEquals(Long.valueOf(3000L), result.second());
    }

    @Test
    public void takeBackupSuccessfully() throws AgentUnavailableException, OperationTimedoutException {
        Long vmId = 1L;
        Long hostId = 2L;
        Long backupOfferingId = 3L;
        Long accountId = 4L;
        Long domainId = 5L;
        Long zoneId = 6L;
        Long backupId = 7L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getHostId()).thenReturn(hostId);
        Mockito.when(vm.getInstanceName()).thenReturn("test-vm");
        Mockito.when(vm.getBackupOfferingId()).thenReturn(backupOfferingId);
        Mockito.when(vm.getAccountId()).thenReturn(accountId);
        Mockito.when(vm.getDomainId()).thenReturn(domainId);
        Mockito.when(vm.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(vm.getState()).thenReturn(VMInstanceVO.State.Running);

        BackupRepository backupRepository = mock(BackupRepository.class);
        Mockito.when(backupRepository.getType()).thenReturn("nfs");
        Mockito.when(backupRepository.getAddress()).thenReturn("address");
        Mockito.when(backupRepository.getMountOptions()).thenReturn("sync");
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(backupOfferingId)).thenReturn(backupRepository);

        HostVO host = mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        VolumeVO volume1 = mock(VolumeVO.class);
        Mockito.when(volume1.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume1.getSize()).thenReturn(100L);
        VolumeVO volume2 = mock(VolumeVO.class);
        Mockito.when(volume2.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume2.getSize()).thenReturn(200L);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(List.of(volume1, volume2));

        BackupAnswer answer = mock(BackupAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.getSize()).thenReturn(100L);
        Mockito.when(agentManager.send(anyLong(), Mockito.any(TakeBackupCommand.class))).thenReturn(answer);

        Mockito.when(backupDao.persist(Mockito.any(BackupVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(backupDao.update(Mockito.anyLong(), Mockito.any(BackupVO.class))).thenReturn(true);

        Pair<Boolean, Backup> result = nasBackupProvider.takeBackup(vm, false);

        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        BackupVO backup = (BackupVO) result.second();
        Assert.assertEquals(Optional.ofNullable(100L), Optional.ofNullable(backup.getSize()));
        Assert.assertEquals(Backup.Status.BackedUp, backup.getStatus());
        Assert.assertEquals("FULL", backup.getType());
        Assert.assertEquals(Optional.of(300L), Optional.of(backup.getProtectedSize()));
        Assert.assertEquals(Optional.of(backupOfferingId), Optional.of(backup.getBackupOfferingId()));
        Assert.assertEquals(Optional.of(accountId), Optional.of(backup.getAccountId()));
        Assert.assertEquals(Optional.of(domainId), Optional.of(backup.getDomainId()));
        Assert.assertEquals(Optional.of(zoneId), Optional.of(backup.getZoneId()));

        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
        Mockito.verify(backupDao).update(Mockito.anyLong(), Mockito.any(BackupVO.class));
        Mockito.verify(agentManager).send(anyLong(), Mockito.any(TakeBackupCommand.class));
    }

    @Test
    public void testGetVMHypervisorHost() {
        Long hostId = 1L;
        Long vmId = 1L;
        Long zoneId = 1L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);

        HostVO host = mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        Host result = nasBackupProvider.getVMHypervisorHost(vm);

        Assert.assertNotNull(result);
        Assert.assertTrue(Objects.equals(hostId, result.getId()));
        Mockito.verify(hostDao).findById(hostId);
    }

    @Test
    public void testGetVMHypervisorHostWithHostDown() {
        Long hostId = 1L;
        Long clusterId = 2L;
        Long vmId = 1L;
        Long zoneId = 1L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);

        HostVO downHost = mock(HostVO.class);
        Mockito.when(downHost.getStatus()).thenReturn(Status.Down);
        Mockito.when(downHost.getClusterId()).thenReturn(clusterId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(downHost);

        HostVO upHostInCluster = mock(HostVO.class);
        Mockito.when(upHostInCluster.getId()).thenReturn(3L);
        Mockito.when(upHostInCluster.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findHypervisorHostInCluster(clusterId)).thenReturn(List.of(upHostInCluster));

        Host result = nasBackupProvider.getVMHypervisorHost(vm);

        Assert.assertNotNull(result);
        Assert.assertTrue(Objects.equals(Long.valueOf(3L), result.getId()));
        Mockito.verify(hostDao).findById(hostId);
        Mockito.verify(hostDao).findHypervisorHostInCluster(clusterId);
    }

    @Test
    public void testGetVMHypervisorHostWithUpHostViaRootVolumeCluster() {
        Long vmId = 1L;
        Long zoneId = 1L;
        Long clusterId = 2L;
        Long poolId = 3L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(null);
        Mockito.when(vm.getId()).thenReturn(vmId);

        VolumeVO rootVolume = mock(VolumeVO.class);
        Mockito.when(rootVolume.getPoolId()).thenReturn(poolId);
        Mockito.when(volumeDao.getInstanceRootVolume(vmId)).thenReturn(rootVolume);

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        Mockito.when(storagePool.getClusterId()).thenReturn(clusterId);
        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(storagePool);

        HostVO upHostInCluster = mock(HostVO.class);
        Mockito.when(upHostInCluster.getId()).thenReturn(4L);
        Mockito.when(upHostInCluster.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findHypervisorHostInCluster(clusterId)).thenReturn(List.of(upHostInCluster));

        Host result = nasBackupProvider.getVMHypervisorHost(vm);

        Assert.assertNotNull(result);
        Assert.assertTrue(Objects.equals(Long.valueOf(4L), result.getId()));
        Mockito.verify(volumeDao).getInstanceRootVolume(vmId);
        Mockito.verify(storagePoolDao).findById(poolId);
        Mockito.verify(hostDao).findHypervisorHostInCluster(clusterId);
    }

    @Test
    public void testGetVMHypervisorHostFallbackToZoneWideKVMHost() {
        Long hostId = 1L;
        Long clusterId = 2L;
        Long vmId = 1L;
        Long zoneId = 1L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        Mockito.when(vm.getDataCenterId()).thenReturn(zoneId);

        HostVO downHost = mock(HostVO.class);
        Mockito.when(downHost.getStatus()).thenReturn(Status.Down);
        Mockito.when(downHost.getClusterId()).thenReturn(clusterId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(downHost);

        Mockito.when(hostDao.findHypervisorHostInCluster(clusterId)).thenReturn(Collections.emptyList());

        HostVO fallbackHost = mock(HostVO.class);
        Mockito.when(fallbackHost.getId()).thenReturn(5L);
        Mockito.when(resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, zoneId))
                .thenReturn(fallbackHost);

        Host result = nasBackupProvider.getVMHypervisorHost(vm);

        Assert.assertNotNull(result);
        Assert.assertTrue(Objects.equals(Long.valueOf(5L), result.getId()));
        Mockito.verify(hostDao).findById(hostId);
        Mockito.verify(hostDao).findHypervisorHostInCluster(clusterId);
        Mockito.verify(resourceManager).findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, zoneId);
    }

    // -- nas.backup.incremental.enabled master switch ------------------------------------

    /**
     * When the operator sets nas.backup.incremental.enabled=false at the zone level, every
     * backup must be a fresh full anchor, regardless of VM state or nas.backup.full.every.
     * This is a single toggle the
     * operator can flip without having to count remaining backups in a chain.
     */
    @Test
    public void decideChainReturnsLegacyFullWhenIncrementalDisabled() {
        Long zoneId = 1L;
        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getDataCenterId()).thenReturn(zoneId);

        // Stub the master switch to false. ConfigKey.valueIn delegates to the framework's
        // ConfigDepot at runtime; for the unit test we override the in-memory value via the
        // ConfigKey's local override (set by ReflectionTestUtils on the spy provider).
        ReflectionTestUtils.setField(nasBackupProvider, "NASBackupIncrementalEnabled",
                new org.apache.cloudstack.framework.config.ConfigKey<>("Advanced", Boolean.class,
                        "nas.backup.incremental.enabled", "false",
                        "test override — disabled", true,
                        org.apache.cloudstack.framework.config.ConfigKey.Scope.Zone));

        NASBackupProvider.ChainDecision decision = nasBackupProvider.decideChain(vm);
        Assert.assertNotNull(decision);
        Assert.assertEquals(NASBackupChainKeys.TYPE_LEGACY_FULL, decision.mode);
        Assert.assertNull("legacy-full must not carry a bitmap", decision.bitmapNew);
        Assert.assertNull(decision.bitmapParent);
        Assert.assertNull("legacy-full must not start a chain", decision.chainId);
        Assert.assertEquals(0, decision.chainPosition);
    }

    // -- decideChain anchored on VM's active_checkpoint_id -------------------------------

    /**
     * No active_checkpoint_id on the VM (post-restore, first-ever backup, or detail purged) =>
     * decideChain must return a fresh full. Relying on the last backup taken as the parent
     * breaks after a restore, so the decision is anchored on the active checkpoint instead.
     */
    @Test
    public void decideChainReturnsFullWhenVmHasNoActiveCheckpoint() {
        Long zoneId = 1L;
        Long vmId = 42L;
        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(vm.getState()).thenReturn(VMInstanceVO.State.Running);

        // Master switch defaults to false (opt-in by zone) — explicitly enable it for this
        // test so we exercise the "no active_checkpoint_id" branch rather than short-circuit
        // at the master-switch gate.
        ReflectionTestUtils.setField(nasBackupProvider, "NASBackupIncrementalEnabled",
                new org.apache.cloudstack.framework.config.ConfigKey<>("Advanced", Boolean.class,
                        "nas.backup.incremental.enabled", "true",
                        "test override — enabled", true,
                        org.apache.cloudstack.framework.config.ConfigKey.Scope.Zone));

        Mockito.when(vmInstanceDetailsDao.findDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID)).thenReturn(null);

        NASBackupProvider.ChainDecision decision = nasBackupProvider.decideChain(vm);
        Assert.assertNotNull(decision);
        Assert.assertEquals(NASBackupChainKeys.TYPE_FULL, decision.mode);
        Assert.assertNull(decision.bitmapParent);
        Assert.assertEquals(0, decision.chainPosition);
    }

    // -- incremental storage-capability guard (Ceph-RBD / Linstor stay on legacy full) ----

    /**
     * Incremental checkpoints are only possible on file-based qcow2 storage. A VM whose every
     * volume sits on NFS / HOST-scope local / SharedMountPoint is checkpoint-capable.
     */
    @Test
    public void allVolumesOnCheckpointCapableStorageTrueForNfsHostAndSharedMount() {
        Long vmId = 55L;
        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);

        VolumeVO nfsVol = mock(VolumeVO.class);
        Mockito.when(nfsVol.getPoolId()).thenReturn(1L);
        VolumeVO hostVol = mock(VolumeVO.class);
        Mockito.when(hostVol.getPoolId()).thenReturn(2L);
        VolumeVO smpVol = mock(VolumeVO.class);
        Mockito.when(smpVol.getPoolId()).thenReturn(3L);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(List.of(nfsVol, hostVol, smpVol));

        StoragePoolVO nfs = mock(StoragePoolVO.class);
        Mockito.when(nfs.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        StoragePoolVO host = mock(StoragePoolVO.class);
        Mockito.when(host.getScope()).thenReturn(ScopeType.HOST);
        StoragePoolVO smp = mock(StoragePoolVO.class);
        Mockito.when(smp.getPoolType()).thenReturn(Storage.StoragePoolType.SharedMountPoint);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(nfs);
        Mockito.when(storagePoolDao.findById(2L)).thenReturn(host);
        Mockito.when(storagePoolDao.findById(3L)).thenReturn(smp);

        Assert.assertTrue(nasBackupProvider.allVolumesOnCheckpointCapableStorage(vm));
    }

    /**
     * A single volume on Ceph-RBD (or any pool that cannot carry a per-disk checkpoint) forces
     * the whole VM onto the legacy full-only path — avoids regressing RBD/Linstor storages.
     */
    @Test
    public void allVolumesOnCheckpointCapableStorageFalseWhenAnyVolumeOnRbd() {
        Long vmId = 56L;
        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);

        VolumeVO nfsVol = mock(VolumeVO.class);
        Mockito.when(nfsVol.getPoolId()).thenReturn(1L);
        VolumeVO rbdVol = mock(VolumeVO.class);
        Mockito.when(rbdVol.getPoolId()).thenReturn(9L);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(List.of(nfsVol, rbdVol));

        StoragePoolVO nfs = mock(StoragePoolVO.class);
        Mockito.when(nfs.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        StoragePoolVO rbd = mock(StoragePoolVO.class);
        Mockito.when(rbd.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(nfs);
        Mockito.when(storagePoolDao.findById(9L)).thenReturn(rbd);

        Assert.assertFalse(nasBackupProvider.allVolumesOnCheckpointCapableStorage(vm));
    }

    /** A volume whose storage pool can no longer be resolved is treated as incapable (safe). */
    @Test
    public void allVolumesOnCheckpointCapableStorageFalseWhenPoolUnresolvable() {
        Long vmId = 57L;
        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        VolumeVO vol = mock(VolumeVO.class);
        Mockito.when(vol.getPoolId()).thenReturn(1L);
        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(List.of(vol));
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(null);

        Assert.assertFalse(nasBackupProvider.allVolumesOnCheckpointCapableStorage(vm));
    }

    // -- restore clears active_checkpoint_id ---------------------------------------------

    /**
     * After a successful restoreVMFromBackup, decideChain on the next backup must produce
     * a full. We verify this end-to-end by checking that vmInstanceDetailsDao.removeDetail
     * is called with the active_checkpoint_id key.
     */
    @Test
    public void restoreClearsActiveCheckpointDetail() throws AgentUnavailableException, OperationTimedoutException {
        Long vmId = 7L;
        Long hostId = 8L;
        Long backupOfferingId = 9L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        Mockito.when(vm.getRemoved()).thenReturn(null);
        Mockito.when(vm.getName()).thenReturn("vm7");

        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupVO backup = new BackupVO();
        backup.setVmId(vmId);
        backup.setBackupOfferingId(backupOfferingId);
        backup.setExternalId("i-2-7-VM/2026.05.16.10.00.00");
        ReflectionTestUtils.setField(backup, "id", 100L);
        // backedUpVolumes defaults to null => BackupVO.getBackedUpVolumes returns emptyList().

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(backupOfferingId)).thenReturn(repo);

        Mockito.when(volumeDao.findByInstance(vmId)).thenReturn(Collections.emptyList());

        BackupAnswer answer = mock(BackupAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(RestoreBackupCommand.class))).thenReturn(answer);

        // Pre-existing checkpoint detail so removeDetail has something to "clear".
        VMInstanceDetailVO existing = mock(VMInstanceDetailVO.class);
        Mockito.when(existing.getValue()).thenReturn("backup-1715000000");
        Mockito.when(vmInstanceDetailsDao.findDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID)).thenReturn(existing);

        boolean ok = nasBackupProvider.restoreVMFromBackup(vm, backup);
        Assert.assertTrue(ok);
        Mockito.verify(vmInstanceDetailsDao).removeDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
    }

    /**
     * Single-volume restore (restoreBackedUpVolume) must also clear the target VM's
     * active_checkpoint_id, so the next backup of that VM is a fresh full — the restored
     * volume's image carries no QEMU bitmap.
     */
    @Test
    public void restoreBackedUpVolumeClearsTargetVmActiveCheckpoint()
            throws AgentUnavailableException, OperationTimedoutException {
        Long targetVmId = 42L;
        Long backupOfferingId = 9L;
        String targetVmName = "i-2-42-VM";
        String volUuid = "vol-uuid-1";
        String hostIp = "10.0.0.5";
        String dsUuid = "ds-uuid-1";

        VolumeVO srcVolume = mock(VolumeVO.class);
        Mockito.when(srcVolume.getUuid()).thenReturn(volUuid);
        Mockito.when(srcVolume.getName()).thenReturn("data1");
        Mockito.when(volumeDao.findByUuid(volUuid)).thenReturn(srcVolume);

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        Mockito.when(diskOffering.getId()).thenReturn(5L);
        Mockito.when(diskOffering.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(diskOfferingDao.findByUuid(Mockito.anyString())).thenReturn(diskOffering);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(11L);
        Mockito.when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(storagePoolDao.findByUuid(dsUuid)).thenReturn(pool);

        HostVO host = mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(8L);
        Mockito.when(hostDao.findByIp(hostIp)).thenReturn(host);

        Backup.VolumeInfo backedUp = new Backup.VolumeInfo(volUuid, "i-2-99-VM/2026/data1.qcow2",
                Volume.Type.DATADISK, 1024L, 1L, "disk-offering-uuid", null, null);

        BackupVO backup = new BackupVO();
        backup.setVmId(99L);
        backup.setBackupOfferingId(backupOfferingId);
        backup.setExternalId("i-2-99-VM/2026.06.22.10.00.00");
        backup.setSize(1024L);
        backup.setBackedUpVolumes(new Gson().toJson(Collections.singletonList(backedUp)));
        ReflectionTestUtils.setField(backup, "id", 200L);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(backupOfferingId)).thenReturn(repo);

        BackupAnswer answer = mock(BackupAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(RestoreBackupCommand.class))).thenReturn(answer);

        VMInstanceVO targetVm = mock(VMInstanceVO.class);
        Mockito.when(targetVm.getId()).thenReturn(targetVmId);
        Mockito.when(vmInstanceDao.findVMByInstanceName(targetVmName)).thenReturn(targetVm);

        VMInstanceDetailVO existing = mock(VMInstanceDetailVO.class);
        Mockito.when(existing.getValue()).thenReturn("backup-1718000000");
        Mockito.when(vmInstanceDetailsDao.findDetail(targetVmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID)).thenReturn(existing);

        Pair<Boolean, String> result = nasBackupProvider.restoreBackedUpVolume(
                backup, backedUp, hostIp, dsUuid, new Pair<>(targetVmName, VirtualMachine.State.Stopped));

        Assert.assertTrue(result.first());
        Mockito.verify(vmInstanceDetailsDao).removeDetail(targetVmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
    }

    // -- delete-pending cascade ----------------------------------------------------------

    /**
     * Deleting an incremental that has a live child must mark the incremental as
     * delete-pending in backup_details and NOT touch the on-NAS file or the backups row.
     * A parent with live children is soft-deleted (delete-pending) rather than removed.
     */
    @Test
    public void deleteWithLiveChildMarksDeletePendingAndPreservesFile()
            throws AgentUnavailableException, OperationTimedoutException {
        Long zoneId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;

        BackupVO parent = new BackupVO();
        parent.setVmId(vmId);
        parent.setBackupOfferingId(offeringId);
        parent.setExternalId("i-2-2-VM/2026.05.10.10.00.00");
        parent.setZoneId(zoneId);
        ReflectionTestUtils.setField(parent, "id", 50L);
        ReflectionTestUtils.setField(parent, "uuid", "parent-uuid");

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        // Note: host.getId() is intentionally not stubbed — the live-child path never
        // contacts the agent (verified below), so the stub would be unnecessary.
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(offeringId)).thenReturn(repo);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);

        // CHAIN_ID on the parent => not the no-chain fast path.
        BackupDetailVO chainIdDetail = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO chainPosDetail = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID)).thenReturn(chainIdDetail);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(chainPosDetail);

        // A live child sits deeper in the chain (higher CHAIN_POSITION).
        BackupVO child = new BackupVO();
        child.setVmId(vmId);
        child.setBackupOfferingId(offeringId);
        child.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        child.setZoneId(zoneId);
        child.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(child, "id", 51L);
        ReflectionTestUtils.setField(child, "uuid", "child-uuid");

        BackupDetailVO childChainId = new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO childChainPos = new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_POSITION, "1", true);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID)).thenReturn(childChainId);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(childChainPos);

        Mockito.when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(parent, child));
        // Re-read under the chain lock + markDeletePending both load the row by id.
        Mockito.when(backupDao.findById(50L)).thenReturn(parent);
        Mockito.doReturn(mock(GlobalLock.class)).when(nasBackupProvider).acquireChainDeleteLock(vmId);

        boolean result = nasBackupProvider.deleteBackup(parent, false);
        Assert.assertTrue(result);

        // No agent traffic — the on-NAS file must be preserved while children are alive.
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        // No DB row removal — the row is the tombstone marker.
        Mockito.verify(backupDao, Mockito.never()).remove(50L);
        // A tombstoned backup is NOT decremented — its space is still occupied until swept.
        Mockito.verify(resourceLimitMgr, Mockito.never()).decrementResourceCount(Mockito.anyLong(), Mockito.eq(Resource.ResourceType.backup));
        // The tombstoned backup is moved to Status.Hidden (replaces the old DELETE_PENDING detail).
        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        Mockito.verify(backupDao).update(Mockito.eq(50L), captor.capture());
        Assert.assertEquals(Backup.Status.Hidden, captor.getValue().getStatus());
        Mockito.verify(backupDetailsDao, Mockito.never()).persist(Mockito.any(BackupDetailVO.class));
    }

    /**
     * Deleting a leaf incremental whose parent is delete-pending must (a) delete the leaf and
     * then (b) sweep up the tombstoned parent. Mirrors DefaultSnapshotStrategy's
     * "delete leaf, then walk up while parent is destroying-and-childless".
     */
    @Test
    public void deletingLeafSweepsUpDeletePendingParent()
            throws AgentUnavailableException, OperationTimedoutException {
        Long zoneId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;

        BackupVO leaf = new BackupVO();
        leaf.setVmId(vmId);
        leaf.setBackupOfferingId(offeringId);
        leaf.setExternalId("i-2-2-VM/2026.05.10.11.00.00");
        leaf.setZoneId(zoneId);
        ReflectionTestUtils.setField(leaf, "id", 51L);
        ReflectionTestUtils.setField(leaf, "uuid", "leaf-uuid");

        BackupVO parent = new BackupVO();
        parent.setVmId(vmId);
        parent.setBackupOfferingId(offeringId);
        parent.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        parent.setZoneId(zoneId);
        ReflectionTestUtils.setField(parent, "id", 50L);
        ReflectionTestUtils.setField(parent, "uuid", "parent-uuid");

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(offeringId)).thenReturn(repo);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);

        // Leaf details. CHAIN_POSITION=1 puts the leaf after the full anchor in the
        // ordered chain — getChainOrderedLeafToRoot sorts by CHAIN_POSITION descending.
        BackupDetailVO leafChainId = new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO leafChainPos = new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_POSITION, "1", true);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID)).thenReturn(leafChainId);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(leafChainPos);

        // Parent is the tombstoned full anchor (CHAIN_POSITION=0).
        BackupDetailVO parentChainId = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO parentChainPos = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true);
        // The parent is the tombstone — now represented by Status.Hidden (was the DELETE_PENDING detail).
        parent.setStatus(Backup.Status.Hidden);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID)).thenReturn(parentChainId);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(parentChainPos);

        // We still use a mutable list + remove() answer so the DAO contract is realistic.
        java.util.List<Backup> liveBackups = new java.util.ArrayList<>(List.of(parent, leaf));
        Mockito.when(backupDao.listByVmId(null, vmId)).thenAnswer(inv -> new java.util.ArrayList<>(liveBackups));
        // The target row is re-read under the chain lock before any chain decision.
        Mockito.when(backupDao.findById(51L)).thenReturn(leaf);
        Mockito.doReturn(mock(GlobalLock.class)).when(nasBackupProvider).acquireChainDeleteLock(vmId);

        // Agent acknowledges every delete.
        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class)))
                .thenReturn(new BackupAnswer(new DeleteBackupCommand(null, null, null, null), true, "ok"));
        // backupDao.remove(id) drops the corresponding row from the live list so the next
        // listByVmId call reflects post-delete state — mirrors the real DAO contract.
        Mockito.when(backupDao.remove(Mockito.anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            liveBackups.removeIf(b -> b.getId() == id);
            return true;
        });

        boolean result = nasBackupProvider.deleteBackup(leaf, false);
        Assert.assertTrue(result);

        // Both backups must be physically deleted (leaf first, then tombstoned parent).
        Mockito.verify(agentManager, Mockito.times(2))
                .send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        Mockito.verify(backupDao).remove(51L);
        Mockito.verify(backupDao).remove(50L);
        // Exactly-once resource accounting: decremented for BOTH physically-removed backups
        // (leaf + swept ancestor), not just one.
        Mockito.verify(resourceLimitMgr, Mockito.times(2))
                .decrementResourceCount(Mockito.anyLong(), Mockito.eq(Resource.ResourceType.backup));
        Mockito.verify(resourceLimitMgr, Mockito.times(2))
                .decrementResourceCount(Mockito.anyLong(), Mockito.eq(Resource.ResourceType.backup_storage), Mockito.any());
    }

    @Test
    public void deletingLastLiveMemberCollectsDeeperOrphanTombstones()
            throws AgentUnavailableException, OperationTimedoutException {
        Long zoneId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;

        BackupVO full = new BackupVO();
        full.setVmId(vmId);
        full.setBackupOfferingId(offeringId);
        full.setExternalId("i-2-2-VM/2026.05.10.10.00.00");
        full.setZoneId(zoneId);
        full.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(full, "id", 50L);

        // The last live incremental — the one being deleted.
        BackupVO inc1 = new BackupVO();
        inc1.setVmId(vmId);
        inc1.setBackupOfferingId(offeringId);
        inc1.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        inc1.setZoneId(zoneId);
        inc1.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(inc1, "id", 51L);

        // A stranded tombstone deeper in the chain: its own descendants are long gone, so
        // only a sweep triggered by an ancestor's deletion can ever collect it.
        BackupVO orphan = new BackupVO();
        orphan.setVmId(vmId);
        orphan.setBackupOfferingId(offeringId);
        orphan.setExternalId("i-2-2-VM/2026.05.10.11.00.00");
        orphan.setZoneId(zoneId);
        orphan.setStatus(Backup.Status.Hidden);
        ReflectionTestUtils.setField(orphan, "id", 52L);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(offeringId)).thenReturn(repo);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);

        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true));
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_POSITION, "1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_POSITION, "2", true));

        Mockito.when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(full, inc1, orphan));
        Mockito.when(backupDao.findById(51L)).thenReturn(inc1);
        Mockito.doReturn(mock(GlobalLock.class)).when(nasBackupProvider).acquireChainDeleteLock(vmId);

        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class)))
                .thenReturn(new BackupAnswer(new DeleteBackupCommand(null, null, null, null), true, "ok"));

        Assert.assertTrue(nasBackupProvider.deleteBackup(inc1, false));

        // inc1 and the deeper orphan tombstone are physically removed; the live full survives.
        Mockito.verify(agentManager, Mockito.times(2))
                .send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        Mockito.verify(backupDao).remove(51L);
        Mockito.verify(backupDao).remove(52L);
        Mockito.verify(backupDao, Mockito.never()).remove(50L);
    }

    @Test
    public void deletingAncestorOfTombstoneWithLiveDescendantTombstonesIt()
            throws AgentUnavailableException, OperationTimedoutException {
        Long zoneId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;

        BackupVO full = new BackupVO();
        full.setVmId(vmId);
        full.setBackupOfferingId(offeringId);
        full.setExternalId("i-2-2-VM/2026.05.10.10.00.00");
        full.setZoneId(zoneId);
        full.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(full, "id", 50L);

        BackupVO inc1 = new BackupVO();
        inc1.setVmId(vmId);
        inc1.setBackupOfferingId(offeringId);
        inc1.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        inc1.setZoneId(zoneId);
        inc1.setStatus(Backup.Status.Hidden);
        ReflectionTestUtils.setField(inc1, "id", 51L);

        BackupVO inc2 = new BackupVO();
        inc2.setVmId(vmId);
        inc2.setBackupOfferingId(offeringId);
        inc2.setExternalId("i-2-2-VM/2026.05.10.11.00.00");
        inc2.setZoneId(zoneId);
        inc2.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(inc2, "id", 52L);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(offeringId)).thenReturn(repo);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);

        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true));
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_POSITION, "2", true));

        Mockito.when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(full, inc1, inc2));
        Mockito.when(backupDao.findById(50L)).thenReturn(full);
        Mockito.doReturn(mock(GlobalLock.class)).when(nasBackupProvider).acquireChainDeleteLock(vmId);

        Assert.assertTrue(nasBackupProvider.deleteBackup(full, false));

        // The full anchor must NOT be physically deleted — inc2 (live) still restores
        // through inc1's and full's files. It becomes a tombstone instead.
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        Mockito.verify(backupDao, Mockito.never()).remove(Mockito.anyLong());
        ArgumentCaptor<BackupVO> captor = ArgumentCaptor.forClass(BackupVO.class);
        Mockito.verify(backupDao).update(Mockito.eq(50L), captor.capture());
        Assert.assertEquals(Backup.Status.Hidden, captor.getValue().getStatus());
    }

    @Test
    public void sweepContinuesPastFailedTombstoneDelete()
            throws AgentUnavailableException, OperationTimedoutException {
        Long zoneId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;

        BackupVO full = new BackupVO();
        full.setVmId(vmId);
        full.setBackupOfferingId(offeringId);
        full.setExternalId("i-2-2-VM/2026.05.10.10.00.00");
        full.setZoneId(zoneId);
        full.setStatus(Backup.Status.Hidden);
        ReflectionTestUtils.setField(full, "id", 50L);

        BackupVO inc1 = new BackupVO();
        inc1.setVmId(vmId);
        inc1.setBackupOfferingId(offeringId);
        inc1.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        inc1.setZoneId(zoneId);
        inc1.setStatus(Backup.Status.Hidden);
        ReflectionTestUtils.setField(inc1, "id", 51L);

        // The last live member — deleting it triggers the sweep of both tombstones.
        BackupVO inc2 = new BackupVO();
        inc2.setVmId(vmId);
        inc2.setBackupOfferingId(offeringId);
        inc2.setExternalId("i-2-2-VM/2026.05.10.11.00.00");
        inc2.setZoneId(zoneId);
        inc2.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(inc2, "id", 52L);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        Mockito.when(vm.getLastHostId()).thenReturn(hostId);
        HostVO host = mock(HostVO.class);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);

        BackupRepositoryVO repo = new BackupRepositoryVO(1L, "nas", "test-repo",
                "nfs", "address", "sync", 1024L, null);
        Mockito.when(backupRepositoryDao.findByBackupOfferingId(offeringId)).thenReturn(repo);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);

        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true));
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_POSITION, "1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_ID))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_ID, "chain-1", true));
        Mockito.when(backupDetailsDao.findDetail(52L, NASBackupChainKeys.CHAIN_POSITION))
                .thenReturn(new BackupDetailVO(52L, NASBackupChainKeys.CHAIN_POSITION, "2", true));

        Mockito.when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(full, inc1, inc2));
        Mockito.when(backupDao.findById(52L)).thenReturn(inc2);
        Mockito.doReturn(mock(GlobalLock.class)).when(nasBackupProvider).acquireChainDeleteLock(vmId);

        // Deletes run leaf-to-root: inc2 succeeds, inc1's rm FAILS, full succeeds.
        DeleteBackupCommand dummy = new DeleteBackupCommand(null, null, null, null);
        Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class)))
                .thenReturn(new BackupAnswer(dummy, true, "ok"),
                        new BackupAnswer(dummy, false, "rm failed"),
                        new BackupAnswer(dummy, true, "ok"));

        Assert.assertTrue(nasBackupProvider.deleteBackup(inc2, false));

        // All three members were attempted; the failed inc1 keeps its row, full is collected.
        Mockito.verify(agentManager, Mockito.times(3))
                .send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        Mockito.verify(backupDao).remove(52L);
        Mockito.verify(backupDao, Mockito.never()).remove(51L);
        Mockito.verify(backupDao).remove(50L);
    }
}
