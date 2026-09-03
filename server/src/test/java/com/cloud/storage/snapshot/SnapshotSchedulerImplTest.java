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

import static org.mockito.Mockito.doReturn;
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
    public void getScopedConfigValueTestFallsBackToGlobalDefaultWhenNoDepotConfigured() {
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
}
