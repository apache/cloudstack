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
package org.apache.cloudstack.storage.asup;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ManagementService;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Cluster;
import org.apache.cloudstack.storage.feign.model.EmsApplicationLog;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.utils.OntapConfigurationManager;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntapAsupManagerTest {

    // ── DAOs ──────────────────────────────────────────────────────────────────
    @Mock private PrimaryDataStoreDao storagePoolDao;
    @Mock private StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock private VolumeDao volumeDao;
    @Mock private SnapshotDao snapshotDao;
    @Mock private VMSnapshotDao vmSnapshotDao;
    @Mock private ManagementService managementService;
    @Mock private ManagementServerHostDao managementServerHostDao;

    @InjectMocks
    private OntapAsupManager asupManager;

    // ── Common fixtures ──────────────────────────────────────────────────────
    private StoragePoolVO pool;
    private Map<String, String> poolDetails;
    private StorageStrategy mockStrategy;
    private Cluster mockCluster;

    @BeforeEach
    void setUp() {
        pool = mock(StoragePoolVO.class);
        lenient().when(pool.getId()).thenReturn(1L);
        lenient().when(pool.getName()).thenReturn("ontap-pool-1");
        lenient().when(pool.getStatus()).thenReturn(StoragePoolStatus.Up);

        poolDetails = new HashMap<>();
        poolDetails.put(OntapStorageConstants.STORAGE_IP, "192.168.1.10");
        poolDetails.put(OntapStorageConstants.PROTOCOL, "NFS3");
        poolDetails.put(OntapStorageConstants.SVM_NAME, "svm1");
        poolDetails.put(OntapStorageConstants.VOLUME_UUID, "fv-uuid-1");
        poolDetails.put(OntapStorageConstants.VOLUME_NAME, "fv-name-1");

        mockStrategy = mock(StorageStrategy.class);

        mockCluster = mock(Cluster.class);
        lenient().when(mockCluster.getUuid()).thenReturn("cluster-uuid-1");
        lenient().when(mockCluster.getName()).thenReturn("ontap-cluster-1");
        lenient().when(mockCluster.getModel()).thenReturn("AFF-A400");
        lenient().when(mockCluster.getPlatformType())
                .thenReturn(OntapStorageConstants.ASUP_PLATFORM_TYPE_PERFORMANCE);
        lenient().when(managementService.getVersion()).thenReturn("4.23.0.0-SNAPSHOT");
        lenient().when(managementServerHostDao.listAll()).thenReturn(Collections.emptyList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // pushAsupTelemetry – no pools
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void pushAsupTelemetry_noOntapPools_sendsNoMessages() {
        when(storagePoolDao.findPoolsByProvider(OntapStorageConstants.ONTAP_PLUGIN_NAME))
                .thenReturn(Collections.emptyList());

        asupManager.pushAsupTelemetry();

        verify(mockStrategy, never()).sendAsupMessage(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Message count / event-id routing
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void pushAsupForStoragePool_newCluster_sendsHeartbeatThenPoolMessage() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        // heartbeat (event-id 0) + pool (event-id 1) = 2 messages
        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(2)).sendAsupMessage(cap.capture());

        List<EmsApplicationLog> msgs = cap.getAllValues();
        assertEquals(OntapStorageConstants.ASUP_EVENT_ID_HEARTBEAT,    msgs.get(0).getEventId());
        assertEquals(OntapStorageConstants.ASUP_EVENT_ID_STORAGE_POOL, msgs.get(1).getEventId());
        assertTrue(msgs.get(0).getEventDescription().contains("\"managementServerCount\":0"),
                msgs.get(0).getEventDescription());
        assertTrue(msgs.get(0).getEventDescription().contains("\"ontapClusterModel\":\"AFF-A400\""),
                msgs.get(0).getEventDescription());
        assertTrue(msgs.get(0).getEventDescription().contains("\"ontapPlatformType\":\"performance\""),
                msgs.get(0).getEventDescription());
    }

    @Test
    void pushAsupForStoragePool_cachedStorageIp_sendsOnlyPoolMessage() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        Map<String, OntapAsupManager.AsupClusterClient> clientsByStorageIp = new HashMap<>();
        clientsByStorageIp.put("192.168.1.10", new OntapAsupManager.AsupClusterClient(mockStrategy, mockCluster));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            asupManager.pushAsupForStoragePool(pool, clientsByStorageIp);
            u.verify(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any()), never());
        }

        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(1)).sendAsupMessage(cap.capture());
        assertEquals(OntapStorageConstants.ASUP_EVENT_ID_STORAGE_POOL, cap.getValue().getEventId());
        verify(mockStrategy, never()).getClusterInfo();
    }

    @Test
    void heartbeat_clusterModelUnknownWhenNodesGetReturnsNull() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(mockCluster.getModel()).thenReturn(null);
        when(mockCluster.getPlatformType()).thenReturn(null);
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(2)).sendAsupMessage(cap.capture());
        String heartbeat = cap.getAllValues().get(0).getEventDescription();
        assertTrue(heartbeat.contains("\"ontapClusterModel\":\"unknown\""), heartbeat);
        assertTrue(heartbeat.contains("\"ontapPlatformType\":\"unknown\""), heartbeat);
    }

    @Test
    void pushAsupForStoragePool_strategyThrows_doesNotPropagateException() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any()))
                    .thenThrow(new RuntimeException("connection refused"));
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        verify(mockStrategy, never()).sendAsupMessage(any());
    }

    @Test
    void pushAsupForStoragePool_poolDetailsEmpty_skipsPool() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(Collections.emptyMap());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any()))
                    .thenThrow(new RuntimeException("no details"));
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        verify(mockStrategy, never()).sendAsupMessage(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Pool message — content verification
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void poolMessage_containsPoolNameClusterUuidAndSnapshotKeys() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("ontap-pool-1"),          "should contain pool name");
        assertTrue(desc.contains("\"poolStatus\":\"Up\""), "should contain pool status");
        assertTrue(desc.contains("cluster-uuid-1"),        "should contain cluster UUID");
        assertTrue(desc.contains("volumeSnapshotCount"), "should contain volumeSnapshotCount");
        assertTrue(desc.contains("vmSnapshotCount"),       "should contain vmSnapshotCount");
        assertTrue(desc.contains("\"multiPrimaryStoragePoolVm\":false"), "desc=" + desc);
    }

    @Test
    void poolMessage_includesMaintenanceStatus() {
        when(pool.getStatus()).thenReturn(StoragePoolStatus.Maintenance);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"poolStatus\":\"Maintenance\""), "desc=" + desc);
    }

    @Test
    void poolMessage_volumeSnapshots_zeroWhenNoVolumes() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"volumeSnapshotCount\":0"), "desc=" + desc);
        assertTrue(desc.contains("\"vmSnapshotCount\":0"),       "desc=" + desc);
    }

    @Test
    void poolMessage_multiPrimaryStoragePoolVm_trueWhenDaoReportsSpan() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());
        when(volumeDao.hasMultiPrimaryStoragePoolVm(1L)).thenReturn(true);

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"multiPrimaryStoragePoolVm\":true"), "desc=" + desc);
    }

    @Test
    void poolMessage_volumeSnapshots_countExcludesDestroyed() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");

        // instanceId=null → no VM IDs → vmSnapshotDao never called
        VolumeVO vol = mockVolume(10L, null, 10_737_418_240L);
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.singletonList(vol));

        // 2 active + 1 Destroyed → only 2 counted
        SnapshotVO s1 = makeSnapshot(1L, 10L, Snapshot.State.BackedUp);
        SnapshotVO s2 = makeSnapshot(2L, 10L, Snapshot.State.Creating);
        SnapshotVO s3 = makeSnapshot(3L, 10L, Snapshot.State.Destroyed);
        when(snapshotDao.searchByVolumes(anyList())).thenReturn(Arrays.asList(s1, s2, s3));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"volumeSnapshotCount\":2"), "Destroyed must be excluded; desc=" + desc);
        verify(volumeDao, times(1)).findNonDestroyedVolumesByPoolId(eq(1L), isNull());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Pool message — VM-snapshot fields (vmSnapshotCount)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void poolMessage_vmSnapshots_countsActiveSnapshotsAcrossDistinctVMs() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");

        VolumeVO vol1 = mockVolume(10L, 100L, 10_737_418_240L); // vm 100
        VolumeVO vol2 = mockVolume(20L, 200L, 10_737_418_240L); // vm 200
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull()))
                .thenReturn(Arrays.asList(vol1, vol2));
        when(snapshotDao.searchByVolumes(anyList())).thenReturn(Collections.emptyList());

        // 2 active VM snapshots; 1 is Expunging (should be excluded)
        VMSnapshotVO vmSnap1   = makeVmSnapshot(VMSnapshot.State.Ready,     null);
        VMSnapshotVO vmSnap2   = makeVmSnapshot(VMSnapshot.State.Ready,     null);
        VMSnapshotVO vmSnapExp = makeVmSnapshot(VMSnapshot.State.Expunging, null);
        when(vmSnapshotDao.searchByVms(anyList())).thenReturn(Arrays.asList(vmSnap1, vmSnap2, vmSnapExp));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"vmSnapshotCount\":2"), "Expunging must be excluded; desc=" + desc);
    }

    @Test
    void poolMessage_vmSnapshots_removedSnapshotsExcluded() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");

        VolumeVO vol = mockVolume(10L, 100L, 1_073_741_824L);
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.singletonList(vol));
        when(snapshotDao.searchByVolumes(anyList())).thenReturn(Collections.emptyList());

        VMSnapshotVO active  = makeVmSnapshot(VMSnapshot.State.Ready, null);
        VMSnapshotVO deleted = makeVmSnapshot(VMSnapshot.State.Ready, new java.util.Date()); // removed
        when(vmSnapshotDao.searchByVms(anyList())).thenReturn(Arrays.asList(active, deleted));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"vmSnapshotCount\":1"), "removed snapshots must be excluded; desc=" + desc);
    }

    @Test
    void poolMessage_vmSnapshots_zeroWhenNoVmIdsOnPool() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");

        // instanceId=null → detached data disk → vmSnapshotDao must NOT be called
        VolumeVO vol = mockVolume(10L, null, 1_073_741_824L);
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.singletonList(vol));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"vmSnapshotCount\":0"), "desc=" + desc);
        verify(vmSnapshotDao, never()).searchByVms(anyList());
    }

    @Test
    void poolMessage_vmSnapshots_countedOnlyOnRootDiskPool() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");

        // Data disk of a VM whose ROOT lives on another pool — must not count that VM snapshot.
        VolumeVO dataDisk = mockVolume(20L, 100L, 1_073_741_824L, Volume.Type.DATADISK);
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull()))
                .thenReturn(Collections.singletonList(dataDisk));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        String desc = capturePoolMessage();
        assertTrue(desc.contains("\"vmSnapshotCount\":0"), "desc=" + desc);
        verify(vmSnapshotDao, never()).searchByVms(anyList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Best-effort: DAO failures must never suppress the pool message
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void poolMessage_snapshotDaoThrows_poolMessageStillSent() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull()))
                .thenThrow(new RuntimeException("DB error"));

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        // heartbeat + pool both sent even when DAO fails
        verify(mockStrategy, times(2)).sendAsupMessage(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Multi-pool: same cluster → single heartbeat
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void twoPoolsSameCluster_singleHeartbeat() {
        StoragePoolVO pool2 = mock(StoragePoolVO.class);
        when(pool2.getId()).thenReturn(2L);
        when(pool2.getName()).thenReturn("ontap-pool-2");

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(storagePoolDetailsDao.listDetailsKeyPairs(2L)).thenReturn(new HashMap<>(poolDetails));
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(anyLong(), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);

            Map<String, OntapAsupManager.AsupClusterClient> clientsByStorageIp = new HashMap<>();
            asupManager.pushAsupForStoragePool(pool, clientsByStorageIp);
            asupManager.pushAsupForStoragePool(pool2, clientsByStorageIp);

            u.verify(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any()), times(1));
        }

        verify(mockStrategy, times(1)).getClusterInfo();
        // 1 heartbeat + 2 pool messages = 3 total
        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(3)).sendAsupMessage(cap.capture());

        long heartbeats = cap.getAllValues().stream()
                .filter(m -> OntapStorageConstants.ASUP_EVENT_ID_HEARTBEAT.equals(m.getEventId()))
                .count();
        assertEquals(1, heartbeats, "exactly 1 heartbeat for two pools sharing a cluster");
    }

    @Test
    void twoPoolsDifferentStorageIp_resolvesStrategyPerCluster() {
        StoragePoolVO pool2 = mock(StoragePoolVO.class);
        when(pool2.getId()).thenReturn(2L);
        when(pool2.getName()).thenReturn("ontap-pool-2");

        Map<String, String> otherCluster = new HashMap<>(poolDetails);
        otherCluster.put(OntapStorageConstants.STORAGE_IP, "192.168.1.20");

        Cluster cluster2 = mock(Cluster.class);
        when(cluster2.getUuid()).thenReturn("cluster-uuid-2");

        StorageStrategy strategy2 = mock(StorageStrategy.class);
        when(strategy2.getClusterInfo()).thenReturn(cluster2);
        when(strategy2.getClusterVersion(cluster2)).thenReturn("9.16.1");

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(storagePoolDetailsDao.listDetailsKeyPairs(2L)).thenReturn(otherCluster);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(anyLong(), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(poolDetails)).thenReturn(mockStrategy);
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(otherCluster)).thenReturn(strategy2);

            Map<String, OntapAsupManager.AsupClusterClient> clientsByStorageIp = new HashMap<>();
            asupManager.pushAsupForStoragePool(pool, clientsByStorageIp);
            asupManager.pushAsupForStoragePool(pool2, clientsByStorageIp);

            u.verify(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any()), times(2));
        }

        verify(mockStrategy, times(1)).getClusterInfo();
        verify(strategy2, times(1)).getClusterInfo();
        verify(mockStrategy, times(2)).sendAsupMessage(any());
        verify(strategy2, times(2)).sendAsupMessage(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Common EMS envelope fields
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void allMessages_haveCorrectEnvelopeFields() {
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(poolDetails);
        when(mockStrategy.getClusterInfo()).thenReturn(mockCluster);
        when(mockStrategy.getClusterVersion(mockCluster)).thenReturn("9.17.1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(1L), isNull())).thenReturn(Collections.emptyList());

        try (MockedStatic<OntapStorageUtils> u = mockStatic(OntapStorageUtils.class)) {
            u.when(() -> OntapStorageUtils.resolveStrategyFromPoolDetails(any())).thenReturn(mockStrategy);
            asupManager.pushAsupForStoragePool(pool, new HashMap<>());
        }

        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(2)).sendAsupMessage(cap.capture());

        for (EmsApplicationLog msg : cap.getAllValues()) {
            assertEquals(OntapStorageConstants.ASUP_EVENT_SOURCE, msg.getEventSource());
            assertEquals(OntapStorageConstants.ASUP_CATEGORY,     msg.getCategory());
            assertEquals(OntapStorageConstants.ASUP_SEVERITY,     msg.getSeverity());
            assertFalse(msg.getAutosupportRequired(), "autosupport_required should be false");
            assertEquals(asupManager.getComputerName(), msg.getComputerName());
            assertEquals(asupManager.getCloudStackVersion(), msg.getAppVersion());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Config defaults
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void asupIntervalHours_defaultIsProductionValue() {
        assertEquals(4, OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS);
        assertEquals(24, OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS);
        assertEquals(String.valueOf(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS),
                OntapConfigurationManager.AsupIntervalHours.defaultValue());
    }

    @Test
    void asupIntervalHours_keyIsAutosupportInterval() {
        assertEquals(OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY,
                OntapConfigurationManager.AsupIntervalHours.key());
        assertEquals("ontap.autosupport.interval", OntapConfigurationManager.AsupIntervalHours.key());
    }

    @Test
    void asupIntervalHours_descriptionIncludesAllowedRange() {
        String description = OntapConfigurationManager.AsupIntervalHours.description();
        assertTrue(description.contains(String.valueOf(OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS)));
        assertTrue(description.contains(String.valueOf(OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS)));
        assertTrue(description.contains(String.valueOf(OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS)));
        assertTrue(description.contains("1 week"), description);
    }

    @Test
    void validateAsupInterval_acceptsDisabledMinMaxAndDefault() {
        OntapConfigurationManager.AsupIntervalHours.validateValue(String.valueOf(OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS));
        OntapConfigurationManager.AsupIntervalHours.validateValue(String.valueOf(OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS));
        OntapConfigurationManager.AsupIntervalHours.validateValue(String.valueOf(OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS));
        OntapConfigurationManager.AsupIntervalHours.validateValue(String.valueOf(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS));
    }

    @Test
    void validateAsupInterval_rejectsOutOfRangeAndNonInteger() {
        assertThrows(InvalidParameterValueException.class, () -> OntapConfigurationManager.AsupIntervalHours.validateValue("-1"));
        assertThrows(InvalidParameterValueException.class, () -> OntapConfigurationManager.AsupIntervalHours.validateValue("1"));
        assertThrows(InvalidParameterValueException.class, () -> OntapConfigurationManager.AsupIntervalHours.validateValue("169"));
        assertThrows(InvalidParameterValueException.class, () -> OntapConfigurationManager.AsupIntervalHours.validateValue("abc"));
        assertThrows(InvalidParameterValueException.class, () -> OntapConfigurationManager.AsupIntervalHours.validateValue(""));
    }

    @Test
    void getAsupIntervalHours_fallsBackOutsideRange() {
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(null));
        assertEquals(OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS));
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(1));
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(2));
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(3));
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(-1));
        assertEquals(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(169));
        assertEquals(OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS));
        assertEquals(OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS,
                asupManager.getAsupIntervalHours(OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS));
        assertEquals(25, asupManager.getAsupIntervalHours(25));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OntapAsupTask / schedule
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void millisUntilNextPush_zeroWhenOverdue() {
        asupManager.lastPushTime = Instant.EPOCH; // never pushed
        assertEquals(0L, asupManager.millisUntilNextPush());
    }

    @Test
    void millisUntilNextPush_remainderOfConfiguredInterval() {
        asupManager.lastPushTime = Instant.now();
        long remaining = asupManager.millisUntilNextPush();
        long configured = Duration.ofHours(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS).toMillis();
        assertTrue(remaining > configured - 5000L && remaining <= configured,
                "remaining=" + remaining);
    }

    @Test
    void asupTask_stampsLastPushTimeAndPushes() {
        asupManager.lastPushTime = Instant.EPOCH; // never pushed
        when(storagePoolDao.findPoolsByProvider(OntapStorageConstants.ONTAP_PLUGIN_NAME))
                .thenReturn(Collections.emptyList());
        Instant before = Instant.now();
        OntapAsupManager.OntapAsupTask task = asupManager.new OntapAsupTask();
        task.run();
        verify(storagePoolDao).findPoolsByProvider(OntapStorageConstants.ONTAP_PLUGIN_NAME);
        assertFalse(asupManager.lastPushTime.equals(Instant.EPOCH));
        assertFalse(asupManager.lastPushTime.isBefore(before));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getCloudStackVersion_returnsManagementServiceVersion() {
        assertEquals("4.23.0.0-SNAPSHOT", asupManager.getCloudStackVersion());
    }

    @Test
    void getCloudStackVersion_blank_returnsUnknown() {
        when(managementService.getVersion()).thenReturn("  ");
        assertEquals(OntapStorageConstants.ASUP_UNKNOWN, asupManager.getCloudStackVersion());
    }

    @Test
    void getManagementServerCount_returnsRegisteredHostCount() {
        when(managementServerHostDao.listAll()).thenReturn(Arrays.asList(
                mock(ManagementServerHostVO.class), mock(ManagementServerHostVO.class)));
        assertEquals(2, asupManager.getManagementServerCount());
    }

    @Test
    void getComputerName_returnsNonEmpty() {
        String host = asupManager.getComputerName();
        assertNotNull(host);
        assertFalse(host.isEmpty());
    }

    @Test
    void getOperatingSystem_returnsNonEmpty() {
        String os = asupManager.getOperatingSystem();
        assertNotNull(os);
        assertFalse(os.isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Captures and returns the event-id 1 (pool) message description.
     * Expects exactly 2 messages to have been sent (heartbeat + pool).
     */
    private String capturePoolMessage() {
        ArgumentCaptor<EmsApplicationLog> cap = ArgumentCaptor.forClass(EmsApplicationLog.class);
        verify(mockStrategy, times(2)).sendAsupMessage(cap.capture());
        EmsApplicationLog poolMsg = cap.getAllValues().get(1);
        assertEquals(OntapStorageConstants.ASUP_EVENT_ID_STORAGE_POOL, poolMsg.getEventId());
        String desc = poolMsg.getEventDescription();
        assertNotNull(desc);
        return desc;
    }

    /**
     * Creates a mock VolumeVO with state=Ready so that CS_VOLUME_STATES filter includes it
     * and getSize() is exercised (avoiding UnnecessaryStubbingException in strict mode).
     */
    private VolumeVO mockVolume(long id, Long instanceId, long size) {
        return mockVolume(id, instanceId, size, Volume.Type.ROOT);
    }

    private VolumeVO mockVolume(long id, Long instanceId, long size, Volume.Type type) {
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getId()).thenReturn(id);
        lenient().when(vol.getInstanceId()).thenReturn(instanceId);
        when(vol.getSize()).thenReturn(size);
        when(vol.getState()).thenReturn(Volume.State.Ready);
        when(vol.getVolumeType()).thenReturn(type);
        return vol;
    }

    /** Creates a mock SnapshotVO with the given id, volumeId and state. */
    private SnapshotVO makeSnapshot(long id, long volumeId, Snapshot.State state) {
        SnapshotVO snap = mock(SnapshotVO.class);
        when(snap.getState()).thenReturn(state);
        return snap;
    }

    /** Creates a mock VMSnapshotVO with the given state and removed timestamp. */
    private VMSnapshotVO makeVmSnapshot(VMSnapshot.State state, java.util.Date removed) {
        VMSnapshotVO vmSnap = mock(VMSnapshotVO.class);
        when(vmSnap.getState()).thenReturn(state);
        lenient().when(vmSnap.getRemoved()).thenReturn(removed);
        return vmSnap;
    }
}
