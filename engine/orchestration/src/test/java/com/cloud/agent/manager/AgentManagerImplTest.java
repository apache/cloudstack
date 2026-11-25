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
package com.cloud.agent.manager;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationCancelledException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AgentManagerImplTest {

    private HostDao hostDao;
    private HostDetailsDao hostDetailsDao;
    private Listener storagePoolMonitor;
    private AgentAttache attache;
    private AgentManagerImpl mgr = Mockito.spy(new AgentManagerImpl());
    private HostVO host;
    private StartupCommand[] cmds;

    @Before
    public void setUp() throws Exception {
        host = Mockito.spy(new HostVO("some-Uuid"));
        Mockito.when(host.getId()).thenReturn(1L);
        host.setDataCenterId(1L);
        cmds = new StartupCommand[]{new StartupRoutingCommand()};
        attache = new ConnectedAgentAttache(null, 1L, "uuid", "kvm-attache", Hypervisor.HypervisorType.KVM, null, false);

        hostDao = Mockito.mock(HostDao.class);
        hostDetailsDao = Mockito.mock(HostDetailsDao.class);
        storagePoolMonitor = Mockito.mock(Listener.class);

        mgr._hostDao = hostDao;
        mgr._hostDetailsDao = hostDetailsDao;
        mgr._hostMonitors = new ArrayList<>();
        mgr._hostMonitors.add(new Pair<>(0, storagePoolMonitor));
    }

    @Test
    public void testNotifyMonitorsOfConnectionNormal() throws ConnectionException {
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(Mockito.mock(Answer.class)).when(mgr).easySend(Mockito.anyLong(), Mockito.any(ReadyCommand.class));
        Mockito.doReturn(true).when(mgr).agentStatusTransitTo(Mockito.eq(host), Mockito.eq(Status.Event.Ready), Mockito.anyLong());

        final AgentAttache agentAttache = mgr.notifyMonitorsOfConnection(attache, cmds, false);
        Assert.assertTrue(agentAttache.isReady()); // Agent is in UP state
    }

    @Test
    public void testNotifyMonitorsOfConnectionWhenStoragePoolConnectionHostFailure() throws ConnectionException {
        ConnectionException connectionException = new ConnectionException(true, "storage pool could not be connected on host");
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doThrow(connectionException).when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        try {
            mgr.notifyMonitorsOfConnection(attache, cmds, false);
            Assert.fail("Connection Exception was expected");
        } catch (ConnectionException e) {
            Assert.assertEquals(e.getMessage(), connectionException.getMessage());
        }
        Mockito.verify(mgr, Mockito.times(1)).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.eq(Status.Event.AgentDisconnected), Mockito.eq(true), Mockito.eq(true));
    }

    @Test
    public void testNotifyMonitorsOfConnectionClearsStaleNvramCapabilityOnReconnect() throws ConnectionException {
        DetailVO staleNvramCapability = Mockito.mock(DetailVO.class);
        ReadyAnswer readyAnswer = Mockito.mock(ReadyAnswer.class);
        host.setDetails(new HashMap<>(Map.of(Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString(),
                Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM, Boolean.TRUE.toString())));

        Mockito.when(staleNvramCapability.getId()).thenReturn(11L);
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(hostDao).loadDetails(host);
        Mockito.when(hostDetailsDao.findDetail(host.getId(), Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM)).thenReturn(staleNvramCapability);
        Mockito.doNothing().when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.when(readyAnswer.getResult()).thenReturn(true);
        Mockito.when(readyAnswer.getDetailsMap()).thenReturn(Map.of(Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString()));
        Mockito.doReturn(readyAnswer).when(mgr).easySend(Mockito.anyLong(), Mockito.any(ReadyCommand.class));
        Mockito.doReturn(true).when(mgr).agentStatusTransitTo(Mockito.eq(host), Mockito.eq(Status.Event.Ready), Mockito.anyLong());

        final AgentAttache agentAttache = mgr.notifyMonitorsOfConnection(attache, cmds, false);

        Assert.assertTrue(agentAttache.isReady());
        Assert.assertFalse(host.getDetails().containsKey(Host.HOST_KVM_DISK_ONLY_VM_SNAPSHOT_NVRAM));
        Mockito.verify(hostDetailsDao).remove(11L);
        Mockito.verify(hostDao).saveDetails(host);
    }

    @Test
    public void testGetTimeoutWithPositiveTimeout() {
        Commands commands = Mockito.mock(Commands.class);
        int timeout = 30;
        int result = mgr.getTimeout(commands, timeout);

        Assert.assertEquals(30, result);
    }

    @Test
    public void testGetTimeoutWithGranularTimeout() {
        Commands commands = Mockito.mock(Commands.class);
        Mockito.doReturn(50).when(mgr).getTimeoutFromGranularWaitTime(commands);

        int timeout = 0;
        int result = mgr.getTimeout(commands, timeout);

        Assert.assertEquals(50, result);
    }

    @Test
    public void testGetHostSshPortWithHostNull() {
        int hostSshPort = mgr.getHostSshPort(null);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithNonKVMHost() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithKVMHostDefaultPort() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(host.getClusterId()).thenReturn(1L);
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithKVMHostCustomPort() {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(host.getDetail(Host.HOST_SSH_PORT)).thenReturn(String.valueOf(3922));
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(3922, hostSshPort);
    }

    private AsyncJobManager cancelledJobOnThisServer(final long jobId) {
        final AsyncJobManager asyncJobManager = Mockito.mock(AsyncJobManager.class);
        mgr.asyncJobManager = asyncJobManager;
        final AsyncJobVO job = new AsyncJobVO();
        job.setId(jobId);
        Mockito.when(asyncJobManager.listCancelRequestedJobsExecutingOn(mgr._nodeId)).thenReturn(Collections.singletonList(job));
        return asyncJobManager;
    }

    @Test
    public void testCancelledJobWithoutInFlightCommandsIsFinalisedAtOnce() {
        final AsyncJobManager asyncJobManager = cancelledJobOnThisServer(42L);

        mgr.new CancelledJobsCheckTask().runInContext();

        Mockito.verify(asyncJobManager).finalizeCancelledJob(42L);
        Assert.assertTrue(mgr.isJobCancelled(42L));
    }

    @Test
    public void testCancelledJobStaysAssignedUntilItsCommandCanBeStopped() throws Exception {
        final AsyncJobManager asyncJobManager = cancelledJobOnThisServer(42L);
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn("vCenter reports task PowerOffVM_Task(task-1) as not cancelable");
        mgr.new CancelledJobsCheckTask().runInContext();
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(Mockito.anyLong());
        Mockito.verify(attache, Mockito.never()).cancelExecution(Mockito.anyLong(), Mockito.anyString());

        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        // the refusal is retried once its interval has passed
        mgr._cancelledJobs.get(42L).retryAfter = 0;
        mgr.new CancelledJobsCheckTask().runInContext();
        Mockito.verify(asyncJobManager).finalizeCancelledJob(42L);
    }

    @Test
    public void testCommandsInFlightHereForAJobCancelledElsewhereAreStopped() throws Exception {
        final AsyncJobManager asyncJobManager = Mockito.mock(AsyncJobManager.class);
        mgr.asyncJobManager = asyncJobManager;
        Mockito.when(asyncJobManager.listCancelRequestedJobsExecutingOn(mgr._nodeId)).thenReturn(Collections.emptyList());
        final AsyncJobVO cancelled = new AsyncJobVO();
        cancelled.setId(42L);
        cancelled.setStatus(JobInfo.Status.IN_PROGRESS);
        cancelled.setCancelRequested(true);
        Mockito.when(asyncJobManager.getAsyncJob(42L)).thenReturn(cancelled);
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        mgr.new CancelledJobsCheckTask().runInContext();

        Mockito.verify(attache).cancelExecution(Mockito.eq(11L), Mockito.anyString());
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(Mockito.anyLong());
    }

    @Test
    public void testEasySendHandsTheCancellationToTheCallerInsteadOfANullAnswer() throws Exception {
        final HostVO up = Mockito.mock(HostVO.class);
        Mockito.when(up.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.findById(1L)).thenReturn(up);
        final Command cmd = new ReadyCommand();
        final OperationCancelledException cancelled = new OperationCancelledException(new Command[] {cmd}, 1L, 11L, 5L, true, "Cancelled by admin via mgmt1");
        Mockito.doThrow(cancelled).when(mgr).send(1L, cmd);

        try {
            mgr.easySend(1L, cmd);
            Assert.fail("The cancellation was swallowed");
        } catch (final OperationCancelledException e) {
            Assert.assertSame(cancelled, e);
        }
    }

    @Test
    public void testRefusalCarriesWhatTheHypervisorReportedAndTheHost() throws Exception {
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.getName()).thenReturn("esxi-1");
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn("no vCenter task is running for StopCommand");
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        Assert.assertEquals("no vCenter task is running for StopCommand (host esxi-1)", mgr.checkJobCannotBeCancelled(42L));
        Assert.assertNull(mgr.checkJobCannotBeCancelled(7L));
    }

    private AsyncJobManager deliveryNotYetClaimed() {
        final AsyncJobManager asyncJobManager = Mockito.mock(AsyncJobManager.class);
        mgr.asyncJobManager = asyncJobManager;
        Mockito.when(asyncJobManager.claimJobCancelDelivery(Mockito.anyLong())).thenReturn(true);
        return asyncJobManager;
    }

    @Test
    public void testCancelWithNothingInFlightRefusesTheNextCommandOnly() {
        final AsyncJobManager asyncJobManager = deliveryNotYetClaimed();
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        Assert.assertTrue(mgr.isJobCancelled(42L));
        Assert.assertTrue(mgr.refusesCommandsOf(42L));
        Mockito.verify(asyncJobManager).claimJobCancelDelivery(42L);
        // the worker is now unwinding: its rollback commands go through
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
        Assert.assertFalse(mgr.refusesCommandsOf(7L));
        Assert.assertFalse(mgr.refusesCommandsOf(null));
    }

    @Test
    public void testRefusalIsSkippedWhenTheCancelReachedTheWorkerOnAnotherServer() {
        final AsyncJobManager asyncJobManager = deliveryNotYetClaimed();
        // the work child on the other server was already refused or stopped
        Mockito.when(asyncJobManager.claimJobCancelDelivery(42L)).thenReturn(false);
        Mockito.when(asyncJobManager.getCancellationReason(42L)).thenReturn("Cancelled by admin via mgmt2");
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        // the API thread's cleanup command passes, here and on every later call
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
        Mockito.verify(asyncJobManager, Mockito.times(1)).claimJobCancelDelivery(42L);
        Assert.assertTrue(mgr.isJobCancelled(42L));
    }

    @Test
    public void testCleanupCommandsOfAWorkerThatKnowsAreLeftAlone() throws Exception {
        final AsyncJobManager asyncJobManager = deliveryNotYetClaimed();
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        // the worker is unwinding with a new command in flight: a later pass must not stop it
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 12L))));
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));
        Mockito.verify(attache, Mockito.never()).checkExecutionIsNotCancellable(12L);
        Mockito.verify(attache, Mockito.never()).cancelExecution(Mockito.eq(12L), Mockito.anyString());

        // the same when the delivery happened on the other server
        mgr._cancelledJobs.clear();
        Mockito.when(asyncJobManager.isJobCancelDelivered(42L)).thenReturn(true);
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));
        Mockito.verify(attache, Mockito.never()).checkExecutionIsNotCancellable(12L);
    }

    @Test
    public void testAnAnsweredRequestStillInTheMapDoesNotBlockTheCancel() throws Exception {
        deliveryNotYetClaimed();
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        // the destination's PrepareForMigration has answered, the source's MigrateCommand is running
        Mockito.when(attache.isExecutionFinished(10L)).thenReturn(true);
        Mockito.when(attache.isExecutionFinished(11L)).thenReturn(false);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Arrays.asList(new Pair<>(1L, 10L), new Pair<>(1L, 11L))));

        Assert.assertNull(mgr.checkJobCannotBeCancelled(42L));
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        Mockito.verify(attache, Mockito.never()).checkExecutionIsNotCancellable(10L);
        Mockito.verify(attache, Mockito.never()).cancelExecution(Mockito.eq(10L), Mockito.anyString());
        Mockito.verify(attache).cancelExecution(Mockito.eq(11L), Mockito.anyString());
    }

    @Test
    public void testARefusedCancelIsRetriedAfterTheIntervalNotOnEveryCheck() throws Exception {
        final AsyncJobManager asyncJobManager = cancelledJobOnThisServer(42L);
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn("vCenter reports task RelocateVM_Task(task-1) as not cancelable");
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        mgr.new CancelledJobsCheckTask().runInContext();
        mgr.new CancelledJobsCheckTask().runInContext();
        mgr.new CancelledJobsCheckTask().runInContext();

        Mockito.verify(attache, Mockito.times(1)).checkExecutionIsNotCancellable(11L);
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(Mockito.anyLong());

        mgr._cancelledJobs.get(42L).retryAfter = 0;
        mgr.new CancelledJobsCheckTask().runInContext();
        Mockito.verify(attache, Mockito.times(2)).checkExecutionIsNotCancellable(11L);
    }

    @Test
    public void testTheCheckDoesNotCancelWhileTheApiThreadIsAlreadyAtIt() throws Exception {
        final AsyncJobManager asyncJobManager = cancelledJobOnThisServer(42L);
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));
        mgr._cancelledJobs.computeIfAbsent(42L, id -> new AgentManagerImpl.CancelledJob()).attempting.set(true);

        mgr.new CancelledJobsCheckTask().runInContext();

        Mockito.verify(attache, Mockito.never()).checkExecutionIsNotCancellable(Mockito.anyLong());
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(Mockito.anyLong());
    }

    @Test
    public void testWithdrawnCancelIsForgotten() {
        final AsyncJobManager asyncJobManager = deliveryNotYetClaimed();
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        mgr.jobCancelWithdrawn(42L);
        Assert.assertFalse(mgr.isJobCancelled(42L));
        Assert.assertFalse(mgr.refusesCommandsOf(42L));

        // the row says the cancel is gone although the entry is still here: the job runs on
        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));
        Mockito.when(asyncJobManager.claimJobCancelDelivery(42L)).thenReturn(false);
        Mockito.when(asyncJobManager.getCancellationReason(42L)).thenReturn(null);
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
        Assert.assertFalse(mgr.isJobCancelled(42L));
    }

    @Test
    public void testWorkJobOfACancelledParentIsStoppedUnderItsParentsKey() throws Exception {
        final AsyncJobManager asyncJobManager = Mockito.mock(AsyncJobManager.class);
        mgr.asyncJobManager = asyncJobManager;
        final AsyncJobVO work = new AsyncJobVO();
        work.setId(43L);
        work.setRelated("42");
        Mockito.when(asyncJobManager.listCancelRequestedJobsExecutingOn(mgr._nodeId)).thenReturn(Collections.singletonList(work));
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        mgr.new CancelledJobsCheckTask().runInContext();

        Mockito.verify(attache).cancelExecution(Mockito.eq(11L), Mockito.anyString());
        Mockito.verify(asyncJobManager).claimJobCancelDelivery(42L);
        Assert.assertTrue(mgr.isJobCancelled(42L));
        // the API job is finalised by the server executing it, not by the one running its work
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(Mockito.anyLong());
    }

    @Test
    public void testStoppedCommandHandsTheCancelToTheWorkerSoItsRollbackPasses() throws Exception {
        final AsyncJobManager asyncJobManager = deliveryNotYetClaimed();
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn(null);
        Mockito.when(attache.cancelExecution(Mockito.eq(11L), Mockito.anyString())).thenReturn(true);
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        Assert.assertTrue(mgr.cancelJobExecution(42L, "Job was cancelled"));

        Assert.assertTrue(mgr.isJobCancelled(42L));
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
        // the other server's workers learn through the row that the cancel has been delivered
        Mockito.verify(asyncJobManager).claimJobCancelDelivery(42L);
    }

    @Test
    public void testCommandThatCannotBeStoppedLeavesTheRefusalForTheNextOne() throws Exception {
        deliveryNotYetClaimed();
        final AgentAttache attache = Mockito.mock(AgentAttache.class);
        Mockito.doReturn(attache).when(mgr).getAttache(1L);
        Mockito.when(attache.checkExecutionIsNotCancellable(11L)).thenReturn("vCenter reports task PowerOffVM_Task(task-1) as not cancelable");
        mgr._jobToHostIdAndReqSequenceMap.put(42L, new HashSet<>(Collections.singleton(new Pair<>(1L, 11L))));

        Assert.assertFalse(mgr.cancelJobExecution(42L, "Job was cancelled"));

        Mockito.verify(attache, Mockito.never()).cancelExecution(Mockito.anyLong(), Mockito.anyString());
        Assert.assertTrue(mgr.refusesCommandsOf(42L));
        Assert.assertFalse(mgr.refusesCommandsOf(42L));
    }
}
