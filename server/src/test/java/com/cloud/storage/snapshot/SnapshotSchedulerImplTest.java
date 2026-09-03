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

import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
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

        Mockito.doReturn(snapshotPolicyVoMock).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotPolicyVO.class));

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        Mockito.verify(snapshotScheduleDaoMock, Mockito.never()).expunge(Mockito.anyLong());
    }

    @Test
    public void scheduleNextSnapshotJobTestPolicyIsNullCallExpunge() {
        Date expected = new Date();
        SnapshotPolicyVO snapshotPolicyVO = null;
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(1l);

        Mockito.doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
        Mockito.doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(snapshotPolicyVO);

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        Mockito.verify(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountIsNullReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(null).when(accountDaoMock).findById(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsDisabledReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(Account.State.DISABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsNotNullNorDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(Account.State.ENABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsRemovedReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(new Date()).when(volumeVoMock).getRemoved();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsNotAttachedToStoragePoolReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(null).when(volumeVoMock).getPoolId();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestAccountIsRemovedOrDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(true).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsRemovedCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        Mockito.verify(snapshotScheduleDaoMock).remove(Mockito.anyLong());
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsNotRemovedDoNotCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        SnapshotPolicyVO snapshotPolicyVO = new SnapshotPolicyVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        Mockito.doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        Mockito.verify(snapshotScheduleDaoMock, Mockito.never()).remove(Mockito.anyLong());
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobIsNullThenScheduleNextSnapshot() {
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        Mockito.doReturn(null).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        Mockito.verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobSucceededThenScheduleNextSnapshot() {
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        Mockito.doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(JobInfo.Status.SUCCEEDED).when(asyncJobVoMock).getStatus();
        Mockito.doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        Mockito.verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobFailedThenScheduleNextSnapshot() {
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        Mockito.doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(JobInfo.Status.FAILED).when(asyncJobVoMock).getStatus();
        Mockito.doReturn(new Date()).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        Mockito.verify(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    @Test
    public void scheduleNextSnapshotJobIfNecessaryTestAsyncJobInProgressThenDoNothing() {
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getAsyncJobId();
        Mockito.doReturn(asyncJobVoMock).when(asyncJobDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(JobInfo.Status.IN_PROGRESS).when(asyncJobVoMock).getStatus();

        snapshotSchedulerImplSpy.scheduleNextSnapshotJobIfNecessary(snapshotScheduleVoMock);

        Mockito.verify(snapshotSchedulerImplSpy, Mockito.never()).scheduleNextSnapshotJob(Mockito.any(SnapshotScheduleVO.class));
    }

    // --- countConsecutiveFailedAttempts (#13454) ---

    @Test
    public void countConsecutiveFailedAttemptsTestLimitZeroReturnsZeroWithoutQuerying() {
        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 0);

        Assert.assertEquals(0, result);
        Mockito.verify(eventDaoMock, Mockito.never()).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void countConsecutiveFailedAttemptsTestStopsAtFirstNonErrorEvent() {
        EventVO error1 = Mockito.mock(EventVO.class);
        Mockito.doReturn(EventVO.LEVEL_ERROR).when(error1).getLevel();
        EventVO error2 = Mockito.mock(EventVO.class);
        Mockito.doReturn(EventVO.LEVEL_ERROR).when(error2).getLevel();
        EventVO success = Mockito.mock(EventVO.class);
        Mockito.doReturn(EventVO.LEVEL_INFO).when(success).getLevel();

        Mockito.doReturn(List.of(error1, error2, success)).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());

        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 3);

        Assert.assertEquals(2, result);
    }

    @Test
    public void countConsecutiveFailedAttemptsTestNoEventsReturnsZero() {
        Mockito.doReturn(Collections.emptyList()).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());

        int result = snapshotSchedulerImplSpy.countConsecutiveFailedAttempts(1L, 3);

        Assert.assertEquals(0, result);
    }

    // --- getScopedConfigValue (#13454) ---

    @Test
    public void getScopedConfigValueTestFallsBackToGlobalDefaultWhenNoDepotConfigured() {
        Mockito.doReturn(1L).when(volumeVoMock).getAccountId();
        Mockito.doReturn(1L).when(volumeVoMock).getDataCenterId();
        Mockito.doReturn(1L).when(accountVoMock).getDomainId();

        Integer result = snapshotSchedulerImplSpy.getScopedConfigValue(SnapshotManager.SnapshotRecurringMaxFailures, volumeVoMock, accountVoMock);

        Assert.assertEquals(Integer.valueOf(3), result);
    }

    // --- handleFailedSnapshotDispatch (#13454) ---

    private void stubVolumeAndAccount() {
        Mockito.doReturn(1L).when(volumeVoMock).getId();
        Mockito.doReturn(1L).when(volumeVoMock).getAccountId();
        Mockito.doReturn(1L).when(volumeVoMock).getDataCenterId();
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
    }

    private void stubConsecutiveFailureEvents(int count) {
        List<EventVO> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EventVO event = Mockito.mock(EventVO.class);
            Mockito.doReturn(EventVO.LEVEL_ERROR).when(event).getLevel();
            events.add(event);
        }
        Mockito.doReturn(events).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
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
        Mockito.doReturn(Collections.emptyList()).when(eventDaoMock).listLatestEventsByResource(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
        ReflectionTestUtils.setField(snapshotSchedulerImplSpy, "_currentTimestamp", new Date());

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.handleFailedSnapshotDispatch(snapshotScheduleVoMock, volumeVoMock, snapshotScheduleVoMock, 5L, new Exception("boom"));

            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_ERROR), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.eq(5L)));
            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()), Mockito.never());
        }

        Mockito.verify(snapshotScheduleVoMock).setScheduledTimestamp(Mockito.any(Date.class));
        Mockito.verify(snapshotScheduleDaoMock).update(Mockito.anyLong(), Mockito.eq(snapshotScheduleVoMock));
    }

    @Test
    public void handleFailedSnapshotDispatchTestAtMaxGivesUpAndNotifies() {
        stubVolumeAndAccount();
        stubConsecutiveFailureEvents(2);

        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getPolicyId();
        Mockito.doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.handleFailedSnapshotDispatch(snapshotScheduleVoMock, volumeVoMock, snapshotScheduleVoMock, null, new Exception("boom"));

            verifyFailureThresholdEventsRaised(actionEventUtilsMocked);
        }

        Mockito.verify(snapshotScheduleDaoMock).update(Mockito.anyLong(), Mockito.eq(snapshotScheduleVoMock));
    }

    @Test
    public void recordSnapshotAttemptOutcomeTestAtMaxNotifiesWithDistinctEventType() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        stubVolumeAndAccount();
        stubConsecutiveFailureEvents(2);

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.recordSnapshotAttemptOutcome(snapshotScheduleVoMock, false, "boom");

            verifyFailureThresholdEventsRaised(actionEventUtilsMocked);
        }
    }

    // --- shouldSkipUnchangedVolumeSnapshot (#6827) ---

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestConfigDisabledByDefaultReturnsFalse() {
        Mockito.doReturn(1L).when(volumeVoMock).getAccountId();
        Mockito.doReturn(1L).when(volumeVoMock).getDataCenterId();
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
        Mockito.verifyNoInteractions(vmInstanceDaoMock);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestNoInstanceReturnsFalse() {
        stubSkipConfigEnabled();
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestNoPriorSnapshotReturnsFalse() {
        stubSkipConfigEnabled();
        Mockito.doReturn(5L).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(1L).when(volumeVoMock).getId();
        Mockito.doReturn(null).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmRunningReturnsFalse() {
        stubSkipConfigEnabled();
        Mockito.doReturn(5L).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(1L).when(volumeVoMock).getId();
        Mockito.doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        Mockito.doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        Mockito.doReturn(VirtualMachine.PowerState.PowerOn).when(vmInstanceVoMock).getPowerState();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmOffSinceBeforeLastSnapshotReturnsTrue() {
        stubSkipConfigEnabled();
        Mockito.doReturn(5L).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(1L).when(volumeVoMock).getId();
        Mockito.doReturn(new Date(2000L)).when(snapshotVoMock).getCreated();
        Mockito.doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        Mockito.doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        Mockito.doReturn(VirtualMachine.PowerState.PowerOff).when(vmInstanceVoMock).getPowerState();
        Mockito.doReturn(new Date(1000L)).when(vmInstanceVoMock).getPowerStateUpdateTime();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSkipUnchangedVolumeSnapshotTestVmOffSinceAfterLastSnapshotReturnsFalse() {
        stubSkipConfigEnabled();
        Mockito.doReturn(5L).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(1L).when(volumeVoMock).getId();
        Mockito.doReturn(new Date(1000L)).when(snapshotVoMock).getCreated();
        Mockito.doReturn(snapshotVoMock).when(snapshotSchedulerImplSpy).findLastSnapshot(Mockito.anyLong());
        Mockito.doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findById(5L);
        Mockito.doReturn(VirtualMachine.PowerState.PowerOff).when(vmInstanceVoMock).getPowerState();
        Mockito.doReturn(new Date(2000L)).when(vmInstanceVoMock).getPowerStateUpdateTime();

        boolean result = snapshotSchedulerImplSpy.shouldSkipUnchangedVolumeSnapshot(volumeVoMock);

        Assert.assertFalse(result);
    }

    private void stubSkipConfigEnabled() {
        Mockito.doReturn(true).when(snapshotSchedulerImplSpy).getScopedConfigValue(Mockito.eq(SnapshotManager.SnapshotSkipIfVmNotRunning), Mockito.any(), Mockito.any());
        Mockito.doReturn(1L).when(volumeVoMock).getAccountId();
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
    }

    @Test
    public void skipAndRescheduleSnapshotTestUpdatesScheduleAndLogsEvent() {
        Mockito.doReturn(snapshotScheduleVoMock).when(snapshotScheduleDaoMock).acquireInLockTable(Mockito.anyLong());
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getId();
        Mockito.doReturn(1L).when(snapshotScheduleVoMock).getPolicyId();
        Mockito.doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(1L).when(volumeVoMock).getAccountId();

        try (MockedStatic<ActionEventUtils> actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class)) {
            snapshotSchedulerImplSpy.skipAndRescheduleSnapshot(snapshotScheduleVoMock, volumeVoMock);

            actionEventUtilsMocked.verify(() -> ActionEventUtils.onCreatedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.eq(EventVO.LEVEL_INFO), Mockito.eq(com.cloud.event.EventTypes.EVENT_SNAPSHOT_SKIPPED),
                    Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()));
        }

        Mockito.verify(snapshotScheduleVoMock).setScheduledTimestamp(Mockito.any());
        Mockito.verify(snapshotScheduleDaoMock).update(Mockito.eq(1L), Mockito.eq(snapshotScheduleVoMock));
        Mockito.verify(snapshotScheduleDaoMock).releaseFromLockTable(1L);
    }
}
