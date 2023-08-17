//
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
//

package org.apache.cloudstack.storage.datastore.lifecycle;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.exception.StorageConflictException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.dao.StoragePoolHostDao;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by ajna123 on 9/22/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudStackPrimaryDataStoreLifeCycleImplTest extends TestCase {

    @InjectMocks
    PrimaryDataStoreLifeCycle _cloudStackPrimaryDataStoreLifeCycle = new CloudStackPrimaryDataStoreLifeCycleImpl();

    @InjectMocks
    StorageManager storageMgr = new StorageManagerImpl();

    @Mock
    ResourceManager _resourceMgr;

    @Mock
    AgentManager agentMgr;

    @Mock
    DataStoreManager _dataStoreMgr;

    @Mock
    DataStoreProviderManager _dataStoreProviderMgr;

    @Mock
    HypervisorHostListener hostListener;

    @Mock
    StoragePoolHostDao storagePoolHostDao;

    @Mock
    PrimaryDataStore store;

    @Mock
    DataStoreProvider dataStoreProvider;

    @Mock
    ModifyStoragePoolAnswer answer;

    @Mock
    StoragePoolInfo info;

    @Mock
    PrimaryDataStoreDao primaryStoreDao;

    @Mock
    StoragePoolVO storagePool;

    @Mock
    PrimaryDataStoreHelper primaryDataStoreHelper;

    AutoCloseable closeable;

    @Before
    public void initMocks() throws StorageConflictException {
        closeable = MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(storageMgr, "_storagePoolDao", primaryStoreDao);
        ReflectionTestUtils.setField(storageMgr, "_dataStoreProviderMgr", _dataStoreProviderMgr);
        ReflectionTestUtils.setField(storageMgr, "_dataStoreMgr", _dataStoreMgr);
        ReflectionTestUtils.setField(_cloudStackPrimaryDataStoreLifeCycle, "storageMgr", storageMgr);

        List<HostVO> hostList = new ArrayList<HostVO>();
        HostVO host1 = new HostVO(1L, "aa01", Host.Type.Routing, "192.168.1.1", "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null, null, 1L, null, 0, 0, "aa", 0, Storage.StoragePoolType.NetworkFilesystem);
        HostVO host2 = new HostVO(1L, "aa02", Host.Type.Routing, "192.168.1.1", "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null, null, 1L, null, 0, 0, "aa", 0, Storage.StoragePoolType.NetworkFilesystem);

        host1.setResourceState(ResourceState.Enabled);
        host2.setResourceState(ResourceState.Disabled);
        hostList.add(host1);
        hostList.add(host2);

        when(_dataStoreMgr.getDataStore(anyLong(), eq(DataStoreRole.Primary))).thenReturn(store);
        when(store.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(store.isShared()).thenReturn(true);
        when(store.getName()).thenReturn("newPool");
        when(store.getStorageProviderName()).thenReturn("default");


        when(_dataStoreProviderMgr.getDataStoreProvider(anyString())).thenReturn(dataStoreProvider);
        when(dataStoreProvider.getName()).thenReturn("default");

        when(hostListener.hostConnect(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        storageMgr.registerHostListener("default", hostListener);


        when(_resourceMgr.listAllUpHosts(eq(Host.Type.Routing), anyLong(), anyLong(), anyLong())).thenReturn(hostList);
        when(agentMgr.easySend(anyLong(), Mockito.any(ModifyStoragePoolCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);

        when(primaryStoreDao.findById(anyLong())).thenReturn(storagePool);
        when(primaryDataStoreHelper.attachCluster(Mockito.any(DataStore.class))).thenReturn(null);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testAttachCluster() throws Exception {
        Assert.assertTrue(_cloudStackPrimaryDataStoreLifeCycle.attachCluster(store, new ClusterScope(1L, 1L, 1L)));
    }
}
