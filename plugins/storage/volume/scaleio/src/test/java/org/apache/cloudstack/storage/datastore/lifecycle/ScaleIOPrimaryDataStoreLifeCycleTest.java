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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.provider.ScaleIOHostListener;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.exception.CloudRuntimeException;

@PrepareForTest(ScaleIOGatewayClient.class)
@RunWith(PowerMockRunner.class)
public class ScaleIOPrimaryDataStoreLifeCycleTest {

    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    private PrimaryDataStoreHelper dataStoreHelper;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private StoragePoolAutomation storagePoolAutomation;
    @Mock
    private HostDao hostDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;
    @Mock
    private DataStoreProviderManager dataStoreProviderMgr;
    @Mock
    private DataStoreProvider dataStoreProvider;
    @Mock
    private DataStoreManager dataStoreMgr;
    @Mock
    private PrimaryDataStore store;
    @Mock
    private TemplateManager templateMgr;
    @Mock
    private AgentManager agentMgr;
    @Mock
    ModifyStoragePoolAnswer answer;

    @Spy
    @InjectMocks
    private StorageManager storageMgr = new StorageManagerImpl();

    @Spy
    @InjectMocks
    private HypervisorHostListener hostListener = new ScaleIOHostListener();

    @InjectMocks
    private ScaleIOPrimaryDataStoreLifeCycle scaleIOPrimaryDataStoreLifeCycleTest;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testAttachZone() throws Exception {
        final DataStore dataStore = mock(DataStore.class);
        when(dataStore.getId()).thenReturn(1L);

        PowerMockito.mockStatic(ScaleIOGatewayClient.class);
        ScaleIOGatewayClientImpl client = mock(ScaleIOGatewayClientImpl.class);
        when(ScaleIOGatewayClientConnectionPool.getInstance().getClient(1L, storagePoolDetailsDao)).thenReturn(client);

        List<String> connectedSdcIps = new ArrayList<>();
        connectedSdcIps.add("192.168.1.1");
        connectedSdcIps.add("192.168.1.2");
        when(client.listConnectedSdcIps()).thenReturn(connectedSdcIps);
        when(client.isSdcConnected(anyString())).thenReturn(true);

        final ZoneScope scope = new ZoneScope(1L);

        List<HostVO> hostList = new ArrayList<HostVO>();
        HostVO host1 = new HostVO(1L, "host01", Host.Type.Routing, "192.168.1.1", "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null, null, 1L, null, 0, 0, "aa", 0, Storage.StoragePoolType.PowerFlex);
        HostVO host2 = new HostVO(2L, "host02", Host.Type.Routing, "192.168.1.2", "255.255.255.0", null, null, null, null, null, null, null, null, null, null,
                UUID.randomUUID().toString(), Status.Up, "1.0", null, null, 1L, null, 0, 0, "aa", 0, Storage.StoragePoolType.PowerFlex);

        host1.setResourceState(ResourceState.Enabled);
        host2.setResourceState(ResourceState.Enabled);
        hostList.add(host1);
        hostList.add(host2);
        when(resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.KVM, 1L)).thenReturn(hostList);

        when(dataStoreMgr.getDataStore(anyLong(), eq(DataStoreRole.Primary))).thenReturn(store);
        when(store.getId()).thenReturn(1L);
        when(store.getPoolType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        when(store.isShared()).thenReturn(true);
        when(store.getName()).thenReturn("ScaleIOPool");
        when(store.getStorageProviderName()).thenReturn(ScaleIOUtil.PROVIDER_NAME);

        when(dataStoreProviderMgr.getDataStoreProvider(ScaleIOUtil.PROVIDER_NAME)).thenReturn(dataStoreProvider);
        when(dataStoreProvider.getName()).thenReturn(ScaleIOUtil.PROVIDER_NAME);
        storageMgr.registerHostListener(ScaleIOUtil.PROVIDER_NAME, hostListener);

        when(agentMgr.easySend(anyLong(), Mockito.any(ModifyStoragePoolCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);

        when(storagePoolHostDao.findByPoolHost(anyLong(), anyLong())).thenReturn(null);

        when(hostDao.findById(1L)).thenReturn(host1);
        when(hostDao.findById(2L)).thenReturn(host2);

        when(dataStoreHelper.attachZone(Mockito.any(DataStore.class))).thenReturn(null);

        scaleIOPrimaryDataStoreLifeCycleTest.attachZone(dataStore, scope, Hypervisor.HypervisorType.KVM);
        verify(storageMgr,times(2)).connectHostToSharedPool(Mockito.any(Long.class), Mockito.any(Long.class));
        verify(storagePoolHostDao,times(2)).persist(Mockito.any(StoragePoolHostVO.class));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAttachZone_UnsupportedHypervisor() throws Exception {
        final DataStore dataStore = mock(DataStore.class);
        final ZoneScope scope = new ZoneScope(1L);
        scaleIOPrimaryDataStoreLifeCycleTest.attachZone(dataStore, scope, Hypervisor.HypervisorType.VMware);
    }

    @Test
    public void testMaintain() {
        final DataStore store = mock(DataStore.class);
        when(storagePoolAutomation.maintain(any(DataStore.class))).thenReturn(true);
        when(dataStoreHelper.maintain(any(DataStore.class))).thenReturn(true);
        final boolean result = scaleIOPrimaryDataStoreLifeCycleTest.maintain(store);
        assertThat(result).isTrue();
    }

    @Test
    public void testCancelMaintain() {
        final DataStore store = mock(DataStore.class);
        when(dataStoreHelper.cancelMaintain(any(DataStore.class))).thenReturn(true);
        when(storagePoolAutomation.cancelMaintain(any(DataStore.class))).thenReturn(true);
        final boolean result = scaleIOPrimaryDataStoreLifeCycleTest.cancelMaintain(store);
        assertThat(result).isTrue();
    }

    @Test
    public void testEnableStoragePool() {
        final DataStore dataStore = mock(DataStore.class);
        when(dataStoreHelper.enable(any(DataStore.class))).thenReturn(true);
        scaleIOPrimaryDataStoreLifeCycleTest.enableStoragePool(dataStore);
    }

    @Test
    public void testDisableStoragePool() {
        final DataStore dataStore = mock(DataStore.class);
        when(dataStoreHelper.disable(any(DataStore.class))).thenReturn(true);
        scaleIOPrimaryDataStoreLifeCycleTest.disableStoragePool(dataStore);
    }

    @Test
    public void testDeleteDataStoreWithStoragePoolNull() {
        final PrimaryDataStore store = mock(PrimaryDataStore.class);
        when(primaryDataStoreDao.findById(anyLong())).thenReturn(null);
        when(dataStoreHelper.deletePrimaryDataStore(any(DataStore.class))).thenReturn(true);
        final boolean result = scaleIOPrimaryDataStoreLifeCycleTest.deleteDataStore(store);
        assertThat(result).isFalse();
    }

    @Test
    public void testDeleteDataStore() {
        final PrimaryDataStore store = mock(PrimaryDataStore.class);
        final StoragePoolVO storagePoolVO = mock(StoragePoolVO.class);
        when(primaryDataStoreDao.findById(anyLong())).thenReturn(storagePoolVO);
        List<VMTemplateStoragePoolVO> unusedTemplates = new ArrayList<>();
        when(templateMgr.getUnusedTemplatesInPool(storagePoolVO)).thenReturn(unusedTemplates);
        List<StoragePoolHostVO> poolHostVOs = new ArrayList<>();
        when(storagePoolHostDao.listByPoolId(anyLong())).thenReturn(poolHostVOs);
        when(dataStoreHelper.deletePrimaryDataStore(any(DataStore.class))).thenReturn(true);
        final boolean result = scaleIOPrimaryDataStoreLifeCycleTest.deleteDataStore(store);
        assertThat(result).isTrue();
    }
}
