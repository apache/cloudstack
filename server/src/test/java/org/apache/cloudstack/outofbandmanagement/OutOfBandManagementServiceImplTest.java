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
package org.apache.cloudstack.outofbandmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceState;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class OutOfBandManagementServiceImplTest {

    private static final String OOBM_ENABLED_DETAIL = "outOfBandManagementEnabled";

    @Mock
    private ClusterDao clusterDao;
    @Mock
    private ClusterDetailsDao clusterDetailsDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private DataCenterDetailsDao dataCenterDetailsDao;
    @Mock
    private OutOfBandManagementDao outOfBandManagementDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private AlertManager alertMgr;
    @Mock
    private BackgroundPollManager backgroundPollManager;

    @Mock
    private HostVO host;
    @Mock
    private DataCenter zone;
    @Mock
    private Cluster cluster;

    private OutOfBandManagementServiceImpl service;

    private static Field cacheField;
    private static Field executorField;
    private static Object originalCache;
    private static Object originalExecutor;

    @BeforeClass
    public static void setUpStaticFields() throws Exception {
        cacheField = OutOfBandManagementServiceImpl.class.getDeclaredField("hostAlertCache");
        cacheField.setAccessible(true);
        originalCache = cacheField.get(null);
        cacheField.set(null, CacheBuilder.newBuilder().build());

        executorField = OutOfBandManagementServiceImpl.class.getDeclaredField("backgroundSyncBlockingExecutor");
        executorField.setAccessible(true);
        originalExecutor = executorField.get(null);
        executorField.set(null, Executors.newSingleThreadExecutor());
    }

    @AfterClass
    public static void tearDownStaticFields() throws Exception {
        ExecutorService executor = (ExecutorService) executorField.get(null);
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        cacheField.set(null, originalCache);
        executorField.set(null, originalExecutor);
    }

    @Before
    public void setUp() {
        service = new OutOfBandManagementServiceImpl();
        ReflectionTestUtils.setField(service, "clusterDao", clusterDao);
        ReflectionTestUtils.setField(service, "clusterDetailsDao", clusterDetailsDao);
        ReflectionTestUtils.setField(service, "dataCenterDao", dataCenterDao);
        ReflectionTestUtils.setField(service, "dataCenterDetailsDao", dataCenterDetailsDao);
        ReflectionTestUtils.setField(service, "outOfBandManagementDao", outOfBandManagementDao);
        ReflectionTestUtils.setField(service, "hostDao", hostDao);
        ReflectionTestUtils.setField(service, "alertMgr", alertMgr);
        ReflectionTestUtils.setField(service, "backgroundPollManager", backgroundPollManager);
    }

    private boolean invokeIsOutOfBandManagementEnabledForHost(Long hostId) throws Exception {
        Method m = OutOfBandManagementServiceImpl.class.getDeclaredMethod("isOutOfBandManagementEnabledForHost", Long.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, hostId);
    }

    // ---------- isOutOfBandManagementEnabled(Host) and private helpers ----------

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseForNullHost() {
        assertFalse(service.isOutOfBandManagementEnabled(null));
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenZoneDisabled() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(new DataCenterDetailVO(1L, OOBM_ENABLED_DETAIL, "false", true));

        assertFalse(service.isOutOfBandManagementEnabled(host));
        verify(clusterDetailsDao, never()).findDetail(anyLong(), any());
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenClusterDisabled() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(new ClusterDetailsVO(2L, OOBM_ENABLED_DETAIL, "false"));

        assertFalse(service.isOutOfBandManagementEnabled(host));
        verify(hostDao, never()).findById(anyLong());
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenHostIsDegraded() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(host.getId()).thenReturn(3L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(hostDao.findById(3L)).thenReturn(host);
        when(host.getResourceState()).thenReturn(ResourceState.Degraded);

        assertFalse(service.isOutOfBandManagementEnabled(host));
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenHostWasRemoved() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(host.getId()).thenReturn(3L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(hostDao.findById(3L)).thenReturn(null);

        assertFalse(service.isOutOfBandManagementEnabled(host));
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenNoConfig() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(host.getId()).thenReturn(3L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(hostDao.findById(3L)).thenReturn(host);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        when(outOfBandManagementDao.findByHost(3L)).thenReturn(null);

        assertFalse(service.isOutOfBandManagementEnabled(host));
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsFalseWhenConfigDisabled() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(host.getId()).thenReturn(3L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(null);
        when(hostDao.findById(3L)).thenReturn(host);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        OutOfBandManagementVO config = new OutOfBandManagementVO(3L);
        config.setEnabled(false);
        when(outOfBandManagementDao.findByHost(3L)).thenReturn(config);

        assertFalse(service.isOutOfBandManagementEnabled(host));
    }

    @Test
    public void isOutOfBandManagementEnabledReturnsTrueWhenFullyEnabled() {
        when(host.getDataCenterId()).thenReturn(1L);
        when(host.getClusterId()).thenReturn(2L);
        when(host.getId()).thenReturn(3L);
        when(dataCenterDetailsDao.findDetail(1L, OOBM_ENABLED_DETAIL)).thenReturn(new DataCenterDetailVO(1L, OOBM_ENABLED_DETAIL, "true", true));
        when(clusterDetailsDao.findDetail(2L, OOBM_ENABLED_DETAIL)).thenReturn(new ClusterDetailsVO(2L, OOBM_ENABLED_DETAIL, "true"));
        when(hostDao.findById(3L)).thenReturn(host);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        OutOfBandManagementVO config = new OutOfBandManagementVO(3L);
        config.setEnabled(true);
        when(outOfBandManagementDao.findByHost(3L)).thenReturn(config);

        assertTrue(service.isOutOfBandManagementEnabled(host));
    }

    @Test
    public void isOutOfBandManagementEnabledForHostReturnsFalseForNullHostId() throws Exception {
        assertFalse(invokeIsOutOfBandManagementEnabledForHost(null));
    }

    // ---------- enable/disable on a zone ----------

    @Test
    public void enableOutOfBandManagementZonePersistsEnabledDetail() {
        when(zone.getId()).thenReturn(10L);

        OutOfBandManagementResponse response = service.enableOutOfBandManagement(zone);

        verify(dataCenterDetailsDao).persist(10L, OOBM_ENABLED_DETAIL, String.valueOf(true));
        assertTrue(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    @Test
    public void disableOutOfBandManagementZonePersistsDisabledDetailAndTransitionsHosts() {
        when(zone.getId()).thenReturn(10L);
        when(hostDao.listIdsByDataCenterId(10L)).thenReturn(Arrays.asList(1L, 2L));
        // use a state from which a real "Disabled" transition exists (On/Off/Unknown) so that
        // transitionPowerStateToDisabled()'s short-circuiting "result = result && transitionPowerState(...)"
        // does not skip the second host once the first one is processed.
        OutOfBandManagementVO config1 = mock(OutOfBandManagementVO.class);
        when(config1.getState()).thenReturn(OutOfBandManagement.PowerState.On);
        when(config1.getHostId()).thenReturn(1L);
        OutOfBandManagementVO config2 = mock(OutOfBandManagementVO.class);
        when(config2.getState()).thenReturn(OutOfBandManagement.PowerState.On);
        when(config2.getHostId()).thenReturn(2L);
        when(outOfBandManagementDao.findByHost(1L)).thenReturn(config1);
        when(outOfBandManagementDao.findByHost(2L)).thenReturn(config2);
        when(outOfBandManagementDao.updateState(any(), any(), any(), any(), any())).thenReturn(true);

        OutOfBandManagementResponse response = service.disableOutOfBandManagement(zone);

        verify(dataCenterDetailsDao).persist(10L, OOBM_ENABLED_DETAIL, String.valueOf(false));
        verify(outOfBandManagementDao).findByHost(1L);
        verify(outOfBandManagementDao).findByHost(2L);
        assertFalse(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    // ---------- enable/disable on a cluster ----------

    @Test
    public void enableOutOfBandManagementClusterPersistsEnabledDetail() {
        when(cluster.getId()).thenReturn(20L);

        OutOfBandManagementResponse response = service.enableOutOfBandManagement(cluster);

        verify(clusterDetailsDao).persist(20L, OOBM_ENABLED_DETAIL, String.valueOf(true));
        assertTrue(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    @Test
    public void disableOutOfBandManagementClusterPersistsDisabledDetailAndTransitionsHosts() {
        when(cluster.getId()).thenReturn(20L);
        when(hostDao.listIdsByClusterId(20L)).thenReturn(Collections.singletonList(5L));
        OutOfBandManagementVO config = mock(OutOfBandManagementVO.class);
        when(config.getState()).thenReturn(OutOfBandManagement.PowerState.On);
        when(config.getHostId()).thenReturn(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(config);
        when(outOfBandManagementDao.updateState(any(), any(), any(), any(), any())).thenReturn(true);

        OutOfBandManagementResponse response = service.disableOutOfBandManagement(cluster);

        verify(clusterDetailsDao).persist(20L, OOBM_ENABLED_DETAIL, String.valueOf(false));
        verify(outOfBandManagementDao).findByHost(5L);
        assertFalse(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    // ---------- enable/disable on a host (exercises static hostAlertCache) ----------

    @Test
    public void enableOutOfBandManagementHostThrowsWhenNoConfig() {
        when(host.getId()).thenReturn(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> service.enableOutOfBandManagement(host));
    }

    @Test
    public void disableOutOfBandManagementHostThrowsWhenNoConfig() {
        when(host.getId()).thenReturn(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> service.disableOutOfBandManagement(host));
    }

    @Test
    public void enableOutOfBandManagementHostInvalidatesCacheAndPersists() {
        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO config = new OutOfBandManagementVO(5L);
        config.setEnabled(false);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(config);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(true);

        OutOfBandManagementResponse response = service.enableOutOfBandManagement(host);

        assertTrue(config.isEnabled());
        verify(outOfBandManagementDao).update(anyLong(), eq(config));
        // called once from getConfigForHost() and again from transitionPowerStateToDisabled()
        verify(outOfBandManagementDao, times(2)).findByHost(5L);
        assertTrue(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    @Test
    public void disableOutOfBandManagementHostInvalidatesCacheAndPersists() {
        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO config = new OutOfBandManagementVO(5L);
        config.setEnabled(true);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(config);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(true);

        OutOfBandManagementResponse response = service.disableOutOfBandManagement(host);

        assertFalse(config.isEnabled());
        verify(outOfBandManagementDao).update(anyLong(), eq(config));
        verify(outOfBandManagementDao, times(2)).findByHost(5L);
        assertFalse(response.getEnabled());
        assertTrue(response.getSuccess());
    }

    @Test
    public void enableOutOfBandManagementHostSkipsTransitionWhenUpdateFails() {
        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO config = new OutOfBandManagementVO(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(config);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(false);

        service.enableOutOfBandManagement(host);

        // only the initial getConfigForHost() lookup, no transitionPowerStateToDisabled() lookup
        verify(outOfBandManagementDao, times(1)).findByHost(5L);
    }

    // ---------- configure(Host, options) ----------

    @Test
    public void configureHostCreatesNewConfigWhenNoneExists() {
        OutOfBandManagementDriver driver = mock(OutOfBandManagementDriver.class);
        when(driver.getName()).thenReturn("ipmitool");
        service.setOutOfBandManagementDrivers(Collections.singletonList(driver));
        service.start();

        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO persisted = new OutOfBandManagementVO(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(null, persisted);
        when(outOfBandManagementDao.persist(any(OutOfBandManagementVO.class))).thenReturn(persisted);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(true);

        ImmutableMap<OutOfBandManagement.Option, String> options = ImmutableMap.of(
                OutOfBandManagement.Option.DRIVER, "ipmitool",
                OutOfBandManagement.Option.ADDRESS, "1.2.3.4");

        OutOfBandManagementResponse response = service.configure(host, options);

        verify(outOfBandManagementDao).persist(any(OutOfBandManagementVO.class));
        assertEquals("ipmitool", persisted.getDriver());
        assertEquals("1.2.3.4", persisted.getAddress());
        assertTrue(response.getSuccess());
    }

    @Test
    public void configureHostUpdatesExistingConfig() {
        OutOfBandManagementDriver driver = mock(OutOfBandManagementDriver.class);
        when(driver.getName()).thenReturn("ipmitool");
        service.setOutOfBandManagementDrivers(Collections.singletonList(driver));
        service.start();

        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO existing = new OutOfBandManagementVO(5L);
        existing.setDriver("ipmitool");
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(existing);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(true);

        ImmutableMap<OutOfBandManagement.Option, String> options = ImmutableMap.of(OutOfBandManagement.Option.ADDRESS, "5.6.7.8");

        OutOfBandManagementResponse response = service.configure(host, options);

        verify(outOfBandManagementDao, never()).persist(any(OutOfBandManagementVO.class));
        verify(outOfBandManagementDao).update(anyLong(), eq(existing));
        assertEquals("5.6.7.8", existing.getAddress());
        assertTrue(response.getSuccess());
    }

    @Test
    public void configureHostThrowsWhenDriverMissingOrInvalid() {
        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO existing = new OutOfBandManagementVO(5L);
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(existing);

        ImmutableMap<OutOfBandManagement.Option, String> options = ImmutableMap.of(OutOfBandManagement.Option.DRIVER, "no-such-driver");

        assertThrows(CloudRuntimeException.class, () -> service.configure(host, options));
        verify(outOfBandManagementDao, never()).update(anyLong(), any(OutOfBandManagementVO.class));
    }

    @Test
    public void configureHostThrowsWhenUpdateFails() {
        OutOfBandManagementDriver driver = mock(OutOfBandManagementDriver.class);
        when(driver.getName()).thenReturn("ipmitool");
        service.setOutOfBandManagementDrivers(Collections.singletonList(driver));
        service.start();

        when(host.getId()).thenReturn(5L);
        OutOfBandManagementVO existing = new OutOfBandManagementVO(5L);
        existing.setDriver("ipmitool");
        when(outOfBandManagementDao.findByHost(5L)).thenReturn(existing);
        when(outOfBandManagementDao.update(anyLong(), any(OutOfBandManagementVO.class))).thenReturn(false);

        ImmutableMap<OutOfBandManagement.Option, String> options = ImmutableMap.of();

        assertThrows(CloudRuntimeException.class, () -> service.configure(host, options));
    }

    // ---------- trivial getters/setters ----------

    @Test
    public void getNameReturnsConfiguredName() {
        ReflectionTestUtils.setField(service, "name", "OutOfBandManagementService");
        assertEquals("OutOfBandManagementService", service.getName());
    }

    @Test
    public void getIdReturnsConfiguredServiceId() {
        ReflectionTestUtils.setField(service, "serviceId", 99L);
        assertEquals(99L, service.getId());
    }

    @Test
    public void getConfigComponentNameReturnsSimpleClassName() {
        assertEquals("OutOfBandManagementServiceImpl", service.getConfigComponentName());
    }

    @Test
    public void getConfigKeysReturnsAllConfigKeys() {
        assertNotNull(service.getConfigKeys());
        assertEquals(3, service.getConfigKeys().length);
    }
}
