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

import static org.mockito.ArgumentMatchers.nullable;

import java.util.Collections;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolStatus;

public class StoragePoolMonitorTest {

    private StorageManagerImpl storageManager;
    private PrimaryDataStoreDao poolDao;
    private StoragePoolMonitor storagePoolMonitor;
    private HostVO host;
    private StoragePoolVO pool;
    private StartupRoutingCommand cmd;

    @Before
    public void setUp() throws Exception {
        storageManager = Mockito.mock(StorageManagerImpl.class);
        poolDao = Mockito.mock(PrimaryDataStoreDao.class);

        storagePoolMonitor = new StoragePoolMonitor(storageManager, poolDao, null);
        host = new HostVO("some-uuid");
        pool = new StoragePoolVO();
        pool.setScope(ScopeType.CLUSTER);
        pool.setStatus(StoragePoolStatus.Up);
        pool.setId(123L);
        cmd = new StartupRoutingCommand();
        cmd.setHypervisorType(Hypervisor.HypervisorType.KVM);
    }

    @Test
    public void testProcessConnectStoragePoolNormal() throws Exception {
        Mockito.when(poolDao.listBy(nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.any(ScopeType.class))).thenReturn(Collections.singletonList(pool));
        Mockito.when(poolDao.findZoneWideStoragePoolsByTags(Mockito.anyLong(), Mockito.any(String[].class))).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.when(poolDao.findZoneWideStoragePoolsByHypervisor(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class))).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.doReturn(true).when(storageManager).connectHostToSharedPool(host.getId(), pool.getId());

        storagePoolMonitor.processConnect(host, cmd, false);

        Mockito.verify(storageManager, Mockito.times(1)).connectHostToSharedPool(Mockito.eq(host.getId()), Mockito.eq(pool.getId()));
        Mockito.verify(storageManager, Mockito.times(1)).createCapacityEntry(Mockito.eq(pool.getId()));
    }

    @Test
    public void testProcessConnectStoragePoolFailureOnHost() throws Exception {
        Mockito.when(poolDao.listBy(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(ScopeType.class))).thenReturn(Collections.singletonList(pool));
        Mockito.when(poolDao.findZoneWideStoragePoolsByTags(Mockito.anyLong(), Mockito.any(String[].class))).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.when(poolDao.findZoneWideStoragePoolsByHypervisor(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class))).thenReturn(Collections.<StoragePoolVO>emptyList());
        Mockito.doThrow(new StorageUnavailableException("unable to mount storage", 123L)).when(storageManager).connectHostToSharedPool(Mockito.anyLong(), Mockito.anyLong());

        storagePoolMonitor.processConnect(host, cmd, false);
    }
}
