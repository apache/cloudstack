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
package com.cloud.storage.listener;

import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.dao.StoragePoolHostDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class StoragePoolMonitorTest {

    private StorageManagerImpl storageManager;
    private PrimaryDataStoreDao poolDao;
    private StoragePoolHostDao storagePoolHostDao;
    private StoragePoolMonitor storagePoolMonitor;
    private HostVO host;
    private StoragePoolVO pool;
    private StartupRoutingCommand cmd;

    @Before
    public void setUp() throws Exception {
        storageManager = mock(StorageManagerImpl.class);
        poolDao = mock(PrimaryDataStoreDao.class);
        storagePoolHostDao = mock(StoragePoolHostDao.class);

        storagePoolMonitor = new StoragePoolMonitor(storageManager, poolDao, storagePoolHostDao, null);
        host = new HostVO("some-uuid");
        pool = new StoragePoolVO();
        pool.setScope(ScopeType.CLUSTER);
        pool.setStatus(StoragePoolStatus.Up);
        pool.setId(123L);
        pool.setPoolType(Storage.StoragePoolType.Filesystem);
        cmd = new StartupRoutingCommand();
        cmd.setHypervisorType(Hypervisor.HypervisorType.KVM);
    }

    @Test
    public void testProcessConnectStoragePoolNormal() throws Exception {
        HostVO hostMock = mock(HostVO.class);
        StartupRoutingCommand startupRoutingCommand = mock(StartupRoutingCommand.class);
        StoragePoolVO poolMock = mock(StoragePoolVO.class);
        Mockito.lenient().when(poolMock.getScope()).thenReturn(ScopeType.CLUSTER);
        Mockito.lenient().when(poolMock.getStatus()).thenReturn(StoragePoolStatus.Up);
        Mockito.lenient().when(poolMock.getId()).thenReturn(123L);
        Mockito.lenient().when(poolMock.getPoolType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(hostMock.getDataCenterId()).thenReturn(1L);
        Mockito.when(hostMock.getPodId()).thenReturn(1L);
        Mockito.when(hostMock.getClusterId()).thenReturn(1L);
        Mockito.when(startupRoutingCommand.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, null, StoragePoolStatus.Up)).thenReturn(Collections.singletonList(pool));
        Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, null, StoragePoolStatus.Up)).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, Hypervisor.HypervisorType.KVM, StoragePoolStatus.Up)).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, Hypervisor.HypervisorType.Any, StoragePoolStatus.Up)).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.doReturn(true).when(storageManager).connectHostToSharedPool(hostMock, 123L);

        storagePoolMonitor.processConnect(hostMock, startupRoutingCommand, false);

        Mockito.verify(storageManager, Mockito.times(1)).connectHostToSharedPool(Mockito.eq(hostMock), Mockito.eq(pool.getId()));
        Mockito.verify(storageManager, Mockito.times(1)).createCapacityEntry(Mockito.eq(pool.getId()));
    }

    @Test
    public void testProcessConnectStoragePoolFailureOnHost() throws Exception {
        Mockito.lenient().when(poolDao.listBy(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(ScopeType.class))).thenReturn(Collections.singletonList(pool));
        Mockito.lenient().when(poolDao.findZoneWideStoragePoolsByTags(Mockito.anyLong(), Mockito.any(String[].class), Mockito.anyBoolean())).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.lenient().when(poolDao.findZoneWideStoragePoolsByHypervisor(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class))).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.lenient().doThrow(new StorageUnavailableException("unable to mount storage", 123L)).when(storageManager).connectHostToSharedPool(Mockito.any(), Mockito.anyLong());

        storagePoolMonitor.processConnect(host, cmd, false);
    }

    @Test
    public void testProcessConnectWithMountDisabledStoragePoolEnabled() throws Exception {
        StoragePoolVO disabledZonePool = new StoragePoolVO();
        disabledZonePool.setId(200L);
        disabledZonePool.setScope(ScopeType.ZONE);
        disabledZonePool.setStatus(StoragePoolStatus.Disabled);
        disabledZonePool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);

        StoragePoolVO disabledClusterPool = new StoragePoolVO();
        disabledClusterPool.setId(201L);
        disabledClusterPool.setScope(ScopeType.CLUSTER);
        disabledClusterPool.setStatus(StoragePoolStatus.Disabled);
        disabledClusterPool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        HostVO hostMock = mock(HostVO.class);
        StartupRoutingCommand startupRoutingCommand = mock(StartupRoutingCommand.class);
        Mockito.when(hostMock.getDataCenterId()).thenReturn(1L);
        Mockito.when(hostMock.getPodId()).thenReturn(1L);
        Mockito.when(hostMock.getClusterId()).thenReturn(1L);
        Mockito.when(hostMock.getId()).thenReturn(1L);
        Mockito.when(startupRoutingCommand.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);

        try (MockedStatic<StorageManager> storageManagerMockedStatic = Mockito.mockStatic(StorageManager.class)) {
            ConfigKey<Boolean> mockConfigKey = mock(ConfigKey.class);
            Mockito.when(mockConfigKey.value()).thenReturn(true);
            Mockito.when(mockConfigKey.valueIn(1L)).thenReturn(true);

            storageManagerMockedStatic.when(StorageManager::getMountDisabledStoragePool).thenReturn(mockConfigKey);

            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, null, StoragePoolStatus.Up))
                    .thenReturn(new ArrayList<>());
            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, null, StoragePoolStatus.Up))
                    .thenReturn(new ArrayList<>());
            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, Hypervisor.HypervisorType.KVM, StoragePoolStatus.Up))
                    .thenReturn(new ArrayList<>());
            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, Hypervisor.HypervisorType.Any, StoragePoolStatus.Up))
                    .thenReturn(new ArrayList<>());

            List<StoragePoolVO> zoneDisabledPoolsBySAG = new ArrayList<>();
            zoneDisabledPoolsBySAG.add(disabledZonePool);
            Mockito.when(poolDao.findDisabledPoolsByScopeAndAccessGroups(1L, null, null, ScopeType.ZONE, new String[0]))
                    .thenReturn(zoneDisabledPoolsBySAG);
            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, null, StoragePoolStatus.Disabled))
                    .thenReturn(new ArrayList<>());

            List<StoragePoolVO> clusterDisabledPoolsBySAG = new ArrayList<>();
            clusterDisabledPoolsBySAG.add(disabledClusterPool);
            Mockito.when(poolDao.findDisabledPoolsByScopeAndAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, new String[0]))
                    .thenReturn(clusterDisabledPoolsBySAG);
            Mockito.when(poolDao.findStoragePoolsByEmptyStorageAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, null, StoragePoolStatus.Disabled))
                    .thenReturn(new ArrayList<>());

            Mockito.when(storageManager.getStorageAccessGroups(null, null, null, 1L)).thenReturn(new String[0]);
            Mockito.when(storageManager.findStoragePoolsConnectedToHost(1L)).thenReturn(Collections.emptyList());
            Mockito.doReturn(true).when(storageManager).connectHostToSharedPool(hostMock, 200L);
            Mockito.doReturn(true).when(storageManager).connectHostToSharedPool(hostMock, 201L);

            storagePoolMonitor.processConnect(hostMock, startupRoutingCommand, false);

            Mockito.verify(poolDao, Mockito.times(1)).findDisabledPoolsByScopeAndAccessGroups(1L, null, null, ScopeType.ZONE, new String[0]);
            Mockito.verify(poolDao, Mockito.times(1)).findStoragePoolsByEmptyStorageAccessGroups(1L, null, null, ScopeType.ZONE, null, StoragePoolStatus.Disabled);
            Mockito.verify(poolDao, Mockito.times(1)).findDisabledPoolsByScopeAndAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, new String[0]);
            Mockito.verify(poolDao, Mockito.times(1)).findStoragePoolsByEmptyStorageAccessGroups(1L, 1L, 1L, ScopeType.CLUSTER, null, StoragePoolStatus.Disabled);

            Mockito.verify(storageManager, Mockito.times(1)).connectHostToSharedPool(Mockito.eq(hostMock), Mockito.eq(200L));
            Mockito.verify(storageManager, Mockito.times(1)).connectHostToSharedPool(Mockito.eq(hostMock), Mockito.eq(201L));
            Mockito.verify(storageManager, Mockito.times(1)).createCapacityEntry(Mockito.eq(200L));
            Mockito.verify(storageManager, Mockito.times(1)).createCapacityEntry(Mockito.eq(201L));
        }
    }
}
