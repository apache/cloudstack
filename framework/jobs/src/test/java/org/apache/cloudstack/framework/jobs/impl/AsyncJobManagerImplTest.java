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

package org.apache.cloudstack.framework.jobs.impl;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.OperationCancelledException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.Volume;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.dao.SyncQueueItemDao;
import org.apache.cloudstack.jobs.JobCancellationHandler;
import org.apache.cloudstack.jobs.JobInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AsyncJobManagerImplTest {

    // db.cloud.maxActive value preloaded into DbProperties for every test in this class.
    // Chosen so the db-derived defaults (4000/2 = 2000 for api, 4000*2/3 = 2666 for worker)
    // are non-trivial and easy to reason about vs. the configured minimums under test.
    private static final int DB_CLOUD_MAX_ACTIVE = 4000;

    @Spy
    @InjectMocks
    AsyncJobManagerImpl asyncJobManager;
    @Mock
    VolumeDataFactory volFactory;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VirtualMachineManager virtualMachineManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;
    @Mock
    AsyncJobDao asyncJobDao;
    @Mock
    SyncQueueItemDao queueItemDao;
    @Mock
    SyncQueueManager queueMgr;
    @Mock
    AsyncJobMonitor jobMonitor;
    @Mock
    ManagementServerHostDao managementServerHostDao;

    private static final long NETWORK_ID = 42L;
    private static final long ACCOUNT_ID = 2L;
    // job.expire.minutes defaults to one day; these networks were created two days ago
    private static final Date OLD_NETWORK_CREATED = new Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L);

    private NetworkVO mockImplementingNetworkListed(Date created) {
        NetworkVO listed = Mockito.mock(NetworkVO.class);
        when(listed.getCreated()).thenReturn(created);
        SearchCriteria<NetworkVO> sc = Mockito.mock(SearchCriteria.class);
        when(networkDao.createSearchCriteria()).thenReturn(sc);
        when(networkDao.search(sc, null)).thenReturn(List.of(listed));
        return listed;
    }

    private NetworkVO mockNetworkReadUnderLock(Network.State state) {
        NetworkVO locked = Mockito.mock(NetworkVO.class);
        when(locked.getState()).thenReturn(state);
        when(networkDao.acquireInLockTable(NETWORK_ID, 0)).thenReturn(locked);
        return locked;
    }

    @Before
    public void preloadDbProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("db.cloud.maxActive", String.valueOf(DB_CLOUD_MAX_ACTIVE));
        // Bypass DbProperties' internal "loaded" guard so configure() reads our
        // properties instead of trying to load a real db.properties file.
        setDbPropertiesStatics(props, true);
    }

    @After
    public void tearDown() throws Exception {
        try {
            shutdownIfPresent("_apiJobExecutor");
            shutdownIfPresent("_workerJobExecutor");
        } finally {
            // Reset static state even if executor shutdown throws, so we do not
            // pollute later test classes with our stubbed ConfigKey values or
            // preloaded DbProperties.
            resetConfigValue(AsyncJobManagerImpl.ApiJobPoolSize);
            resetConfigValue(AsyncJobManagerImpl.WorkJobPoolSize);
            setDbPropertiesStatics(new Properties(), false);
        }
    }

    @Test
    public void testCleanupVolumeResource() {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Volume.toString());
        job.setInstanceId(1L);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        when(volFactory.getVolume(Mockito.anyLong())).thenReturn(volumeInfo);
        when(volumeInfo.getState()).thenReturn(Volume.State.Attaching);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(volumeInfo, Mockito.times(1)).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void testCleanupVmResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.VirtualMachine.toString());
        job.setInstanceId(1L);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(Mockito.anyLong())).thenReturn(vmInstanceVO);
        when(vmInstanceVO.getState()).thenReturn(VirtualMachine.State.Starting);
        when(vmInstanceVO.getHostId()).thenReturn(1L);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(virtualMachineManager, Mockito.times(1)).stateTransitTo(vmInstanceVO, VirtualMachine.Event.OperationFailed, 1L);
    }

    @Test
    public void testCleanupNetworkResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Network.toString());
        job.setInstanceId(1L);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkDao.findById(Mockito.anyLong())).thenReturn(networkVO);
        when(networkVO.getState()).thenReturn(Network.State.Implementing);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(networkOrchestrationService, Mockito.times(1)).stateTransitTo(networkVO,
                Network.Event.OperationFailed);
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkWhoseLockIsHeld() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        // a VM start implementing the network holds its lock, so a try without waiting fails
        when(networkDao.acquireInLockTable(NETWORK_ID, 0)).thenReturn(null);

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).releaseFromLockTable(Mockito.anyLong());
        Mockito.verifyNoInteractions(asyncJobDao);
    }

    @Test
    public void testCleanupOrphanedNetworksShutsDownOrphanedNetworkUnderLock() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        when(locked.getCreated()).thenReturn(OLD_NETWORK_CREATED);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(Collections.emptyList());

        asyncJobManager.cleanupOrphanedNetworks();

        InOrder inOrder = Mockito.inOrder(networkDao, networkOrchestrationService);
        inOrder.verify(networkDao).acquireInLockTable(NETWORK_ID, 0);
        inOrder.verify(networkOrchestrationService).stateTransitTo(locked, Network.Event.OperationFailed);
        inOrder.verify(networkDao).releaseFromLockTable(NETWORK_ID);
        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.eq(listed), Mockito.any());
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkImplementedBeforeItWasLocked() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        // the implementation finished between the search and the lock
        mockNetworkReadUnderLock(Network.State.Implemented);

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkWithPendingNetworkJob() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceId(NETWORK_ID);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(List.of(job));

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsYoungNetwork() throws NoTransitionException {
        mockImplementingNetworkListed(new Date());

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkDao, Mockito.never()).acquireInLockTable(Mockito.anyLong(), Mockito.anyInt());
        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(asyncJobDao);
    }

    @Test
    public void testCleanupOrphanedNetworksReleasesLockWhenForcingShutdown() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        when(locked.getCreated()).thenReturn(OLD_NETWORK_CREATED);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(Collections.emptyList());
        when(networkOrchestrationService.stateTransitTo(locked, Network.Event.OperationFailed)).thenThrow(new NoTransitionException("no transition"));

        asyncJobManager.cleanupOrphanedNetworks();

        InOrder inOrder = Mockito.inOrder(locked, networkDao);
        inOrder.verify(locked).setState(Network.State.Shutdown);
        inOrder.verify(networkDao).update(NETWORK_ID, locked);
        inOrder.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }

    @Test
    public void configureApiPoolUsesConfiguredValueWhenLargerThanDbDerived() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 3000);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // apiPoolSize = max(3000, 4000/2) = 3000 (configured wins)
        Assert.assertEquals(3000, apiPoolCoreSize());
    }

    @Test
    public void configureApiPoolUsesDbDerivedWhenLargerThanConfigured() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // apiPoolSize = max(50, 4000/2) = 2000 (db-derived wins)
        Assert.assertEquals(DB_CLOUD_MAX_ACTIVE / 2, apiPoolCoreSize());
    }

    @Test
    public void configureWorkerPoolUsesTwoThirdsFormula() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // workPoolSize = max(50, 4000*2/3) = 2666 (db-derived wins)
        Assert.assertEquals((DB_CLOUD_MAX_ACTIVE * 2) / 3, workerPoolCoreSize());
    }

    @Test
    public void configureWorkerPoolUsesConfiguredValueWhenLargerThanDbDerived() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 5000);
        invokeConfigureAndSwallowPostSetupNpe();
        // workPoolSize = max(5000, 4000*2/3) = 5000 (configured wins)
        Assert.assertEquals(5000, workerPoolCoreSize());
    }

    /**
     * Invoke configure() and swallow the NPE it throws when wiring SearchBuilders
     * on the un-mocked DAOs after the pool-sizing block. The executors are created
     * inside configure()'s try/catch (before the DAO wiring), so pool sizes can
     * still be inspected after the failure.
     */
    private void invokeConfigureAndSwallowPostSetupNpe() {
        try {
            asyncJobManager.configure(null, null);
        } catch (Exception ignored) {
            // expected: DAO wiring after pool-sizing NPEs because we intentionally
            // did not mock the DAOs (they are not part of what we are testing).
        }
    }

    private int apiPoolCoreSize() throws Exception {
        Object executor = readField("_apiJobExecutor");
        Assert.assertNotNull("configure() did not create _apiJobExecutor - has DAO wiring been moved before pool sizing?", executor);
        return ((ThreadPoolExecutor) executor).getCorePoolSize();
    }

    private int workerPoolCoreSize() throws Exception {
        Object executor = readField("_workerJobExecutor");
        Assert.assertNotNull("configure() did not create _workerJobExecutor - has DAO wiring been moved before pool sizing?", executor);
        return ((ThreadPoolExecutor) executor).getCorePoolSize();
    }

    private void setField(String name, Object value) throws Exception {
        Field f = AsyncJobManagerImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(asyncJobManager, value);
    }

    private Object readField(String name) throws Exception {
        Field f = AsyncJobManagerImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(asyncJobManager);
    }

    private void shutdownIfPresent(String executorFieldName) {
        try {
            Object executor = readField(executorFieldName);
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdownNow();
            }
        } catch (Exception ignored) {
            // never populated by this test - nothing to clean up
        }
    }

    private void overrideConfigValue(final ConfigKey<?> configKey, final Object value) {
        try {
            Field f = ConfigKey.class.getDeclaredField("_value");
            f.setAccessible(true);
            f.set(configKey, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void resetConfigValue(final ConfigKey<?> configKey) throws Exception {
        Field f = ConfigKey.class.getDeclaredField("_value");
        f.setAccessible(true);
        f.set(configKey, null);
    }

    private void setDbPropertiesStatics(Properties props, boolean loaded) throws Exception {
        Field propsField = DbProperties.class.getDeclaredField("properties");
        Field loadedField = DbProperties.class.getDeclaredField("loaded");
        propsField.setAccessible(true);
        loadedField.setAccessible(true);
        propsField.set(null, props);
        loadedField.set(null, loaded);
    }

    private AsyncJobVO childJob(long id, JobInfo.Status status) {
        AsyncJobVO child = new AsyncJobVO();
        child.setId(id);
        child.setStatus(status);
        child.setRelated("10");
        return child;
    }

    private AsyncJobVO workJob(long id, JobInfo.Status status) {
        AsyncJobVO work = childJob(id, status);
        work.setDispatcher("VmWorkJobDispatcher");
        return work;
    }

    private SyncQueueItemVO queueItem(long id, Long lastProcessMsid) {
        SyncQueueItemVO item = new SyncQueueItemVO();
        item.setId(id);
        item.setLastProcessMsid(lastProcessMsid);
        return item;
    }

    @Test
    public void testCancelQueuedChildJobsCompletesAndPurgesWaitingChild() {
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(workJob(11L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, null));
        Mockito.doNothing().when(asyncJobManager).completeAsyncJob(Mockito.eq(11L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.eq(ApiErrorCode.INTERNAL_ERROR.getHttpCode()), Mockito.anyString());

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        ArgumentCaptor<String> result = ArgumentCaptor.forClass(String.class);
        Mockito.verify(asyncJobManager).completeAsyncJob(Mockito.eq(11L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.eq(ApiErrorCode.INTERNAL_ERROR.getHttpCode()), result.capture());
        // the parent reads a work job's result as a serialized object
        Object cause = JobSerializerHelper.fromObjectSerializedString(result.getValue());
        Assert.assertTrue(String.valueOf(cause), cause instanceof OperationCancelledException);
        Assert.assertTrue(((OperationCancelledException) cause).getReason().contains("user request"));
        Mockito.verify(jobMonitor).unregisterByJobId(11L);
        Mockito.verify(queueMgr).purgeItem(5L);
        // the parent's cleanup after this outcome must not be refused as the job's first command
        Mockito.verify(asyncJobDao).claimCancelDelivery(10L);
    }

    @Test
    public void testCancelledResultOfAWorkJobIsAnExceptionItsParentCanRethrow() {
        String serialized = AsyncJobManagerImpl.cancelledResultFor(workJob(11L, JobInfo.Status.IN_PROGRESS), "Cancelled by admin via mgmt2");

        Object cause = JobSerializerHelper.fromObjectSerializedString(serialized);
        Assert.assertTrue(cause instanceof OperationCancelledException);
        Assert.assertEquals("Cancelled by admin via mgmt2", ((OperationCancelledException) cause).getReason());
        Assert.assertEquals("Cancelled by admin via mgmt2", ((OperationCancelledException) cause).getMessage());

        AsyncJobVO work = workJob(11L, JobInfo.Status.CANCELLED);
        work.setResult(serialized);
        Assert.assertEquals("Cancelled by admin via mgmt2", AsyncJobManagerImpl.getJobCancelReasonFromResult(work));
    }

    @Test
    public void testCancelledResultOfAnApiJobIsAnApiError() {
        AsyncJobVO api = childJob(10L, JobInfo.Status.IN_PROGRESS);
        api.setDispatcher("ApiAsyncJobDispatcher");
        Assert.assertFalse(AsyncJobManagerImpl.isWorkJob(api));
        Assert.assertFalse(AsyncJobManagerImpl.isWorkJob(childJob(10L, JobInfo.Status.IN_PROGRESS)));
        Assert.assertTrue(AsyncJobManagerImpl.isWorkJob(workJob(11L, JobInfo.Status.IN_PROGRESS)));

        String serialized = AsyncJobManagerImpl.cancelledResultFor(api, "Cancelled by admin via mgmt2");
        Assert.assertEquals(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2"), serialized);
        api.setResult(serialized);
        Assert.assertEquals("Cancelled by admin via mgmt2", AsyncJobManagerImpl.getJobCancelReasonFromResult(api));
    }

    @Test
    public void testCancelQueuedChildJobsLeavesDequeuedChildToTheInFlightPath() {
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(childJob(11L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, 1L));

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(jobMonitor, Mockito.never()).unregisterByJobId(Mockito.anyLong());
        Mockito.verify(queueMgr, Mockito.never()).purgeItem(Mockito.anyLong());
    }

    @Test
    public void testCancelQueuedChildJobsSkipsFinishedAndUnqueuedChildren() {
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Arrays.asList(childJob(11L, JobInfo.Status.SUCCEEDED), childJob(12L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(12L, SyncQueueItem.AsyncJobContentType)).thenReturn(null);

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        Mockito.verify(queueItemDao, Mockito.never()).getQueueItemIdByContentIdAndType(Mockito.eq(11L), Mockito.anyString());
        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(queueMgr, Mockito.never()).purgeItem(Mockito.anyLong());
    }

    @Test
    public void testCancellationReasonComesFromTheStoredRequest() {
        AsyncJobVO parent = childJob(10L, JobInfo.Status.IN_PROGRESS);
        parent.setCancelRequested(true);
        parent.setRelated(null);
        parent.setResult(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2"));
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(parent);
        when(asyncJobDao.findByIdIncludingRemoved(11L)).thenReturn(childJob(11L, JobInfo.Status.IN_PROGRESS));
        AsyncJobVO other = childJob(12L, JobInfo.Status.IN_PROGRESS);
        other.setRelated("");
        when(asyncJobDao.findByIdIncludingRemoved(12L)).thenReturn(other);

        Assert.assertEquals("Cancelled by admin via mgmt2", asyncJobManager.getCancellationReason(10L));
        Assert.assertEquals("Cancelled by admin via mgmt2", asyncJobManager.getCancellationReason(11L));
        Assert.assertNull(asyncJobManager.getCancellationReason(12L));
    }

    @Test
    public void testFinalisationWaitsForARunningChildAndThenCancelsTheParent() {
        AsyncJobVO parent = childJob(10L, JobInfo.Status.IN_PROGRESS);
        parent.setCancelRequested(true);
        parent.setRelated(null);
        parent.setResult(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2"));
        when(asyncJobDao.findById(10L)).thenReturn(parent);
        AsyncJobVO child = childJob(11L, JobInfo.Status.IN_PROGRESS);
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(child));
        Mockito.doNothing().when(asyncJobManager).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

        asyncJobManager.finalizeCancelledJob(10L);
        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

        child.setStatus(JobInfo.Status.CANCELLED);
        asyncJobManager.finalizeCancelledJob(10L);
        Mockito.verify(asyncJobManager).completeAsyncJob(Mockito.eq(10L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.anyInt(), Mockito.contains("Cancelled by admin via mgmt2"));
    }

    @Test
    public void testFinalisationWaitsForTheThreadStillRunningTheJob() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setCancelRequested(true);
        job.setRelated(null);
        job.setResult(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2"));
        when(asyncJobDao.findById(10L)).thenReturn(job);
        when(jobMonitor.isActiveJob(10L)).thenReturn(true, false);
        Mockito.doNothing().when(asyncJobManager).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

        // the API thread is still running: its own completion decides whether the work finished or was stopped
        asyncJobManager.finalizeCancelledJob(10L);
        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(asyncJobDao, Mockito.never()).listChildJobs(10L);

        asyncJobManager.finalizeCancelledJob(10L);
        Mockito.verify(asyncJobManager).completeAsyncJob(Mockito.eq(10L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.anyInt(), Mockito.contains("Cancelled by admin via mgmt2"));
    }

    @Test
    public void testParentFailingAfterItsQueuedChildWasCancelledEndsForThatReason() {
        AsyncJobVO parent = childJob(10L, JobInfo.Status.IN_PROGRESS);
        parent.setRelated(null);
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(parent);
        AsyncJobVO child = childJob(11L, JobInfo.Status.CANCELLED);
        child.setCancelRequested(true);
        child.setResult(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt1"));
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(child));

        Assert.assertEquals("Cancelled by admin via mgmt1", asyncJobManager.getCancellationReason(10L));
    }

    @Test
    public void testNotCancellableJobIsRefusedUnlessAllItsWorkIsStillQueued() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        job.setCmd("org.apache.cloudstack.api.command.user.vm.DeployVMCmd");
        job.setExecutingMsid(1L);
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job);

        AsyncJobVO child = childJob(11L, JobInfo.Status.IN_PROGRESS);
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(child));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        // taken by a server: its work is under way
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, 1L));
        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A deploy with work under way was cancelled");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("is not cancellable"));
        }

        // still waiting in the queue: nothing has started
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, null));
        Mockito.lenient().when(asyncJobDao.findById(10L)).thenReturn(job);
        when(asyncJobDao.requestCancel(Mockito.eq(10L), Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
        } catch (CloudRuntimeException e) {
            Assert.assertFalse("Refused although nothing had started: " + e.getMessage(), e.getMessage().contains("is not cancellable"));
        }
        Mockito.verify(asyncJobDao).requestCancel(Mockito.eq(10L), Mockito.eq(ApiErrorCode.INTERNAL_ERROR.getHttpCode()), Mockito.contains("Cancelled by admin"));
    }

    @Test
    public void testWorkTakenBetweenTheCheckAndTheFlagWithdrawsTheCancel() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        job.setCmd("org.apache.cloudstack.api.command.user.vm.DeployVMCmd");
        job.setExecutingMsid(1L);
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job);
        when(asyncJobDao.requestCancel(Mockito.eq(10L), Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
        AsyncJobVO child = childJob(11L, JobInfo.Status.IN_PROGRESS);
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(child));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        // queued at the check, taken by a server by the time the flag is written
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, null), queueItem(5L, 1L));

        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A job whose work had started was cancelled");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("started meanwhile"));
        }
        Mockito.verify(asyncJobDao).withdrawCancel(10L);
    }

    @Test
    public void testWithdrawnCancelIsAnnouncedToTheHandlers() throws Exception {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        job.setCmd("org.apache.cloudstack.api.command.user.vm.DeployVMCmd");
        job.setExecutingMsid(1L);
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job);
        when(asyncJobDao.requestCancel(Mockito.eq(10L), Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
        when(asyncJobDao.withdrawCancel(10L)).thenReturn(true);
        AsyncJobVO child = childJob(11L, JobInfo.Status.IN_PROGRESS);
        when(asyncJobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(child));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, null), queueItem(5L, 1L));
        JobCancellationHandler handler = Mockito.mock(JobCancellationHandler.class);
        setField("jobCancellationHandlers", Collections.singletonList(handler));

        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A job whose work had started was cancelled");
        } catch (CloudRuntimeException expected) {
            // the cancel was withdrawn
        }
        Mockito.verify(handler).jobCancelWithdrawn(10L);
    }

    @Test
    public void testCancelOfAJobThatCompletedBeforeTheRequestIsRefusedAndKeepsItsResult() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        AsyncJobVO completed = childJob(10L, JobInfo.Status.SUCCEEDED);
        completed.setRelated(null);
        // in progress when read, completed by the time the flag is written
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job, completed);

        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A completed job was cancelled");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("is not running. Current job status is SUCCEEDED"));
        }
        Mockito.verify(asyncJobDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(asyncJobDao, Mockito.never()).withdrawCancel(Mockito.anyLong());
    }

    @Test
    public void testCancelOfAJobThatCompletedWhileItsWorkWasBeingStoppedIsRefused() throws Exception {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        AsyncJobVO completed = childJob(10L, JobInfo.Status.SUCCEEDED);
        completed.setRelated(null);
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job, completed);
        when(asyncJobDao.requestCancel(Mockito.eq(10L), Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
        JobCancellationHandler handler = Mockito.mock(JobCancellationHandler.class);
        when(handler.cancelJobExecution(10L, "Cancelled by admin")).thenReturn(true);
        setField("jobCancellationHandlers", Collections.singletonList(handler));

        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A job that completed meanwhile was cancelled");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("completed meanwhile"));
        }
        Mockito.verify(asyncJobManager, Mockito.never()).finalizeCancelledJob(10L);
    }

    @Test
    public void testCompletionOtherThanCancelledClearsACancelThatDidNotTake() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setCancelRequested(true);
        job.setCancelDelivered(true);

        asyncJobManager.recordCompletion(job, JobInfo.Status.SUCCEEDED, 0, "the volume");

        Assert.assertEquals(JobInfo.Status.SUCCEEDED, job.getStatus());
        Assert.assertFalse(job.isCancelRequested());
        Assert.assertFalse(job.isCancelDelivered());
        Assert.assertEquals("the volume", job.getResult());
        Mockito.verify(asyncJobDao).update(10L, job);
    }

    @Test
    public void testCancelRequestedListingIncludesTheWorkJobsOfCancelledParents() {
        AsyncJobVO parent = childJob(10L, JobInfo.Status.IN_PROGRESS);
        parent.setRelated(null);
        parent.setCancelRequested(true);
        when(asyncJobDao.getCancelRequestedJobs(1L)).thenReturn(Collections.emptyList());
        AsyncJobVO ofCancelled = workJob(11L, JobInfo.Status.IN_PROGRESS);
        AsyncJobVO ofRunning = workJob(12L, JobInfo.Status.IN_PROGRESS);
        ofRunning.setRelated("20");
        AsyncJobVO orphan = workJob(13L, JobInfo.Status.IN_PROGRESS);
        orphan.setRelated("");
        when(asyncJobDao.getRunningWorkJobs(1L)).thenReturn(Arrays.asList(ofCancelled, ofRunning, orphan));
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(parent);
        when(asyncJobDao.findByIdIncludingRemoved(20L)).thenReturn(childJob(20L, JobInfo.Status.IN_PROGRESS));

        List<AsyncJobVO> listed = asyncJobManager.listCancelRequestedJobsExecutingOn(1L);

        Assert.assertEquals(1, listed.size());
        Assert.assertEquals(11L, listed.get(0).getId());
    }

    @Test
    public void testSecondCancelOfAJobBeingCancelledIsRefused() {
        AsyncJobVO job = childJob(10L, JobInfo.Status.IN_PROGRESS);
        job.setRelated(null);
        job.setUuid("job-uuid");
        job.setCancelRequested(true);
        job.setResult(AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2"));
        when(asyncJobDao.findByIdIncludingRemoved(10L)).thenReturn(job);

        try {
            asyncJobManager.cancelAsyncJob(10L, "Cancelled by admin");
            Assert.fail("A job already being cancelled was cancelled again");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("already being cancelled (Cancelled by admin via mgmt2)"));
        }
        Mockito.verify(asyncJobDao, Mockito.never()).requestCancel(Mockito.anyLong(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    public void testCancelledResultReadsLikeAFailure() {
        String serialized = AsyncJobManagerImpl.cancelledResult("Cancelled by admin via mgmt2");

        Assert.assertTrue(serialized.startsWith(ExceptionResponse.class.getName() + "/"));
        ExceptionResponse response = (ExceptionResponse) JobSerializerHelper.fromSerializedString(serialized);
        Assert.assertEquals("Cancelled by admin via mgmt2", response.getErrorText());
        Assert.assertEquals(Integer.valueOf(ApiErrorCode.INTERNAL_ERROR.getHttpCode()), response.getErrorCode());
    }
}
