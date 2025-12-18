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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.backup.dao.BackupDao;
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
}
