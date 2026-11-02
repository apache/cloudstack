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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.backup.dao.InternalBackupServiceJobDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class BackupValidationServiceJobControllerTest {

    @Mock
    private DataCenterDao dataCenterDaoMock;

    @Mock
    private DataCenterVO dataCenterVoMock;

    @Mock
    private HostVO hostVO;

    @Mock
    private InternalBackupServiceJobDao internalBackupServiceJobDaoMock;

    @Mock
    private InternalBackupServiceJobVO internalBackupServiceJobVoMock;

    @Mock
    private ConfigKey<Boolean> backupValidationTaskEnabledMock;

    @Spy
    @InjectMocks
    private BackupValidationServiceJobController backupValidationServiceJobControllerSpy;

    long datacenterId = 1L;

    @Test
    public void searchAndDispatchJobsTestLockFailureReturnsEarly() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(false).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);

        backupValidationServiceJobControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock).unlockFromLockTable(any());
        verify(backupValidationServiceJobControllerSpy, never()).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
    }

    @Test
    public void searchAndDispatchJobsTestTaskDisabledReturnsAfterReschedule() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(false).when(backupValidationTaskEnabledMock).value();

        backupValidationServiceJobControllerSpy.searchAndDispatchJobs();

        verify(backupValidationServiceJobControllerSpy).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestZoneWithoutBackupFrameworkIsSkipped() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(false).when(backupValidationServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupValidationServiceJobControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestEmptyWaitingJobsSkipsDispatch() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupValidationServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupValidationServiceJobControllerSpy.searchAndDispatchJobs();

        verify(backupValidationServiceJobControllerSpy, never()).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(any(), any());
        verify(backupValidationServiceJobControllerSpy, never()).submitQueuedJobsForExecution(any(), any(), any(), any(), anyLong());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestHappyPathDispatchesJobs() {
        Pair<HashMap<HostVO, Long>, Integer> executingJobsPair = new Pair<>(new HashMap<>(), 0);

        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupValidationServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupValidationServiceJobControllerSpy).filterJobsOfDomainsAndAccountsWithDisabledValidationTask(any());
        doReturn(executingJobsPair).when(backupValidationServiceJobControllerSpy).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(eq(dataCenterVoMock), any());
        doReturn(List.of(new Pair<>(hostVO, 0L))).when(backupValidationServiceJobControllerSpy).filterHostsWithTooManyJobs(any(), any());
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupValidationServiceJobControllerSpy).thinJobsToStartList(eq(dataCenterVoMock), any(), anyInt(), any());
        doNothing().when(backupValidationServiceJobControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));

        backupValidationServiceJobControllerSpy.searchAndDispatchJobs();


        verify(backupValidationServiceJobControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }
}
