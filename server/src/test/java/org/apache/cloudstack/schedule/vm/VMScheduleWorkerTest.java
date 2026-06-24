/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.schedule.vm;

import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.schedule.ResourceScheduleVO;
import org.apache.cloudstack.schedule.ResourceScheduledJobVO;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduledJobDao;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityExistsException;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VMScheduleWorkerTest {
    @Spy
    @InjectMocks
    private VMScheduleWorker vmScheduleWorker = new VMScheduleWorker();

    @Mock
    private UserVmManager userVmManager;

    @Mock
    private AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;

    @Mock
    private ResourceScheduleDao resourceScheduleDao;

    @Mock
    private ResourceScheduledJobDao resourceScheduledJobDao;

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private AsyncJobDispatcher asyncJobDispatcher;

    private AutoCloseable closeable;

    private MockedStatic<ActionEventUtils> actionEventUtilsMocked;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onScheduledActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(),
                        Mockito.anyLong()))
                .thenReturn(1L);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
    }

    @After
    public void tearDown() throws Exception {
        actionEventUtilsMocked.close();
        closeable.close();
    }

    @Test
    public void testProcessJobRunning() {
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMScheduleAction.STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMScheduleAction.FORCE_STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMScheduleAction.REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMScheduleAction.FORCE_REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Stopped, VMScheduleAction.START);
    }

    private void executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State state, VMScheduleAction action) {
        ResourceScheduledJobVO job = Mockito.mock(ResourceScheduledJobVO.class);
        UserVm vm = Mockito.mock(UserVm.class);

        Mockito.when(job.getResourceId()).thenReturn(1L);
        Mockito.when(job.getActionName()).thenReturn(action.name());
        Mockito.when(vm.getState()).thenReturn(state);
        Mockito.when(userVmManager.getUserVm(1L)).thenReturn(vm);
        Mockito.doReturn(1L).when(vmScheduleWorker).submitAsyncJob(
                Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        Long jobId = vmScheduleWorker.processJob(job);

        Assert.assertNotNull(jobId);
        Assert.assertEquals(1L, (long) jobId);
    }

    @Test
    public void testProcessJobInvalidAction() {
        ResourceScheduledJobVO job = Mockito.mock(ResourceScheduledJobVO.class);
        UserVm vm = Mockito.mock(UserVm.class);

        Mockito.when(job.getResourceId()).thenReturn(1L);
        Mockito.when(job.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(userVmManager.getUserVm(1L)).thenReturn(vm);

        Long jobId = vmScheduleWorker.processJob(job);

        Assert.assertNull(jobId);
    }

    @Test
    public void testProcessJobVMInInvalidState() {
        ResourceScheduledJobVO job = Mockito.mock(ResourceScheduledJobVO.class);
        UserVm vm = Mockito.mock(UserVm.class);

        Mockito.when(job.getResourceId()).thenReturn(1L);
        Mockito.when(job.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Unknown);
        Mockito.when(userVmManager.getUserVm(1L)).thenReturn(vm);

        Long jobId = vmScheduleWorker.processJob(job);

        Assert.assertNull(jobId);
    }

    @Test
    public void testScheduleNextJobScheduleScheduleExists() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(startDate);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Mockito.when(resourceScheduledJobDao.persist(Mockito.any())).thenThrow(EntityExistsException.class);
        Date actualScheduledTime = vmScheduleWorker.scheduleNextJob(schedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleFutureSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(startDate);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduleWorker.scheduleNextJob(schedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleFutureScheduleWithTimeZoneChecks() throws Exception {
        String cron = "30 5 * * *";
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        UserVm vm = Mockito.mock(UserVm.class);

        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn(cron);
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("EST").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(startDate);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(startDate.toInstant(), schedule.getTimeZoneId());
        zonedDateTime = zonedDateTime.withHour(5).withMinute(30).withSecond(0).withNano(0);
        Date expectedScheduledTime = Date.from(zonedDateTime.toInstant());
        if (expectedScheduledTime.before(startDate)) {
            expectedScheduledTime = Date.from(zonedDateTime.plusDays(1).toInstant());
        }

        Date actualScheduledTime = vmScheduleWorker.scheduleNextJob(schedule, new Date());
        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleCurrentSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(now, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduleWorker.scheduleNextJob(schedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleCurrentScheduleWithTimeZoneChecks() throws Exception {
        String cron = "30 5 * * *";
        Date now = DateUtils.setSeconds(new Date(), 0);
        UserVm vm = Mockito.mock(UserVm.class);

        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn(cron);
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("EST").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn(VMScheduleAction.START.name());
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(now.toInstant(), schedule.getTimeZoneId());
        zonedDateTime = zonedDateTime.withHour(5).withMinute(30).withSecond(0).withNano(0);
        Date expectedScheduledTime = Date.from(zonedDateTime.toInstant());
        if (expectedScheduledTime.before(now)) {
            expectedScheduledTime = DateUtils.addDays(expectedScheduledTime, 1);
        }

        Date actualScheduledTime = vmScheduleWorker.scheduleNextJob(schedule, new Date());
        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleExpired() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(schedule.getEndDate()).thenReturn(DateUtils.addMinutes(now, -5));
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(Mockito.mock(UserVm.class));
        Date actualDate = vmScheduleWorker.scheduleNextJob(schedule, new Date());
        Assert.assertNull(actualDate);
    }

    @Test
    public void testScheduleNextJobNextOccurrenceAfterEndDate() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(true);
        Mockito.when(schedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(schedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        // endDate is in the future (now < endDate), but the first occurrence (>= startDate)
        // falls after endDate, so no further jobs can ever be scheduled. The schedule's
        // declared lifetime hasn't ended yet though, so it must remain enabled until
        // endDate actually passes (handled separately by the "end time has passed" branch).
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(now, 10));
        Mockito.when(schedule.getEndDate()).thenReturn(DateUtils.addDays(now, 5));
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(Mockito.mock(UserVm.class));

        Date actualDate = vmScheduleWorker.scheduleNextJob(schedule, new Date());

        Assert.assertNull(actualDate);
        Mockito.verify(schedule, Mockito.never()).setEnabled(Mockito.anyBoolean());
        Mockito.verify(resourceScheduleDao, Mockito.never()).persist(Mockito.any());
    }

    @Test
    public void testScheduleNextJobScheduleDisabled() {
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Mockito.when(schedule.getEnabled()).thenReturn(false);
        Date actualDate = vmScheduleWorker.scheduleNextJob(schedule, new Date());
        Assert.assertNull(actualDate);
    }

    @Test
    public void testExecuteJobs() {
        ResourceScheduledJobVO job1 = Mockito.mock(ResourceScheduledJobVO.class);
        ResourceScheduledJobVO job2 = Mockito.mock(ResourceScheduledJobVO.class);

        Mockito.doReturn(null).when(vmScheduleWorker).processJob(job2);

        Mockito.when(resourceScheduledJobDao.acquireInLockTable(job1.getId())).thenReturn(job1);
        Mockito.when(resourceScheduledJobDao.acquireInLockTable(job2.getId())).thenReturn(job2);

        Map<Long, ResourceScheduledJobVO> jobs = new HashMap<>();
        jobs.put(1L, job1);
        jobs.put(2L, job2);

        ReflectionTestUtils.setField(vmScheduleWorker, "currentTimestamp", new Date());

        vmScheduleWorker.executeJobs(jobs);

        Mockito.verify(vmScheduleWorker, Mockito.times(2)).processJob(Mockito.any());
        Mockito.verify(resourceScheduledJobDao, Mockito.times(2)).acquireInLockTable(Mockito.anyLong());
    }

    @Test
    public void testSubmitStopVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        Mockito.when(asyncJobDispatcher.getName()).thenReturn("ApiAsyncJobDispatcher");
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(Mockito.any(StopVMCmd.class))).thenReturn(Mockito.mock(StopVMCmd.class));
            long jobId = vmScheduleWorker.submitAsyncJob(StopVMCmd.class, vm.getAccountId(), vm.getId(), 1L,
                    Map.of("forced", "false"));
            Assert.assertEquals(1L, jobId);
        }
    }

    @Test
    public void testSubmitRebootVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        Mockito.when(asyncJobDispatcher.getName()).thenReturn("ApiAsyncJobDispatcher");
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(Mockito.any(RebootVMCmd.class))).thenReturn(Mockito.mock(RebootVMCmd.class));
            long jobId = vmScheduleWorker.submitAsyncJob(RebootVMCmd.class, vm.getAccountId(), vm.getId(), 1L,
                    Map.of("forced", "false"));
            Assert.assertEquals(1L, jobId);
        }
    }

    @Test
    public void testSubmitStartVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        Mockito.when(asyncJobDispatcher.getName()).thenReturn("ApiAsyncJobDispatcher");
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(Mockito.any(StartVMCmd.class))).thenReturn(Mockito.mock(StartVMCmd.class));
            long jobId = vmScheduleWorker.submitAsyncJob(StartVMCmd.class, vm.getAccountId(), vm.getId(), 1L,
                    Map.of());
            Assert.assertEquals(1L, jobId);
        }
    }

    @Test
    public void parseActionResolvesEnum() {
        Assert.assertEquals(VMScheduleAction.START, vmScheduleWorker.parseAction("start"));
        Assert.assertEquals(VMScheduleAction.STOP, vmScheduleWorker.parseAction("STOP"));
        Assert.assertEquals(VMScheduleAction.FORCE_REBOOT, vmScheduleWorker.parseAction("Force_Reboot"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void parseActionThrowsOnUnknown() {
        vmScheduleWorker.parseAction("BOGUS");
    }
}
