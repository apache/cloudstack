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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.http.ParseException;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

import net.bytebuddy.agent.VirtualMachine;

public class BackrollBackupProviderTest {
    @Mock
    BackrollClient clientMock;

    @Spy
    @InjectMocks
    BackrollBackupProvider backupProvider;

    @Mock
    BackrollBackupProvider backupProviderMock;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    BackupDao backupDao;

    @Before
    public void setUp() throws Exception {
        vmInstanceDao = mock(VMInstanceDao.class);
        clientMock = mock(BackrollClient.class);
        backupDao = mock(BackupDao.class);
        backupProviderMock = mock(BackrollBackupProvider.class);
        backupProvider = new BackrollBackupProvider(backupDao, vmInstanceDao, clientMock, Mockito.mock(Logger.class));
    }
    @Test
     public void syncBackups_Test() throws BackrollApiException, IOException  {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(2l);        
        vmInstanceVO.setBackupOfferingId(2l);
        Backup.Metric metric = new Backup.Metric(2L, 3L);

        BackupVO backupBackingUp = new BackupVO();
        backupBackingUp.setVmId(1l);
        backupBackingUp.setExternalId("abc,defgh");
        backupBackingUp.setType("Full");        
        backupBackingUp.setSize(2L);
        backupBackingUp.setZoneId(2l);
        backupBackingUp.setStatus(Backup.Status.BackingUp);

        BackupVO backupBackedUp = new BackupVO();
        backupBackedUp.setVmId(6l);
        backupBackedUp.setExternalId("abc,defgh");
        backupBackedUp.setType("Full");
        backupBackedUp.setZoneId(2l);       
        backupBackedUp.setSize(2L);
        backupBackedUp.setStatus(Backup.Status.BackedUp);

        BackupVO backupFailed = new BackupVO();
        backupFailed.setVmId(6l);
        backupFailed.setExternalId("abc,defgh");
        backupFailed.setType("Full");
        backupFailed.setZoneId(2l);       
        backupFailed.setSize(2L);
        backupFailed.setStatus(Backup.Status.Failed);

        BackrollTaskStatus backupStatus = new BackrollTaskStatus();
        backupStatus.setState(TaskState.SUCCESS);

        BackrollBackupMetrics metrics = new BackrollBackupMetrics(2L, 3L);

        List<BackrollVmBackup> backupsFromBackroll = Arrays.asList(
            new BackrollVmBackup("OK","OK", new Date()));

        List<Backup> backupsInDb = Arrays.asList(backupBackingUp, backupBackedUp, backupFailed);
        Mockito.doReturn(backupsInDb).when(backupDao).listByVmId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(backupStatus).when(clientMock).checkBackupTaskStatus(Mockito.anyString());
        Mockito.doReturn(metrics).when(clientMock).getBackupMetrics(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(new BackupVO()).when(backupDao).persist(Mockito.any(BackupVO.class));
        Mockito.doReturn(backupsFromBackroll).when(clientMock).getAllBackupsfromVirtualMachine(Mockito.anyString());
        Mockito.doReturn(true).when(backupDao).remove(Mockito.anyLong());

        backupProvider.syncBackups(vmInstanceVO, metric);
    }


    @Test
    public void getClient_Test() {
        BackrollClient client = backupProvider.getClient(2L);
        assertEquals(client, clientMock);
    }

    @Test
    public void deleteBackupTestSuccess_Test() throws BackrollApiException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(2l);
        vmInstanceVO.setUuid(UUID.randomUUID().toString());
        BackupVO backup = new BackupVO();
        backup.setVmId(1l);
        backup.setExternalId("abc,defgh");
        backup.setType("Full");
        backup.setZoneId(2l);
        backup.setStatus(Backup.Status.Removed);
        Boolean deleteBackupResponse = true;
        Mockito.doReturn(vmInstanceVO).when(vmInstanceDao).findByIdIncludingRemoved(Mockito.anyLong());
        Mockito.doReturn(deleteBackupResponse).when(clientMock).deleteBackup(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(new BackupVO()).when(backupDao).persist(Mockito.any(BackupVO.class));
        boolean result = backupProvider.deleteBackup(backup, true);
        assertEquals(true, result);
    }

    @Test
    public void deleteBackupBackinUp_Test() {
        BackupVO backup = new BackupVO();
        backup.setStatus(Backup.Status.BackingUp);
        try {
            backupProvider.deleteBackup(backup, false);
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("You can't delete a backup while it still BackingUp");
            assertEquals(expected , e.getMessage());
        }
    }
}