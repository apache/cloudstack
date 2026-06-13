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
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
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
    public void decideChainReturnsFullWhenIncrementalDisabled() {
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
        Assert.assertEquals(NASBackupChainKeys.TYPE_FULL, decision.mode);
        Assert.assertNull(decision.bitmapParent);
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
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID)).thenReturn(chainIdDetail);

        // A live child references parent-uuid via PARENT_BACKUP_ID.
        BackupVO child = new BackupVO();
        child.setVmId(vmId);
        child.setBackupOfferingId(offeringId);
        child.setExternalId("i-2-2-VM/2026.05.10.10.30.00");
        child.setZoneId(zoneId);
        child.setStatus(Backup.Status.BackedUp);
        ReflectionTestUtils.setField(child, "id", 51L);
        ReflectionTestUtils.setField(child, "uuid", "child-uuid");

        BackupDetailVO childChainId = new BackupDetailVO(51L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO childParent = new BackupDetailVO(51L, NASBackupChainKeys.PARENT_BACKUP_ID, "parent-uuid", true);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID)).thenReturn(childChainId);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.PARENT_BACKUP_ID)).thenReturn(childParent);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.DELETE_PENDING)).thenReturn(null);

        Mockito.when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(parent, child));

        boolean result = nasBackupProvider.deleteBackup(parent, false);
        Assert.assertTrue(result);

        // No agent traffic — the on-NAS file must be preserved while children are alive.
        Mockito.verify(agentManager, Mockito.never()).send(Mockito.anyLong(), Mockito.any(DeleteBackupCommand.class));
        // No DB row removal — the row is the tombstone marker.
        Mockito.verify(backupDao, Mockito.never()).remove(50L);
        // A DELETE_PENDING detail was persisted.
        ArgumentCaptor<BackupDetailVO> captor = ArgumentCaptor.forClass(BackupDetailVO.class);
        Mockito.verify(backupDetailsDao).persist(captor.capture());
        Assert.assertEquals(NASBackupChainKeys.DELETE_PENDING, captor.getValue().getName());
        Assert.assertEquals("true", captor.getValue().getValue());
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
        BackupDetailVO leafParent = new BackupDetailVO(51L, NASBackupChainKeys.PARENT_BACKUP_ID, "parent-uuid", true);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_ID)).thenReturn(leafChainId);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(leafChainPos);
        Mockito.when(backupDetailsDao.findDetail(51L, NASBackupChainKeys.PARENT_BACKUP_ID)).thenReturn(leafParent);

        // Parent is the tombstoned full anchor (CHAIN_POSITION=0).
        BackupDetailVO parentChainId = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_ID, "chain-1", true);
        BackupDetailVO parentChainPos = new BackupDetailVO(50L, NASBackupChainKeys.CHAIN_POSITION, "0", true);
        BackupDetailVO parentPending = new BackupDetailVO(50L, NASBackupChainKeys.DELETE_PENDING, "true", true);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_ID)).thenReturn(parentChainId);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.CHAIN_POSITION)).thenReturn(parentChainPos);
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.DELETE_PENDING)).thenReturn(parentPending);
        // Parent has no parent of its own (it's the full anchor).
        Mockito.when(backupDetailsDao.findDetail(50L, NASBackupChainKeys.PARENT_BACKUP_ID)).thenReturn(null);

        // listByVmId is called once now (chain snapshot taken before the leaf delete).
        // We still use a mutable list + remove() answer so the DAO contract is realistic.
        java.util.List<Backup> liveBackups = new java.util.ArrayList<>(List.of(parent, leaf));
        Mockito.when(backupDao.listByVmId(null, vmId)).thenAnswer(inv -> new java.util.ArrayList<>(liveBackups));

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
    }
}
