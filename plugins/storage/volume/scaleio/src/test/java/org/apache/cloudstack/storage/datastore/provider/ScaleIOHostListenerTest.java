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
package org.apache.cloudstack.storage.datastore.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOHostListenerTest {

    @Mock
    private AgentManager agentMgr;
    @Mock
    private AlertManager alertMgr;
    @Mock
    private DataStoreManager dataStoreMgr;
    @Mock
    private HostDao hostDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;
    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    private ScaleIOSDCManager sdcManager;

    @Mock
    private HostVO host;
    @Mock
    private PrimaryDataStore pool;
    @Mock
    private ModifyStoragePoolAnswer mspAnswer;
    @Mock
    private StoragePoolDetailVO systemIdDetail;

    private ScaleIOHostListener listener;

    private static final long HOST_ID = 5L;
    private static final long POOL_ID = 10L;

    @Before
    public void setup() {
        listener = new ScaleIOHostListener();
        ReflectionTestUtils.setField(listener, "_agentMgr", agentMgr);
        ReflectionTestUtils.setField(listener, "_alertMgr", alertMgr);
        ReflectionTestUtils.setField(listener, "_dataStoreMgr", dataStoreMgr);
        ReflectionTestUtils.setField(listener, "_hostDao", hostDao);
        ReflectionTestUtils.setField(listener, "_storagePoolHostDao", storagePoolHostDao);
        ReflectionTestUtils.setField(listener, "_storagePoolDetailsDao", storagePoolDetailsDao);
        ReflectionTestUtils.setField(listener, "_sdcManager", sdcManager);
    }

    private void setUpHostAndPoolFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(pool);
        when(pool.getId()).thenReturn(POOL_ID);
    }

    private void setUpSystemId(String systemId) {
        if (systemId == null) {
            when(storagePoolDetailsDao.findDetail(POOL_ID, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(null);
        } else {
            when(storagePoolDetailsDao.findDetail(POOL_ID, ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID)).thenReturn(systemIdDetail);
            when(systemIdDetail.getValue()).thenReturn(systemId);
        }
    }

    private StoragePoolInfo poolInfoWithDetails(Map<String, String> details) {
        return new StoragePoolInfo("uuid", "hostAddr", "/host/path", "/local/path",
                Storage.StoragePoolType.NetworkFilesystem, 1000L, 500L, details);
    }

    private MockedStatic<ComponentContext> mockComponentContextInject() {
        MockedStatic<ComponentContext> mocked = mockStatic(ComponentContext.class);
        mocked.when(() -> ComponentContext.inject(any(ScaleIOSDCManager.class))).thenAnswer(inv -> inv.getArgument(0));
        return mocked;
    }

    // ---- hostAdded ----

    @Test
    public void testHostAddedReturnsTrue() {
        assertTrue(listener.hostAdded(HOST_ID));
    }

    // ---- hostConnect ----

    @Test
    public void testHostConnectReturnsFalseWhenHostNotFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(null);

        boolean result = listener.hostConnect(HOST_ID, POOL_ID);

        assertFalse(result);
        verify(dataStoreMgr, never()).getDataStore(anyLong(), any(DataStoreRole.class));
    }

    @Test
    public void testHostConnectThrowsWhenSystemIdMissing() {
        setUpHostAndPoolFound();
        setUpSystemId(null);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> listener.hostConnect(HOST_ID, POOL_ID));

        assertTrue(ex.getMessage().contains("Failed to get the system id"));
    }

    @Test
    public void testHostConnectThrowsWhenAgentAnswerIsNull() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(null);

        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            assertThrows(CloudRuntimeException.class, () -> listener.hostConnect(HOST_ID, POOL_ID));
        }
    }

    @Test
    public void testHostConnectSendsAlertAndThrowsWhenAnswerFails() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(host.toString()).thenReturn("Host-5");
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(false);

        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            assertThrows(CloudRuntimeException.class, () -> listener.hostConnect(HOST_ID, POOL_ID));
        }

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), anyLong(), any(), subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Unable to attach"));
        assertTrue(subjectCaptor.getValue().contains("Host-5"));
    }

    @Test
    public void testHostConnectDeletesExistingStoragePoolHostWhenPoolDetailsEmpty() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(host.toString()).thenReturn("Host-5");
        StoragePoolHostVO existing = new StoragePoolHostVO(POOL_ID, HOST_ID, "/old/path");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(existing);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(null));

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        verify(storagePoolHostDao).deleteStoragePoolHostDetails(HOST_ID, POOL_ID);
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertService.AlertType.ALERT_TYPE_HOST), anyLong(), any(), subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Host-5"));
    }

    @Test
    public void testHostConnectDoesNotDeleteWhenNoExistingStoragePoolHostAndPoolDetailsEmpty() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(new HashMap<>()));

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        verify(storagePoolHostDao, never()).deleteStoragePoolHostDetails(anyLong(), anyLong());
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void testHostConnectPersistsNewStoragePoolHostWhenSdcIdFound() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.SDC_ID, "sdc-123");
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(details));

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        ArgumentCaptor<StoragePoolHostVO> captor = ArgumentCaptor.forClass(StoragePoolHostVO.class);
        verify(storagePoolHostDao).persist(captor.capture());
        assertEquals("sdc-123", captor.getValue().getLocalPath());
        assertEquals(POOL_ID, captor.getValue().getPoolId());
        assertEquals(HOST_ID, captor.getValue().getHostId());
    }

    @Test
    public void testHostConnectUpdatesExistingStoragePoolHostWhenSdcIdFound() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        StoragePoolHostVO existing = new StoragePoolHostVO(POOL_ID, HOST_ID, "/old/path");
        ReflectionTestUtils.setField(existing, "id", 42L);
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(existing);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.SDC_ID, "sdc-456");
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(details));

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        assertEquals("sdc-456", existing.getLocalPath());
        verify(storagePoolHostDao).update(eq(42L), eq(existing));
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void testHostConnectDelegatesToSdcManagerWhenOnlySdcGuidPresent() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.SDC_GUID, "guid-789");
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(details));
        when(sdcManager.getHostSdcId(eq("guid-789"), any(DataStore.class))).thenReturn("sdc-from-guid");

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        verify(sdcManager).getHostSdcId(eq("guid-789"), any(DataStore.class));
        ArgumentCaptor<StoragePoolHostVO> captor = ArgumentCaptor.forClass(StoragePoolHostVO.class);
        verify(storagePoolHostDao).persist(captor.capture());
        assertEquals("sdc-from-guid", captor.getValue().getLocalPath());
    }

    @Test
    public void testHostConnectSendsAlertWhenSdcDetailsMissingFromBothKeys() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(host.toString()).thenReturn("Host-5");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        Map<String, String> details = new HashMap<>();
        details.put("some.other.key", "value");
        when(mspAnswer.getPoolInfo()).thenReturn(poolInfoWithDetails(details));

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostConnect(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertService.AlertType.ALERT_TYPE_HOST), anyLong(), any(), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("Couldn't retrieve"));
        assertTrue(bodyCaptor.getValue().contains("Host-5"));
    }

    // Note: the ConnectOnDemand-disabled-and-SDC-not-connected branch (ScaleIOSDCManager.ConnectOnDemand.valueIn(...)
    // together with sdcManager.isHostSdcConnected(...)) is intentionally not covered here. ConnectOnDemand is a
    // static ConfigKey whose value() falls back to a process-wide static ConfigDepot (ConfigKey.s_depot) that may
    // already have been initialised as a side effect of other tests running earlier in the same JVM/module test
    // run, making the outcome of valueIn(...) order-dependent and awkward to control from a plain Mockito unit
    // test without also mocking static state shared across the whole test module (see the same rationale
    // documented in engine/storage/volume DefaultHostListenerTest for the analogous CLVMSecureZeroFill branch).

    // ---- hostDisconnected ----

    @Test
    public void testHostDisconnectedReturnsFalseWhenHostNotFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(null);

        boolean result = listener.hostDisconnected(HOST_ID, POOL_ID);

        assertFalse(result);
    }

    @Test
    public void testHostDisconnectedThrowsWhenSystemIdMissing() {
        setUpHostAndPoolFound();
        setUpSystemId(null);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                () -> listener.hostDisconnected(HOST_ID, POOL_ID));

        assertTrue(ex.getMessage().contains("Failed to get the system id"));
    }

    @Test
    public void testHostDisconnectedReturnsFalseAndSendsAlertWhenAnswerFails() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        when(host.toString()).thenReturn("Host-5");
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(false);

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostDisconnected(HOST_ID, POOL_ID);
        }

        assertFalse(result);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), anyLong(), any(), subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Unable to detach"));
        assertTrue(subjectCaptor.getValue().contains("Host-5"));
        verify(storagePoolHostDao, never()).deleteStoragePoolHostDetails(anyLong(), anyLong());
    }

    @Test
    public void testHostDisconnectedDeletesStoragePoolHostAndReturnsTrue() {
        setUpHostAndPoolFound();
        setUpSystemId("system-1");
        StoragePoolHostVO existing = new StoragePoolHostVO(POOL_ID, HOST_ID, "/some/path");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(existing);
        when(agentMgr.easySend(eq(HOST_ID), any(ModifyStoragePoolCommand.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);

        boolean result;
        try (MockedStatic<ComponentContext> mocked = mockComponentContextInject()) {
            result = listener.hostDisconnected(HOST_ID, POOL_ID);
        }

        assertTrue(result);
        verify(storagePoolHostDao).deleteStoragePoolHostDetails(HOST_ID, POOL_ID);
    }

    // ---- remaining trivial lifecycle callbacks ----

    @Test
    public void testHostAboutToBeRemovedReturnsTrue() {
        assertTrue(listener.hostAboutToBeRemoved(HOST_ID));
    }

    @Test
    public void testHostRemovedReturnsTrue() {
        assertTrue(listener.hostRemoved(HOST_ID, 99L));
    }

    @Test
    public void testHostEnabledReturnsTrue() {
        assertTrue(listener.hostEnabled(HOST_ID));
    }
}
