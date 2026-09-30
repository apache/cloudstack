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
package com.cloud.storage.snapshot;

import com.cloud.api.ApiDispatcher;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotSchedulerImplTest {

    @Spy
    @InjectMocks
    SnapshotSchedulerImpl snapshotSchedulerImplSpy = new SnapshotSchedulerImpl();

    @Mock
    SnapshotPolicyDao snapshotPolicyDaoMock;

    @Mock
    SnapshotPolicyVO snapshotPolicyVoMock;

    @Mock
    SnapshotScheduleDao snapshotScheduleDaoMock;

    @Mock
    AccountDao accountDaoMock;

    @Mock
    VolumeDao volumeDaoMock;

    @Mock
    VolumeVO volumeVoMock;

    @Mock
    AccountVO accountVoMock;

    @Mock
    private SnapshotScheduleVO snapshotScheduleVoMock;

    @Mock
    private AsyncJobDao asyncJobDaoMock;

    @Mock
    private AsyncJobVO asyncJobVoMock;

    @Mock
    private EventDao eventDaoMock;

    @Mock
    private VMInstanceDao vmInstanceDaoMock;

    @Mock
    private VMInstanceVO vmInstanceVoMock;

    @Mock
    private SnapshotVO snapshotVoMock;

    @Mock
    private ApiDispatcher apiDispatcherMock;

    @Mock
    private AsyncJobManager asyncJobManagerMock;

    @Mock
    private TaggedResourceService taggedResourceServiceMock;

    @Mock
    private AsyncJobDispatcher asyncJobDispatcherMock;

    @Mock
    private SnapshotDao snapshotDaoMock;

    @Test
    public void scheduleNextSnapshotJobTestParameterIsNullReturnNull() {
        SnapshotScheduleVO snapshotScheduleVO = null;

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);

        Assert.assertNull(result);
    }

    @Test
    public void scheduleNextSnapshotJobTestIsManualPolicyIdReturnNull() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(Snapshot.MANUAL_POLICY_ID);

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);

        Assert.assertNull(result);
    }

    @Test
    public void scheduleNextSnapshotJobTestPolicyIsNotNullDoNotCallExpunge() {
        Date expected = new Date();
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(1l);

        doReturn(snapshotPolicyVoMock).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotPolicyVO.class));

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        verify(snapshotScheduleDaoMock, never()).expunge(Mockito.anyLong());
    }

    @Test
    public void scheduleNextSnapshotJobTestPolicyIsNullCallExpunge() {
        Date expected = new Date();
        SnapshotPolicyVO snapshotPolicyVO = null;
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(1l);

        doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        doReturn(true).when(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
        doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(snapshotPolicyVO);

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        verify(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountIsNullReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(null).when(accountDaoMock).findById(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsDisabledReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        doReturn(Account.State.DISABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsNotNullNorDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        doReturn(Account.State.ENABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsRemovedReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(new Date()).when(volumeVoMock).getRemoved();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsNotAttachedToStoragePoolReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(null).when(volumeVoMock).getPoolId();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestAccountIsRemovedOrDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(1l).when(volumeVoMock).getPoolId();
        doReturn(true).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsRemovedCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        doReturn(1l).when(volumeVoMock).getPoolId();
        doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        verify(snapshotScheduleDaoMock).remove(Mockito.anyLong());
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsNotRemovedDoNotCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        SnapshotPolicyVO snapshotPolicyVO = new SnapshotPolicyVO();

        doReturn(1l).when(volumeVoMock).getPoolId();
        doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        verify(snapshotScheduleDaoMock, never()).remove(Mockito.anyLong());
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobIsNullThenScheduleNextSnapshot() {
        doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        doReturn(null).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobSucceededThenScheduleNextSnapshot() {
        doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        doReturn(JobInfo.Status.SUCCEEDED).when(asyncJobVoMock).getStatus();
        doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobFailedThenScheduleNextSnapshot() {
        doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        doReturn(JobInfo.Status.FAILED).when(asyncJobVoMock).getStatus();
        doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobInProgressThenDoNothing() {
        doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        doReturn(JobInfo.Status.IN_PROGRESS).when(asyncJobVoMock).getStatus();

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        verify(snapshotSchedulerImplSpy, never()).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    // --- countConsecutiveFailedAttempts (#13454) ---

    @Test
    public void countConsecutiveFailedAttemptsTestLimitZeroReturnsZeroWithoutQuerying() {
        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 0);

        Assert.assertEquals(0, result);
        verify(eventDaoMock, never()).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void countConsecutiveFailedAttemptsTestStopsAtFirstNonErrorEvent() {
        EventVO error1 = mock(EventVO.class);
        doReturn(EventVO.LEVEL_ERROR).when(error1).getLevel();
        EventVO error2 = mock(EventVO.class);
        doReturn(EventVO.LEVEL_ERROR).when(error2).getLevel();
        EventVO success = mock(EventVO.class);
        doReturn(EventVO.LEVEL_INFO).when(success).getLevel();

        doReturn(List.of(error1, error2, success)).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());

        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 3);

        Assert.assertEquals(2, result);
    }

    @Test
    public void countConsecutiveFailedAttemptsTestNoEventsReturnsZero() {
        doReturn(Collections.emptyList()).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());

        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 3);

        Assert.assertEquals(0, result);
    }

    // --- getScopedConfigValue (#13454) ---

    @Test
    public void getScopedConfigValueTestFallsBackToGlobalDefaultWhenNoScopeOverrideConfigured() {
        doReturn(1L).when(volumeVoMock).getAccountId();
        doReturn(1L).when(volumeVoMock).getDataCenterId();
        doReturn(1L).when(accountVoMock).getDomainId();

        Integer result = snapshotSchedulerImplSpy.getScopedConfigValue(SnapshotManager.SnapshotRecurringMaxFailures, volumeVoMock, accountVoMock);

        Assert.assertEquals(Integer.valueOf(3), result);
    }

    // --- handleFailedSnapshotDispatch (#13454) ---

    private void stubVolumeAndAccount() {
        doReturn(1L).when(volumeVoMock).getId();
        doReturn(1L).when(volumeVoMock).getAccountId();
        doReturn(1L).when(volumeVoMock).getDataCenterId();
        doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
    }

    private void stubConsecutiveFailureEvents(int count) {
        List<EventVO> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EventVO event = mock(EventVO.class);
            doReturn(EventVO.LEVEL_ERROR).when(event).getLevel();
            events.add(event);
        }
        doReturn(events).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    // Must use a distinct event type from EVENT_SNAPSHOT_CREATE, otherwise it becomes the "latest event" scanned
    // by countConsecutiveFailedAttempts and silently resets the consecutive-failure count on the next attempt.
    private void verifyFailureThresholdEventsRaised(MockedStatic<ActionEventUtils> actionEventUtilsMocked) {
        actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_ERROR), Mockito.eq(EventTypes.EVENT_SNAPSHOT_CREATE), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()));
        actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_WARN), Mockito.eq(EventTypes.EVENT_SNAPSHOT_RECURRING_FAILURE_LIMIT_REACHED), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()));
    }

    @Test
    public void handleFailedSnapshotDispatchTestUnderMaxReschedulesWithRetryIntervalOnly() {
        stubVolumeAndAccount();
        doReturn(Collections.emptyList()).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
        ReflectionTestUtils.setField(snapshotSchedulerImplSpy, "_currentTimestamp", new Date());

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.handleFailedSnapshotDispatch(snapshotScheduleVoMock, volumeVoMock, snapshotScheduleVoMock, 5L, new Exception("boom"));

            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_ERROR), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.eq(5L)));
            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()), never());
        }

        verify(snapshotScheduleVoMock).setScheduledTimestamp(Mockito.any(Date.class));
        verify(snapshotScheduleDaoMock).update(Mockito.anyLong(), Mockito.eq(snapshotScheduleVoMock));
    }

    @Test
    public void handleFailedSnapshotDispatchTestAtMaxGivesUpAndNotifies() {
        stubVolumeAndAccount();
        stubConsecutiveFailureEvents(2);

        doReturn(1L).when(snapshotScheduleVoMock).getPolicyId();
        doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.handleFailedSnapshotDispatch(snapshotScheduleVoMock, volumeVoMock, snapshotScheduleVoMock, null, new Exception("boom"));

            verifyFailureThresholdEventsRaised(actionEventUtilsMocked);
        }

        verify(snapshotScheduleDaoMock).update(Mockito.anyLong(), Mockito.eq(snapshotScheduleVoMock));
    }

    @Test
    public void recordSnapshotAttemptOutcomeTestAtMaxNotifiesWithDistinctEventType() {
        doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        stubVolumeAndAccount();
        stubConsecutiveFailureEvents(2);

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.recordSnapshotAttemptOutcome(snapshotScheduleVoMock, false, "boom");

            verifyFailureThresholdEventsRaised(actionEventUtilsMocked);
        }
    }

    @Test
    public void recordSnapshotAttemptOutcomeTestSucceededLogsInfoEventAndReturns() {
        doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.recordSnapshotAttemptOutcome(snapshotScheduleVoMock, true, null);

            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_INFO), Mockito.eq(EventTypes.EVENT_SNAPSHOT_CREATE),
                    Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()));
        }

        verify(accountDaoMock, never()).findById(Mockito.anyLong());
        verify(eventDaoMock, never()).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    // --- dispatchSnapshotCreateJob (#13454) ---

    @Test
    public void dispatchSnapshotCreateJobTestBuildsAsyncJobAndReturnsScheduledEventId() throws Exception {
        doReturn(1L).when(snapshotScheduleVoMock).getId();
        doReturn(1L).when(snapshotScheduleVoMock).getPolicyId();
        doReturn(2L).when(snapshotScheduleVoMock).getVolumeId();
        doReturn(new Date()).when(snapshotScheduleVoMock).getScheduledTimestamp();

        doReturn(1L).when(volumeVoMock).getAccountId();
        doReturn("volume-uuid").when(volumeVoMock).getUuid();

        doReturn(Collections.emptyList()).when(taggedResourceServiceMock).listByResourceTypeAndId(Mockito.any(), Mockito.anyLong());
        doReturn(7L).when(asyncJobManagerMock).submitAsyncJob(Mockito.any(AsyncJobVO.class));
        doReturn("SnapshotDispatcher").when(asyncJobDispatcherMock).getName();
        snapshotSchedulerImplSpy.setAsyncJobDispatcher(asyncJobDispatcherMock);

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
             MockedStatic<com.cloud.utils.component.ComponentContext> componentContextMocked = Mockito.mockStatic(com.cloud.utils.component.ComponentContext.class)) {
            actionEventUtilsMocked.when(() -> ActionEventUtils.onScheduledActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())
            ).thenReturn(42L);

            Long eventId = snapshotSchedulerImplSpy.dispatchSnapshotCreateJob(snapshotScheduleVoMock, volumeVoMock, snapshotScheduleVoMock);

            Assert.assertEquals(Long.valueOf(42L), eventId);
        }

        verify(snapshotScheduleVoMock).setAsyncJobId(7L);
        verify(snapshotScheduleDaoMock).update(1L, snapshotScheduleVoMock);
    }

    // --- shouldSkipSchedule / scheduleSnapshots (#13454, #6827) ---

    @Test
    public void shouldSkipScheduleTestCannotBeScheduledReturnsTrueWithoutCheckingUnchangedVolume() {
        doReturn(false).when(snapshotSchedulerImplSpy).canSnapshotBeScheduled(Mockito.any(), Mockito.any());

        boolean result = snapshotSchedulerImplSpy.shouldSkipSchedule(snapshotScheduleVoMock, volumeVoMock);

        Assert.assertTrue(result);
        verify(snapshotSchedulerImplSpy, never()).shouldSkipUnchangedVolumeSnapshot(Mockito.any());
    }

    @Test
    public void shouldSkipScheduleTestUnchangedVolumeReschedulesAndReturnsTrue() {
        doReturn(true).when(snapshotSchedulerImplSpy).canSnapshotBeScheduled(Mockito.any(), Mockito.any());
        doReturn(true).when(snapshotSchedulerImplSpy).shouldSkipUnchangedVolumeSnapshot(Mockito.any());
        doNothing().when(snapshotSchedulerImplSpy).skipAndRescheduleSnapshot(Mockito.any(), Mockito.any());

        boolean result = snapshotSchedulerImplSpy.shouldSkipSchedule(snapshotScheduleVoMock, volumeVoMock);

        Assert.assertTrue(result);
        verify(snapshotSchedulerImplSpy).skipAndRescheduleSnapshot(snapshotScheduleVoMock, volumeVoMock);
    }

    @Test
    public void shouldSkipScheduleTestCanBeScheduledAndNotUnchangedReturnsFalse() {
        doReturn(true).when(snapshotSchedulerImplSpy).canSnapshotBeScheduled(Mockito.any(), Mockito.any());
        doReturn(false).when(snapshotSchedulerImplSpy).shouldSkipUnchangedVolumeSnapshot(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.shouldSkipSchedule(snapshotScheduleVoMock, volumeVoMock);

        Assert.assertFalse(result);
        verify(snapshotSchedulerImplSpy, never()).skipAndRescheduleSnapshot(Mockito.any(), Mockito.any());
    }

    @Test
    public void scheduleSnapshotsTestSkippedScheduleIsNeverLockedOrDispatched() throws Exception {
        ReflectionTestUtils.setField(snapshotSchedulerImplSpy, "_currentTimestamp", new Date());
        doReturn(List.of(snapshotScheduleVoMock)).when(snapshotScheduleDaoMock).getSchedulesToExecute(Mockito.any(Date.class));
        doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        doReturn(true).when(snapshotSchedulerImplSpy).shouldSkipSchedule(Mockito.any(), Mockito.any());

        snapshotSchedulerImplSpy.scheduleSnapshots();

        verify(snapshotScheduleDaoMock, never()).acquireInLockTable(Mockito.anyLong());
        verify(snapshotSchedulerImplSpy, never()).dispatchSnapshotCreateJob(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void scheduleSnapshotsTestDispatchFailureIsHandledAndLockIsReleased() throws Exception {
        ReflectionTestUtils.setField(snapshotSchedulerImplSpy, "_currentTimestamp", new Date());
        doReturn(1L).when(snapshotScheduleVoMock).getId();
        doReturn(List.of(snapshotScheduleVoMock)).when(snapshotScheduleDaoMock).getSchedulesToExecute(Mockito.any(Date.class));
        doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        doReturn(false).when(snapshotSchedulerImplSpy).shouldSkipSchedule(Mockito.any(), Mockito.any());
        doReturn(snapshotScheduleVoMock).when(snapshotScheduleDaoMock).acquireInLockTable(1L);
        doThrow(new RuntimeException("dispatch boom")).when(snapshotSchedulerImplSpy).dispatchSnapshotCreateJob(Mockito.any(), Mockito.any(), Mockito.any());
        doNothing().when(snapshotSchedulerImplSpy).handleFailedSnapshotDispatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        snapshotSchedulerImplSpy.scheduleSnapshots();

        verify(snapshotSchedulerImplSpy).handleFailedSnapshotDispatch(Mockito.eq(snapshotScheduleVoMock), Mockito.eq(volumeVoMock), Mockito.eq(snapshotScheduleVoMock), Mockito.isNull(), Mockito.any(Exception.class));
        verify(snapshotScheduleDaoMock).releaseFromLockTable(1L);
    }

    // --- shouldSkipUnchangedVolumeSnapshot (#6827) ---

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestConfigDisabledByDefaultReturnsFalse() {
        doReturn(1L).when(volumeVoMock).getAccountId();
        doReturn(1L).when(volumeVoMock).getDataCenterId();
        doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
        Mockito.verifyNoInteractions(vmInstanceDaoMock);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestNoInstanceReturnsFalse() {
        stubSkipConfigEnabled();
        doReturn(null).when(volumeVoMock).getInstanceId();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestNoPriorSnapshotReturnsFalse() {
        stubSkipConfigEnabled();
        doReturn(5L).when(volumeVoMock).getInstanceId();
        doReturn(1L).when(volumeVoMock).getId();
        doReturn(null).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmRunningReturnsFalse() {
        stubSkipConfigEnabled();
        doReturn(5L).when(volumeVoMock).getInstanceId();
        doReturn(1L).when(volumeVoMock).getId();
        doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        doReturn(VirtualMachine.PowerState.PowerOn).when(vmInstanceVoMock).getPowerState();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmOffSinceBeforeLastSnapshotReturnsTrue() {
        stubSkipConfigEnabled();
        doReturn(5L).when(volumeVoMock).getInstanceId();
        doReturn(1L).when(volumeVoMock).getId();
        doReturn(new Date(2000L)).when(snapshotVoMock).getCreated();
        doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        doReturn(VirtualMachine.PowerState.PowerOff).when(vmInstanceVoMock).getPowerState();
        doReturn(new Date(1000L)).when(vmInstanceVoMock).getPowerStateUpdateTime();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmOffSinceAfterLastSnapshotReturnsFalse() {
        stubSkipConfigEnabled();
        doReturn(5L).when(volumeVoMock).getInstanceId();
        doReturn(1L).when(volumeVoMock).getId();
        doReturn(new Date(1000L)).when(snapshotVoMock).getCreated();
        doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        doReturn(VirtualMachine.PowerState.PowerOff).when(vmInstanceVoMock).getPowerState();
        doReturn(new Date(2000L)).when(vmInstanceVoMock).getPowerStateUpdateTime();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    private void stubSkipConfigEnabled() {
        doReturn(true).when(snapshotSchedulerImplSpy).getScopedConfigValue(Mockito.eq(SnapshotManager.SnapshotSkipIfVmNotRunning), Mockito.any(), Mockito.any());
        doReturn(1L).when(volumeVoMock).getAccountId();
        doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
    }

    @Test
    public void skipAndRescheduleSnapshotTestUpdatesScheduleAndLogsEvent() {
        doReturn(snapshotScheduleVoMock).when(snapshotScheduleDaoMock).acquireInLockTable(Mockito.anyLong());
        doReturn(1L).when(snapshotScheduleVoMock).getId();
        doReturn(1L).when(snapshotScheduleVoMock).getPolicyId();
        doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        doReturn(1L).when(volumeVoMock).getAccountId();

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.skipAndRescheduleSnapshot(snapshotScheduleVoMock, volumeVoMock);

            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_INFO), Mockito.eq(com.cloud.event.EventTypes.EVENT_SNAPSHOT_SKIPPED),
                    Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()));
        }

        verify(snapshotScheduleVoMock).setScheduledTimestamp(Mockito.any());
        verify(snapshotScheduleDaoMock).update(1L, snapshotScheduleVoMock);
        verify(snapshotScheduleDaoMock).releaseFromLockTable(1L);
    }

    @Test
    public void skipAndRescheduleSnapshotTestCouldNotAcquireLockDoesNothing() {
        doReturn(1L).when(snapshotScheduleVoMock).getId();
        doReturn(null).when(snapshotScheduleDaoMock).acquireInLockTable(1L);

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.skipAndRescheduleSnapshot(snapshotScheduleVoMock, volumeVoMock);

            actionEventUtilsMocked.verifyNoInteractions();
        }

        verify(snapshotScheduleDaoMock, never()).update(Mockito.anyLong(), Mockito.any());
        verify(snapshotScheduleDaoMock, never()).releaseFromLockTable(Mockito.anyLong());
    }

    // --- findLastSnapshot ---

    @Test
    public void findLastSnapshotTestReturnsMostRecentSnapshot() {
        doReturn(List.of(snapshotVoMock)).when(snapshotDaoMock).listByVolumeId(Mockito.any(), Mockito.eq(1L));

        SnapshotVO result = snapshotSchedulerImplSpy.findLastSnapshot(1L);

        Assert.assertSame(snapshotVoMock, result);
    }

    @Test
    public void findLastSnapshotTestNoSnapshotsReturnsNull() {
        doReturn(Collections.emptyList()).when(snapshotDaoMock).listByVolumeId(Mockito.any(), Mockito.eq(1L));

        SnapshotVO result = snapshotSchedulerImplSpy.findLastSnapshot(1L);

        Assert.assertNull(result);
    }
}
