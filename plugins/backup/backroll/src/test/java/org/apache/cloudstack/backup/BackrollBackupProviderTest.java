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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.Backup.RestorePoint;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.framework.config.ConfigKey;

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
    private ConfigKey<String> BackrollUrlConfigKey;

    @Mock
    private ConfigKey<String> BackrollAppNameConfigKey;

    @Mock
    private ConfigKey<String> BackrollPasswordConfigKey;

    @Mock
    BackupDao backupDao;

    @Before
    public void setUp() throws Exception {
        vmInstanceDao = mock(VMInstanceDao.class);
        clientMock = mock(BackrollClient.class);
        backupDao = mock(BackupDao.class);
        backupProvider = new BackrollBackupProvider(backupDao, vmInstanceDao, clientMock, Mockito.mock(Logger.class));
        backupProvider.BackrollAppNameConfigKey = BackrollAppNameConfigKey;
        backupProvider.BackrollPasswordConfigKey = BackrollPasswordConfigKey;
        backupProvider.BackrollUrlConfigKey = BackrollUrlConfigKey;
    }

    @Test
    public void listBackupOfferings_Test() throws BackrollApiException, IOException {
        Mockito.doReturn("dummyUrlToRequest").when(clientMock).getBackupOfferingUrl();
        Mockito.doReturn(Arrays.asList(new BackrollOffering("dummyName", "dummyId"))).when(clientMock)
                .getBackupOfferings(Mockito.anyString());
        List<BackupOffering> results = backupProvider.listBackupOfferings(2L);
        assertTrue(results.size() == 1);
    }

    @Test
    public void takeBackup_Test() throws BackrollApiException, IOException {

        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(2l);
        vmInstanceVO.setUuid(UUID.randomUUID().toString());
        vmInstanceVO.setBackupOfferingId(2L);

        Mockito.doReturn("/status/f32092e4-3e8a-461b-8733-ed93e23fa782").when(clientMock)
                .startBackupJob(Mockito.anyString());
        Mockito.doReturn(new BackupVO()).when(backupDao).persist(Mockito.any(BackupVO.class));
        Mockito.doNothing().when(clientMock).triggerTaskStatus(Mockito.anyString());
        syncBackups_Test();
        Pair<Boolean, Backup> result = backupProvider.takeBackup(vmInstanceVO);
        assertTrue(result.first());
    }

    @Test
    public void restoreBackedUpVolume_Test() {
        try {
            backupProvider.restoreBackedUpVolume(new BackupVO(), "dummyString", "dummyString", "dummyString",
                    new Pair<String, VirtualMachine.State>("dummyString", VirtualMachine.State.Shutdown));
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("Backroll plugin does not support this feature");
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void getConfigKeys_Test() {
        assertEquals(3, backupProvider.getConfigKeys().length);
    }

    @Test
    public void getConfigComponentName_Test() {
        assertEquals(BackupService.class.getSimpleName(), backupProvider.getConfigComponentName());
    }

    @Test
    public void getBackupMetricsEmpty_Test() {
        assertEquals(backupProvider.getBackupMetrics(2L, Arrays.asList()).size(), 0);
    }

    @Test
    public void getBackupMetrics_Test() throws BackrollApiException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(1l);
        vmInstanceVO.setBackupOfferingId(1l);

        VMInstanceVO vmInstanceVO2 = new VMInstanceVO();
        vmInstanceVO2.setInstanceName("test2");
        vmInstanceVO2.setDataCenterId(2l);
        vmInstanceVO2.setBackupOfferingId(2l);

        VMInstanceVO vmInstanceVO3 = new VMInstanceVO();
        vmInstanceVO3.setInstanceName("test3");
        vmInstanceVO3.setDataCenterId(3l);
        vmInstanceVO3.setBackupOfferingId(3l);

        List<BackrollVmBackup> backupsFromBackroll = Arrays.asList(
                new BackrollVmBackup("OK", "OK", new Date()));

        BackrollBackupMetrics metrics = new BackrollBackupMetrics(2L, 3L);

        Mockito.doReturn(metrics).when(clientMock).getBackupMetrics(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(backupsFromBackroll).when(clientMock).getAllBackupsfromVirtualMachine(Mockito.anyString());
        assertEquals(
                backupProvider.getBackupMetrics(2L, Arrays.asList(vmInstanceVO, vmInstanceVO2, vmInstanceVO3)).size(),
                1);

        Mockito.verify(clientMock, times(3)).getAllBackupsfromVirtualMachine(Mockito.anyString());
        Mockito.verify(clientMock, times(3)).getBackupMetrics(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void restoreVMFromBackupTrue_Test() throws BackrollApiException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setDataCenterId(2l);

        BackupVO backupVo = new BackupVO();
        backupVo.setExternalId("abc,defgh");

        Mockito.doReturn(true).when(clientMock).restoreVMFromBackup(Mockito.anyString(), Mockito.anyString());

        Boolean result = backupProvider.restoreVMFromBackup(vmInstanceVO, backupVo);

        assertTrue(result);
    }

    @Test
    public void restoreVMFromBackupFalse_Test() throws BackrollApiException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setDataCenterId(2l);

        BackupVO backupVo = new BackupVO();
        backupVo.setExternalId("abc,defgh");

        Mockito.doReturn(false).when(clientMock).restoreVMFromBackup(Mockito.anyString(), Mockito.anyString());

        try {
            backupProvider.restoreVMFromBackup(vmInstanceVO, backupVo);
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("Failed to restore VM from Backup");
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void getDescription_Test() {
        assertEquals("Backroll Backup Plugin", backupProvider.getDescription());
    }

    @Test
    public void isValidProviderOffering_Test() {
        assertTrue(backupProvider.isValidProviderOffering(2L, "dummyString"));
    }

    @Test
    public void getName_Test() {
        assertEquals("backroll", backupProvider.getName());
    }

    @Test
    public void assignVMToBackupOffering_Test() {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        BackrollOffering backrollOf = new BackrollOffering("dummyName", UUID.randomUUID().toString());
        assertTrue(backupProvider.assignVMToBackupOffering(vmInstanceVO, backrollOf));
    }

    @Test
    public void removeVMFromBackupOffering_Test() {
        assertTrue(backupProvider.removeVMFromBackupOffering(new VMInstanceVO()));
    }

    @Test
    public void willDeleteBackupsOnOfferingRemoval_Test() {
        assertFalse(backupProvider.willDeleteBackupsOnOfferingRemoval());
    }

    @Test
    public void syncBackups_Test() throws BackrollApiException, IOException {
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
                new BackrollVmBackup("OK", "OK", new Date()));

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
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void listRestorePoints_Test() throws BackrollApiException, IOException {
        List<RestorePoint> rps = Arrays.asList(new RestorePoint("rp1", new Date(), "incremental"),
                new RestorePoint("rp2", new Date(), "incremental"),
                new RestorePoint("rp3", new Date(), "incremental"),
                new RestorePoint("rp4", new Date(), "incremental"));

        VMInstanceVO vmInstanceVO3 = new VMInstanceVO();
        vmInstanceVO3.setInstanceName("test3");
        vmInstanceVO3.setDataCenterId(3l);
        vmInstanceVO3.setBackupOfferingId(3l);

        Mockito.doReturn(rps).when(clientMock).listRestorePoints(Mockito.anyString());

        List<RestorePoint> rPoints = backupProvider.listRestorePoints(vmInstanceVO3);

        assertEquals(rPoints.size(), rps.size());

    }

    @Test
    public void createNewBackupEntryForRestorePoint_Test() throws BackrollApiException, IOException {
        RestorePoint restorePoint = new RestorePoint("restore-123", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-456");
        vm.setDataCenterId(2L);
        vm.setBackupOfferingId(3L);

        Backup.Metric metric = null;

        BackrollBackupMetrics backupMetrics = new BackrollBackupMetrics(100L, 200L);
        Mockito.doReturn(backupMetrics).when(clientMock).getBackupMetrics(vm.getUuid(), restorePoint.getId());

        BackupVO savedBackup = new BackupVO();
        Mockito.doReturn(savedBackup).when(backupDao).persist(Mockito.any(BackupVO.class));

        Backup result = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm, metric);

        assertNotNull(result);
        assertEquals(vm.getId(), result.getVmId());
        assertEquals(restorePoint.getId(), result.getExternalId());
        assertEquals("INCREMENTAL", result.getType());
        assertEquals(restorePoint.getCreated(), result.getDate());
        assertEquals(Backup.Status.BackedUp, result.getStatus());
        assertEquals(vm.getBackupOfferingId(), (Long)result.getBackupOfferingId());
        assertEquals(vm.getAccountId(), result.getAccountId());
        assertEquals(vm.getDomainId(), result.getDomainId());
        assertEquals(vm.getDataCenterId(), result.getZoneId());
        assertEquals((Long)backupMetrics.getDeduplicated(), result.getSize());
        assertEquals((Long)backupMetrics.getSize(), result.getProtectedSize());

        Mockito.verify(clientMock).getBackupMetrics(vm.getUuid(), restorePoint.getId());
        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
    }

    @Test
    public void createNewBackupEntryForRestorePoint_WithMetric_Test() throws BackrollApiException, IOException {
        RestorePoint restorePoint = new RestorePoint("restore-789", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-789");
        vm.setDataCenterId(3L);
        vm.setBackupOfferingId(4L);

        Backup.Metric metric = new Backup.Metric(150L, 250L);


        BackupVO savedBackup = new BackupVO();
        Mockito.doReturn(savedBackup).when(backupDao).persist(Mockito.any(BackupVO.class));


        Backup result = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm, metric);


        assertNotNull(result);
        assertEquals(vm.getId(), result.getVmId());
        assertEquals(restorePoint.getId(), result.getExternalId());
        assertEquals("INCREMENTAL", result.getType());
        assertEquals(restorePoint.getCreated(), result.getDate());
        assertEquals(Backup.Status.BackedUp, result.getStatus());
        assertEquals(vm.getBackupOfferingId(), (Long)result.getBackupOfferingId());
        assertEquals(vm.getAccountId(), result.getAccountId());
        assertEquals(vm.getDomainId(), result.getDomainId());
        assertEquals(vm.getDataCenterId(), result.getZoneId());
        assertEquals(metric.getBackupSize(), result.getSize());
        assertEquals(metric.getDataSize(), result.getProtectedSize());


        Mockito.verify(clientMock, Mockito.never()).getBackupMetrics(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
    }

    @Test(expected = CloudRuntimeException.class)
    public void createNewBackupEntryForRestorePoint_BackrollApiException_Test()
            throws BackrollApiException, IOException {

        RestorePoint restorePoint =new RestorePoint("restore-404", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-404");
        vm.setDataCenterId(4L);
        vm.setBackupOfferingId(5L);

        Backup.Metric metric = null;

        Mockito.doThrow(new BackrollApiException()).when(clientMock).getBackupMetrics(vm.getUuid(),
                restorePoint.getId());

        backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm, metric);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createNewBackupEntryForRestorePoint_IOException_Test() throws BackrollApiException, IOException {

        RestorePoint restorePoint = new RestorePoint("restore-500", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-500");
        vm.setDataCenterId(5L);
        vm.setBackupOfferingId(6L);

        Backup.Metric metric = null;

        Mockito.doThrow(new IOException("IO Error")).when(clientMock).getBackupMetrics(vm.getUuid(),
                restorePoint.getId());

        backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm, metric);
    }
}
