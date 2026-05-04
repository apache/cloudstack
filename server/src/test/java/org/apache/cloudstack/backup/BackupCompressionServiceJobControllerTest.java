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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.InternalBackupServiceJobDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class BackupCompressionServiceJobControllerTest {

    @Mock
    private DataCenterDao dataCenterDaoMock;

    @Mock
    private DataCenterVO dataCenterVoMock;

    @Mock
    private ClusterDao clusterDaoMock;

    @Mock
    private ClusterVO clusterVoMock;

    @Mock
    private HostDao hostDaoMock;

    @Mock
    private HostVO hostVO;

    @Mock
    private InternalBackupServiceJobDao internalBackupServiceJobDaoMock;

    @Mock
    private InternalBackupServiceJobVO internalBackupServiceJobVoMock;

    @Mock
    private BackupDao backupDaoMock;

    @Mock
    private BackupVO backupVoMock;

    @Mock
    private VMInstanceDao vmInstanceDaoMock;

    @Mock
    private VMInstanceVO vmInstanceVoMock;

    @Mock
    private ConfigKey<Integer> maxConcurrentJobsConfigKey;

    @Mock
    private ConfigKey<Boolean> backupCompressionTaskEnabledMock;

    @Spy
    @InjectMocks
    private BackupCompressionServiceJobController backupCompressionServiceJobControllerSpy;

    long datacenterId = 1L;
    long clusterId = 2L;
    long hostId = 3L;
    long jobId = 32L;
    long backupId = 46L;
    long instanceId = 100L;

    @Test
    public void rescheduleLostJobsTestNoHostsInCluster() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listAllZones();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(List.of(clusterVoMock)).when(clusterDaoMock).listByDcHyType(datacenterId, Hypervisor.HypervisorType.KVM.toString());
        doReturn(clusterId).when(clusterVoMock).getId();
        doReturn(List.of()).when(hostDaoMock).findRoutingByClusterId(clusterId);

        backupCompressionServiceJobControllerSpy.rescheduleLostJobs();

        verify(backupCompressionServiceJobControllerSpy, never()).getLostJobs(any(), any(), any());
    }

    @Test
    public void rescheduleLostJobsTestNoLostJobs() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listAllZones();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(List.of(clusterVoMock)).when(clusterDaoMock).listByDcHyType(datacenterId, Hypervisor.HypervisorType.KVM.toString());
        doReturn(clusterId).when(clusterVoMock).getId();
        doReturn(List.of(hostVO)).when(hostDaoMock).findRoutingByClusterId(clusterId);
        doReturn(List.of()).when(backupCompressionServiceJobControllerSpy).getLostJobs(any(), any(), any());

        backupCompressionServiceJobControllerSpy.rescheduleLostJobs();

        verify(backupCompressionServiceJobControllerSpy, never()).processJobResult(any(), anyBoolean());
    }

    @Test
    public void rescheduleLostJobsTestProcessJob() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listAllZones();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(List.of(clusterVoMock)).when(clusterDaoMock).listByDcHyType(datacenterId, Hypervisor.HypervisorType.KVM.toString());
        doReturn(clusterId).when(clusterVoMock).getId();
        doReturn(List.of(hostVO)).when(hostDaoMock).findRoutingByClusterId(clusterId);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupCompressionServiceJobControllerSpy).getLostJobs(eq(clusterVoMock), any(), any());
        doNothing().when(backupCompressionServiceJobControllerSpy).processJobResult(any(), anyBoolean());

        backupCompressionServiceJobControllerSpy.rescheduleLostJobs();

        verify(backupCompressionServiceJobControllerSpy).processJobResult(any(), anyBoolean());
    }

    @Test
    public void processJobResultTestSuccessfulJob() {
        backupCompressionServiceJobControllerSpy.processJobResult(internalBackupServiceJobVoMock, true);

        verify(internalBackupServiceJobDaoMock).update(internalBackupServiceJobVoMock);
        verify(backupDaoMock, never()).findByIdIncludingRemoved(any());
    }

    @Test
    public void processJobResultTestFailedJobAndRemovedBackup() {
        doReturn(backupId).when(internalBackupServiceJobVoMock).getBackupId();
        doReturn(backupVoMock).when(backupDaoMock).findByIdIncludingRemoved(backupId);
        doReturn(new Date()).when(backupVoMock).getRemoved();

        backupCompressionServiceJobControllerSpy.processJobResult(internalBackupServiceJobVoMock, false);

        verify(backupDaoMock).findByIdIncludingRemoved(any());
        verify(internalBackupServiceJobVoMock).setRemoved(any());
        verify(internalBackupServiceJobDaoMock).update(internalBackupServiceJobVoMock);
    }

    @Test
    public void processJobResultTestFailedJobAndReachedMaxAttempts() {
        doReturn(backupId).when(internalBackupServiceJobVoMock).getBackupId();
        doReturn(backupVoMock).when(backupDaoMock).findByIdIncludingRemoved(backupId);
        doReturn(1).when(backupCompressionServiceJobControllerSpy).getMaxAttempts(internalBackupServiceJobVoMock);
        doReturn(1).when(internalBackupServiceJobVoMock).getAttempts();

        backupCompressionServiceJobControllerSpy.processJobResult(internalBackupServiceJobVoMock, false);

        verify(backupDaoMock).findByIdIncludingRemoved(any());
        verify(backupCompressionServiceJobControllerSpy).getMaxAttempts(internalBackupServiceJobVoMock);
        verify(internalBackupServiceJobVoMock).setRemoved(any());
        verify(internalBackupServiceJobDaoMock).update(internalBackupServiceJobVoMock);
    }

    @Test
    public void processJobResultTestFailedJob() {
        doReturn(backupId).when(internalBackupServiceJobVoMock).getBackupId();
        doReturn(backupVoMock).when(backupDaoMock).findByIdIncludingRemoved(backupId);
        doReturn(2).when(backupCompressionServiceJobControllerSpy).getMaxAttempts(internalBackupServiceJobVoMock);
        doReturn(1).when(internalBackupServiceJobVoMock).getAttempts();
        doReturn(60).when(backupCompressionServiceJobControllerSpy).getRetryInterval(internalBackupServiceJobVoMock);

        backupCompressionServiceJobControllerSpy.processJobResult(internalBackupServiceJobVoMock, false);

        verify(backupDaoMock).findByIdIncludingRemoved(any());
        verify(backupCompressionServiceJobControllerSpy).getMaxAttempts(internalBackupServiceJobVoMock);
        verify(internalBackupServiceJobVoMock, never()).setRemoved(any());
        verify(internalBackupServiceJobVoMock).setScheduledStartTime(any());
        verify(internalBackupServiceJobVoMock).setStartTime(null);
        verify(internalBackupServiceJobVoMock).setHostId(null);
        verify(internalBackupServiceJobDaoMock).update(internalBackupServiceJobVoMock);
    }

    @Test
    public void getHostToNumberOfExecutingJobsAndTotalExecutingJobsTestNoExecutingJobsAndNoEligibleHosts() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(List.of(hostVO)).when(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        doReturn(Status.Down).when(hostVO).getStatus();
        doReturn(List.of()).when(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);

        Pair<HashMap<HostVO, Long>, Integer> result =
                backupCompressionServiceJobControllerSpy.getHostToNumberOfExecutingJobsAndTotalExecutingJobs(dataCenterVoMock);

        assertTrue(result.first().isEmpty());
        assertEquals(Integer.valueOf(0), result.second());
        verify(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        verify(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);
    }

    @Test
    public void getHostToNumberOfExecutingJobsAndTotalExecutingJobsTestOneEligibleHostAndOneExecutingJob() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(hostId).when(hostVO).getId();
        doReturn(Status.Up).when(hostVO).getStatus();
        doReturn(ResourceState.Enabled).when(hostVO).getResourceState();
        doReturn(List.of(hostVO)).when(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);
        doReturn(hostId).when(internalBackupServiceJobVoMock).getHostId();

        Pair<HashMap<HostVO, Long>, Integer> result =
                backupCompressionServiceJobControllerSpy.getHostToNumberOfExecutingJobsAndTotalExecutingJobs(dataCenterVoMock);

        assertEquals(Integer.valueOf(1), result.second());
        assertEquals(1, result.first().size());
        assertTrue(result.first().containsKey(hostVO));
        assertEquals(Long.valueOf(1L), result.first().get(hostVO));
    }

    @Test
    public void getHostToNumberOfExecutingJobsAndTotalExecutingJobsTestEligibleHostButExecutingJobOnDisabledHostIsIgnored() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(hostId).when(hostVO).getId();
        doReturn(Status.Down).when(hostVO).getStatus();
        doReturn(List.of(hostVO)).when(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);
        doReturn(hostId).when(internalBackupServiceJobVoMock).getHostId();

        Pair<HashMap<HostVO, Long>, Integer> result =
                backupCompressionServiceJobControllerSpy.getHostToNumberOfExecutingJobsAndTotalExecutingJobs(dataCenterVoMock);

        assertEquals(Integer.valueOf(1), result.second());
        assertTrue(result.first().isEmpty());
        assertFalse(result.first().containsKey(hostVO));
    }

    @Test
    public void getHostToNumberOfExecutingJobsAndTotalExecutingJobsTestJobExecutingInUnknownHost() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(List.of()).when(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);

        Pair<HashMap<HostVO, Long>, Integer> result =
                backupCompressionServiceJobControllerSpy.getHostToNumberOfExecutingJobsAndTotalExecutingJobs(dataCenterVoMock);

        assertTrue(result.first().isEmpty());
        assertEquals(Integer.valueOf(1), result.second());
        verify(hostDaoMock).listAllRoutingHostsByZoneAndHypervisorType(datacenterId, Hypervisor.HypervisorType.KVM);
        verify(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(datacenterId);
    }

    @Test
    public void thinJobsToStartListTestUnlimitedConcurrentJobs() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(0).when(maxConcurrentJobsConfigKey).valueIn(datacenterId);
        ArrayList<InternalBackupServiceJobVO> originalJobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            originalJobs.add(Mockito.mock(InternalBackupServiceJobVO.class));
        }

        List<InternalBackupServiceJobVO> result = backupCompressionServiceJobControllerSpy.thinJobsToStartList(dataCenterVoMock, new ArrayList<>(originalJobs), 0,
                maxConcurrentJobsConfigKey);

        assertEquals(originalJobs, result);
    }

    @Test
    public void thinJobsToStartListTestTotalExecutingJobsBiggerThanMax() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(3).when(maxConcurrentJobsConfigKey).valueIn(datacenterId);
        ArrayList<InternalBackupServiceJobVO> originalJobs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            originalJobs.add(Mockito.mock(InternalBackupServiceJobVO.class));
        }

        List<InternalBackupServiceJobVO> result = backupCompressionServiceJobControllerSpy.thinJobsToStartList(dataCenterVoMock, new ArrayList<>(originalJobs), 3,
                maxConcurrentJobsConfigKey);

        assertEquals(List.of(), result);
    }


    @Test
    public void thinJobsToStartListTestTotalExecutingJobsLowerThanMax() {
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(6).when(maxConcurrentJobsConfigKey).valueIn(datacenterId);
        ArrayList<InternalBackupServiceJobVO> originalJobs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            originalJobs.add(Mockito.mock(InternalBackupServiceJobVO.class));
        }

        List<InternalBackupServiceJobVO> result = backupCompressionServiceJobControllerSpy.thinJobsToStartList(dataCenterVoMock, new ArrayList<>(originalJobs), 3,
                maxConcurrentJobsConfigKey);

        assertEquals(originalJobs.subList(0, 3), result);
    }

    @Test
    public void filterHostsWithTooManyJobsTestUnlimitedCompressionPerHost() {
        doReturn(0).when(backupCompressionServiceJobControllerSpy).getMaxConcurrentCompressionsPerHost(maxConcurrentJobsConfigKey, hostVO);

        HashMap<HostVO, Long> hostToNumberOfExecutingJobs = new HashMap<>();
        hostToNumberOfExecutingJobs.put(hostVO, 10L);

        List<Pair<HostVO, Long>> result = backupCompressionServiceJobControllerSpy.filterHostsWithTooManyJobs(hostToNumberOfExecutingJobs, maxConcurrentJobsConfigKey);

        assertEquals(List.of(new Pair<>(hostVO, 10L)), result);
    }

    @Test
    public void filterHostsWithTooManyJobsTestLimitedCompressionPerHost() {
        doReturn(4).when(backupCompressionServiceJobControllerSpy).getMaxConcurrentCompressionsPerHost(any(), any());


        HashMap<HostVO, Long> hostToNumberOfExecutingJobs = new HashMap<>();
        hostToNumberOfExecutingJobs.put(hostVO, 10L);
        HostVO hostVO2 = Mockito.mock(HostVO.class);
        hostToNumberOfExecutingJobs.put(hostVO2, 2L);

        List<Pair<HostVO, Long>> result = backupCompressionServiceJobControllerSpy.filterHostsWithTooManyJobs(hostToNumberOfExecutingJobs, maxConcurrentJobsConfigKey);

        assertEquals(List.of(new Pair<>(hostVO2, 2L)), result);
    }

    @Test
    public void submitQueuedJobsForExecutionTestNoAvailableHostsReturnsEarly() {
        List<InternalBackupServiceJobVO> jobsToExecute = List.of(internalBackupServiceJobVoMock);

        backupCompressionServiceJobControllerSpy.submitQueuedJobsForExecution(jobsToExecute, List.of(), new HashSet<>(), maxConcurrentJobsConfigKey, datacenterId);

        verify(internalBackupServiceJobVoMock, never()).setHostId(any());
        verify(internalBackupServiceJobVoMock, never()).setStartTime(any());
        verify(internalBackupServiceJobDaoMock, never()).update(any());
    }

    @Test
    public void submitQueuedJobsForExecutionTestBusyInstanceIsSkipped() {
        doReturn(instanceId).when(internalBackupServiceJobVoMock).getInstanceId();
        doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(instanceId);

        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = List.of(new Pair<>(hostVO, 0L));
        Set<Long> busyInstances = Set.of(instanceId);

        backupCompressionServiceJobControllerSpy.submitQueuedJobsForExecution(List.of(internalBackupServiceJobVoMock), hostAndNumberOfJobsPairList, busyInstances,
                maxConcurrentJobsConfigKey, datacenterId);

        verify(internalBackupServiceJobVoMock, never()).setHostId(any());
        verify(internalBackupServiceJobVoMock, never()).setStartTime(any());
        verify(internalBackupServiceJobDaoMock, never()).update(any());
        verify(backupCompressionServiceJobControllerSpy, never()).submitQueuedJob(any(), anyLong(), any());
    }

    @Test
    public void submitQueuedJobsForExecutionTestSchedulesJobAndRequeuesHostWhenBelowLimit() {
        doReturn(instanceId).when(internalBackupServiceJobVoMock).getInstanceId();
        doReturn(jobId).when(internalBackupServiceJobVoMock).getId();
        doReturn(backupId).when(internalBackupServiceJobVoMock).getBackupId();
        doReturn(hostId).when(hostVO).getId();
        doReturn(5).when(backupCompressionServiceJobControllerSpy).getMaxConcurrentCompressionsPerHost(maxConcurrentJobsConfigKey, hostVO);
        doNothing().when(backupCompressionServiceJobControllerSpy).submitQueuedJob(any(), eq(datacenterId), any());

        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = new java.util.ArrayList<>();
        hostAndNumberOfJobsPairList.add(new Pair<>(hostVO, 0L));
        Set<Long> busyInstances = new HashSet<>();

        backupCompressionServiceJobControllerSpy.submitQueuedJobsForExecution(List.of(internalBackupServiceJobVoMock), hostAndNumberOfJobsPairList, busyInstances,
                maxConcurrentJobsConfigKey, datacenterId);

        verify(internalBackupServiceJobVoMock).setHostId(hostId);
        verify(internalBackupServiceJobVoMock).setStartTime(any());
        verify(internalBackupServiceJobDaoMock).update(internalBackupServiceJobVoMock);
        verify(backupCompressionServiceJobControllerSpy).submitQueuedJob(eq(internalBackupServiceJobVoMock), eq(datacenterId), any());
        assertEquals(1, hostAndNumberOfJobsPairList.size());
        assertSame(hostVO, hostAndNumberOfJobsPairList.get(0).first());
        assertEquals(Long.valueOf(1L), hostAndNumberOfJobsPairList.get(0).second());
        assertTrue(busyInstances.contains(instanceId));
    }

    @Test
    public void submitQueuedJobsForExecutionTestDoesNotRequeueHostWhenLimitReached() {
        doReturn(instanceId).when(internalBackupServiceJobVoMock).getInstanceId();
        doReturn(jobId).when(internalBackupServiceJobVoMock).getId();
        doReturn(backupId).when(internalBackupServiceJobVoMock).getBackupId();
        doReturn(hostId).when(hostVO).getId();
        doReturn(1).when(backupCompressionServiceJobControllerSpy).getMaxConcurrentCompressionsPerHost(maxConcurrentJobsConfigKey, hostVO);
        doNothing().when(backupCompressionServiceJobControllerSpy).submitQueuedJob(any(), eq(datacenterId), any());

        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = new java.util.ArrayList<>();
        hostAndNumberOfJobsPairList.add(new Pair<>(hostVO, 0L));
        Set<Long> busyInstances = new HashSet<>();

        backupCompressionServiceJobControllerSpy.submitQueuedJobsForExecution(List.of(internalBackupServiceJobVoMock), hostAndNumberOfJobsPairList, busyInstances,
                maxConcurrentJobsConfigKey, datacenterId);

        assertTrue(hostAndNumberOfJobsPairList.isEmpty());
        assertTrue(busyInstances.contains(instanceId));
    }

    @Test
    public void searchAndDispatchJobsTestLockFailureReturnsEarly() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(false).when(internalBackupServiceJobDaoMock).lockInLockTable("compression_lock", 300);

        backupCompressionServiceJobControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock).unlockFromLockTable(any());
        verify(backupCompressionServiceJobControllerSpy, never()).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
    }

    @Test
    public void searchAndDispatchJobsTestTaskDisabledReturnsAfterReschedule() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("compression_lock", 300);
        doNothing().when(backupCompressionServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(false).when(backupCompressionTaskEnabledMock).value();

        backupCompressionServiceJobControllerSpy.searchAndDispatchJobs();

        verify(backupCompressionServiceJobControllerSpy).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("compression_lock");
    }

    @Test
    public void searchAndDispatchJobsTestZoneWithoutBackupFrameworkIsSkipped() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("compression_lock", 300);
        doNothing().when(backupCompressionServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupCompressionTaskEnabledMock).value();
        doReturn(false).when(backupCompressionServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupCompressionServiceJobControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("compression_lock");
    }

    @Test
    public void searchAndDispatchJobsTestEmptyWaitingJobsSkipsDispatch() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("compression_lock", 300);
        doNothing().when(backupCompressionServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupCompressionTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupCompressionServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupCompressionServiceJobControllerSpy.searchAndDispatchJobs();

        verify(backupCompressionServiceJobControllerSpy, never()).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(any(), any());
        verify(backupCompressionServiceJobControllerSpy, never()).submitQueuedJobsForExecution(any(), any(), any(), any(), anyLong());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("compression_lock");
    }

    @Test
    public void searchAndDispatchJobsTestHappyPathDispatchesJobs() {
        Pair<HashMap<HostVO, Long>, Integer> executingJobsPair = new Pair<>(new HashMap<>(), 0);

        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("compression_lock", 300);
        doNothing().when(backupCompressionServiceJobControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupCompressionTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupCompressionServiceJobControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(internalBackupServiceJobDaoMock).listWaitingJobsAndScheduledToBeforeNow(eq(datacenterId),
                eq(InternalBackupServiceJobType.StartCompression), eq(InternalBackupServiceJobType.FinalizeCompression));
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupCompressionServiceJobControllerSpy).filterJobsOfDomainsAndAccountsWithDisabledCompressionTask(any());
        doReturn(executingJobsPair).when(backupCompressionServiceJobControllerSpy).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(eq(dataCenterVoMock), any());
        doReturn(List.of(new Pair<>(hostVO, 0L))).when(backupCompressionServiceJobControllerSpy).filterHostsWithTooManyJobs(any(), any());
        doReturn(new HashSet<Long>()).when(backupCompressionServiceJobControllerSpy).submitFinalizeJobsForExecution(any(), any(), eq(datacenterId));
        doReturn(List.of()).when(internalBackupServiceJobDaoMock).listExecutingJobsByZoneIdAndJobType(eq(datacenterId), eq(InternalBackupServiceJobType.BackupValidation));
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupCompressionServiceJobControllerSpy).thinJobsToStartList(eq(dataCenterVoMock), any(), anyInt(), any());
        doNothing().when(backupCompressionServiceJobControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));

        backupCompressionServiceJobControllerSpy.searchAndDispatchJobs();


        verify(backupCompressionServiceJobControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("compression_lock");
    }
}
