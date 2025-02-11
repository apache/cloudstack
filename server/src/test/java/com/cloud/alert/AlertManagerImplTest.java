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
import java.util.List;

import javax.mail.MessagingException;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

@RunWith(MockitoJUnitRunner.class)
public class AlertManagerImplTest {

    @Spy
    @InjectMocks
    AlertManagerImpl alertManagerImplMock;

    @Mock
    AlertDao alertDaoMock;

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

    private void sendMessage (){
        try {
            DataCenterVO zone = Mockito.mock(DataCenterVO.class);
            Mockito.when(zone.getId()).thenReturn(0L);
            Mockito.when(_dcDao.findById(0L)).thenReturn(zone);
            HostPodVO pod = Mockito.mock(HostPodVO.class);
            Mockito.when(pod.getId()).thenReturn(1L);
            Mockito.when(_podDao.findById(1L)).thenReturn(pod);
            ClusterVO cluster = Mockito.mock(ClusterVO.class);
            Mockito.when(cluster.getId()).thenReturn(1L);
            Mockito.when(_clusterDao.findById(1L)).thenReturn(cluster);

            alertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1l, 1l, "", "");
        } catch (UnsupportedEncodingException | MessagingException e) {
            Assert.fail();
        }
    }

    @Test
    public void sendAlertTestSendMail() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        alertManagerImplMock.recipients = new String [] {""};

        sendMessage();

        Mockito.verify(alertManagerImplMock).sendMessage(Mockito.any());
    }

    @Test
    public void sendAlertTestDebugLogging() {
        Mockito.doReturn(0).when(alertVOMock).getSentCount();
        Mockito.doReturn(alertVOMock).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());

        sendMessage();

        Mockito.verify(alertManagerImplMock.logger).debug(Mockito.anyString());
        Mockito.verify(alertManagerImplMock, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    public void sendAlertTestWarnLogging() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        alertManagerImplMock.recipients = null;

        sendMessage();

        Mockito.verify(alertManagerImplMock.logger, Mockito.times(2)).warn(Mockito.anyString());
        Mockito.verify(alertManagerImplMock, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    public void testRecalculateHostCapacities() {
        List<Long> mockHostIds = List.of(1L, 2L, 3L);
        Mockito.when(hostDao.listIdsByType(Host.Type.Routing)).thenReturn(mockHostIds);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(capacityManager).updateCapacityForHost(host);
        alertManagerImplMock.recalculateHostCapacities();
        Mockito.verify(hostDao, Mockito.times(3)).findById(Mockito.anyLong());
        Mockito.verify(capacityManager, Mockito.times(3)).updateCapacityForHost(host);
    }

    @Test
    public void testRecalculateStorageCapacities() {
        List<Long> mockPoolIds = List.of(101L, 102L, 103L);
        Mockito.when(primaryDataStoreDao.listAllIds()).thenReturn(mockPoolIds);
        StoragePoolVO sharedPool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(sharedPool.isShared()).thenReturn(true);
        Mockito.when(primaryDataStoreDao.findById(mockPoolIds.get(0))).thenReturn(sharedPool);
        Mockito.when(primaryDataStoreDao.findById(mockPoolIds.get(1))).thenReturn(sharedPool);
        StoragePoolVO nonSharedPool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(nonSharedPool.isShared()).thenReturn(false);
        Mockito.when(primaryDataStoreDao.findById(mockPoolIds.get(2))).thenReturn(nonSharedPool);
        Mockito.when(capacityManager.getAllocatedPoolCapacity(sharedPool, null)).thenReturn(10L);
        Mockito.when(capacityManager.getAllocatedPoolCapacity(nonSharedPool, null)).thenReturn(20L);
        alertManagerImplMock.recalculateStorageCapacities();
        Mockito.verify(storageManager, Mockito.times(2)).createCapacityEntry(sharedPool, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, 10L);
        Mockito.verify(storageManager, Mockito.times(1)).createCapacityEntry(nonSharedPool, Capacity.CAPACITY_TYPE_LOCAL_STORAGE, 20L);
    }
}
