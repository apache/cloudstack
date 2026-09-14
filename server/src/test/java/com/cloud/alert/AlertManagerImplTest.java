// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.alert;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

import javax.mail.MessagingException;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.alert.dao.AlertDao;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.StorageManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AlertManagerImplTest {

    @Spy
    @InjectMocks
    AlertManagerImpl alertManagerImplMock;

    @Mock
    AlertDao _alertDao;

    @Mock
    private DataCenterDao _dcDao;

    @Mock
    private HostPodDao _podDao;

    @Mock
    private ClusterDao _clusterDao;

    @Mock
    AlertVO alertVOMock;

    @Mock
    HostDao hostDao;

    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    CapacityManager capacityManager;

    @Mock
    StorageManager storageManager;

    @Mock
    Logger loggerMock;

    @Mock
    SMTPMailSender mailSenderMock;

    private final String[] recipients = new String[]{"test@test.com"};
    private final String senderAddress = "sender@test.com";

    @Before
    public void setUp() {
        alertManagerImplMock.recipients = recipients;
        alertManagerImplMock.senderAddress = senderAddress;
    }

    private void sendMessage() {
        try {
            DataCenterVO zone = mock(DataCenterVO.class);
            when(zone.getId()).thenReturn(0L);
            when(_dcDao.findById(0L)).thenReturn(zone);
            HostPodVO pod = mock(HostPodVO.class);
            when(pod.getId()).thenReturn(1L);
            when(_podDao.findById(1L)).thenReturn(pod);
            ClusterVO cluster = mock(ClusterVO.class);
            when(cluster.getId()).thenReturn(1L);
            when(_clusterDao.findById(1L)).thenReturn(cluster);

            alertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1L, 1L, "", "");
        } catch (UnsupportedEncodingException | MessagingException e) {
            Assert.fail();
        }
    }

    @Test
    public void sendAlertTestSendMail() {
        doReturn(null).when(_alertDao).getLastAlert(anyShort(), anyLong(),
                anyLong(), anyLong());
        doReturn(null).when(_alertDao).persist(any());
        alertManagerImplMock.recipients = new String[]{""};

        sendMessage();

        verify(alertManagerImplMock).sendMessage(any());
    }

    @Test
    public void sendAlertTestDebugLogging() {
        doReturn(0).when(alertVOMock).getSentCount();
        doReturn(alertVOMock).when(_alertDao).getLastAlert(anyShort(), anyLong(),
                anyLong(), anyLong());

        sendMessage();

        verify(alertManagerImplMock.logger).debug(anyString());
        verify(alertManagerImplMock, never()).sendMessage(any());
    }

    @Test
    public void sendAlertTestWarnLogging() {
        doReturn(null).when(_alertDao).getLastAlert(anyShort(), anyLong(),
                anyLong(), anyLong());
        doReturn(null).when(_alertDao).persist(any());
        alertManagerImplMock.recipients = null;

        sendMessage();

        verify(alertManagerImplMock.logger, times(2)).warn(anyString());
        verify(alertManagerImplMock, never()).sendMessage(any());
    }

    @Test
    public void testSendAlertWithNullParameters() throws MessagingException, UnsupportedEncodingException {
        // Given
        String subject = "Test Subject";
        String content = "Test Content";
        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_MEMORY;

        // When
        alertManagerImplMock.sendAlert(alertType, null, null, null, subject, content);

        // Then
        ArgumentCaptor<AlertVO> alertCaptor = ArgumentCaptor.forClass(AlertVO.class);
        verify(_alertDao).persist(alertCaptor.capture());

        AlertVO capturedAlert = alertCaptor.getValue();
        assertNotNull("Captured alert should not be null", capturedAlert);
        assertEquals(0L, capturedAlert.getDataCenterId());
        assertNull(capturedAlert.getPodId());
        assertNull(capturedAlert.getClusterId());
        assertEquals(subject, capturedAlert.getSubject());
        assertEquals(content, capturedAlert.getContent());
        assertEquals(alertType.getType(), capturedAlert.getType());
    }

    @Test(expected = NullPointerException.class)
    public void testSendAlertWithNullAlertType() throws MessagingException, UnsupportedEncodingException {
        // When
        alertManagerImplMock.sendAlert(null, 0, 1L, 1L, "subject", "content");
    }

    @Test
    public void testRecalculateHostCapacities() {
        List<Long> mockHostIds = List.of(1L, 2L, 3L);
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(mockHostIds);
        HostVO host = mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(host);
        doNothing().when(capacityManager).updateCapacityForHost(host);
        alertManagerImplMock.recalculateHostCapacities();
        verify(hostDao, times(3)).findById(anyLong());
        verify(capacityManager, times(3)).updateCapacityForHost(host);
    }

    @Test
    public void testRecalculateStorageCapacities() {
        List<Long> mockPoolIds = List.of(101L, 102L, 103L);
        when(primaryDataStoreDao.listAllIds()).thenReturn(mockPoolIds);
        StoragePoolVO sharedPool = mock(StoragePoolVO.class);
        when(sharedPool.isShared()).thenReturn(true);
        when(primaryDataStoreDao.findById(mockPoolIds.get(0))).thenReturn(sharedPool);
        when(primaryDataStoreDao.findById(mockPoolIds.get(1))).thenReturn(sharedPool);
        StoragePoolVO nonSharedPool = mock(StoragePoolVO.class);
        when(nonSharedPool.isShared()).thenReturn(false);
        when(primaryDataStoreDao.findById(mockPoolIds.get(2))).thenReturn(nonSharedPool);
        when(capacityManager.getAllocatedPoolCapacity(sharedPool, null)).thenReturn(10L);
        when(capacityManager.getAllocatedPoolCapacity(nonSharedPool, null)).thenReturn(20L);
        alertManagerImplMock.recalculateStorageCapacities();
        verify(storageManager, times(2)).createCapacityEntry(sharedPool, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, 10L);
        verify(storageManager, times(1)).createCapacityEntry(nonSharedPool, Capacity.CAPACITY_TYPE_LOCAL_STORAGE, 20L);
    }

    @Test
    public void testRecalculateHostCapacitiesWithEmptyHostList() throws Exception {
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(List.of());
        alertManagerImplMock.recalculateHostCapacities();
        verify(hostDao, never()).findById(anyLong());
        verify(capacityManager, never()).updateCapacityForHost(any());
        assertNull("executor should never be created when there is nothing to submit", getCapacityExecutorService());
    }

    @Test
    public void testRecalculateStorageCapacitiesWithEmptyPoolList() throws Exception {
        when(primaryDataStoreDao.listAllIds()).thenReturn(List.of());
        alertManagerImplMock.recalculateStorageCapacities();
        verify(primaryDataStoreDao, never()).findById(anyLong());
        verify(storageManager, never()).createCapacityEntry(any(), anyShort(), anyLong());
        assertNull("executor should never be created when there is nothing to submit", getCapacityExecutorService());
    }

    @Test
    public void testRecalculateHostCapacitiesLogsAndContinuesOnTaskFailure() {
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(List.of(1L, 2L, 3L));
        HostVO host1 = mock(HostVO.class);
        HostVO host2 = mock(HostVO.class);
        HostVO host3 = mock(HostVO.class);
        when(hostDao.findById(1L)).thenReturn(host1);
        when(hostDao.findById(2L)).thenReturn(host2);
        when(hostDao.findById(3L)).thenReturn(host3);
        doThrow(new RuntimeException("boom")).when(capacityManager).updateCapacityForHost(host2);

        alertManagerImplMock.recalculateHostCapacities();

        verify(capacityManager).updateCapacityForHost(host1);
        verify(capacityManager).updateCapacityForHost(host2);
        verify(capacityManager).updateCapacityForHost(host3);
        verify(alertManagerImplMock.logger).error(anyString(), any(Throwable.class));
    }

    @Test
    public void testRecalculateHostCapacitiesReusesExecutorAcrossCalls() throws Exception {
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(List.of(1L));
        HostVO hostMock = mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(hostMock);

        alertManagerImplMock.recalculateHostCapacities();
        ExecutorService firstExecutor = getCapacityExecutorService();
        assertNotNull(firstExecutor);

        when(primaryDataStoreDao.listAllIds()).thenReturn(List.of(101L));
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(primaryDataStoreDao.findById(101L)).thenReturn(pool);
        alertManagerImplMock.recalculateStorageCapacities();

        assertEquals("host and storage recalculation should share the same long-lived pool",
                firstExecutor, getCapacityExecutorService());
    }

    @Test
    public void testRecalculateHostCapacitiesRecreatesExecutorAfterShutdown() throws Exception {
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(List.of(1L));
        HostVO hostMock = mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(hostMock);

        alertManagerImplMock.recalculateHostCapacities();
        ExecutorService firstExecutor = getCapacityExecutorService();
        firstExecutor.shutdown();

        alertManagerImplMock.recalculateHostCapacities();
        ExecutorService secondExecutor = getCapacityExecutorService();

        Assert.assertNotEquals("a shut down executor should be replaced rather than reused", firstExecutor, secondExecutor);
        Assert.assertFalse(secondExecutor.isShutdown());
    }

    @Test
    public void testStopShutsDownCapacityExecutorServiceWhenPresent() throws Exception {
        Timer timerMock = mock(Timer.class);
        setTimer(timerMock);
        when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(List.of(1L));
        HostVO hostMock = mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(hostMock);
        alertManagerImplMock.recalculateHostCapacities();

        boolean result = alertManagerImplMock.stop();

        Assert.assertTrue(result);
        verify(timerMock).cancel();
        Assert.assertTrue(getCapacityExecutorService().isShutdown());
    }

    @Test
    public void testStopDoesNotThrowWhenCapacityExecutorServiceNeverCreated() throws Exception {
        Timer timerMock = mock(Timer.class);
        setTimer(timerMock);

        boolean result = alertManagerImplMock.stop();

        Assert.assertTrue(result);
        verify(timerMock).cancel();
        assertNull(getCapacityExecutorService());
    }

    private ExecutorService getCapacityExecutorService() throws Exception {
        Field field = AlertManagerImpl.class.getDeclaredField("capacityExecutorService");
        field.setAccessible(true);
        return (ExecutorService) field.get(alertManagerImplMock);
    }

    private void setTimer(Timer timer) throws Exception {
        Field field = AlertManagerImpl.class.getDeclaredField("_timer");
        field.setAccessible(true);
        field.set(alertManagerImplMock, timer);
    }
}
