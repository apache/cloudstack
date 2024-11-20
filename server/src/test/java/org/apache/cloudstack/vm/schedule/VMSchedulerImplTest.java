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
package org.apache.cloudstack.vm.schedule;

import com.cloud.event.ActionEventUtils;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.vm.schedule.dao.VMScheduledJobDao;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VMSchedulerImplTest {
    @Spy
    @InjectMocks
    private VMSchedulerImpl vmScheduler = new VMSchedulerImpl();

    @Mock
    private UserVmManager userVmManager;

    @Mock
    private VMScheduleDao vmScheduleDao;

    @Mock
    private VMScheduledJobDao vmScheduledJobDao;

    @Mock
    private EnumMap<VMSchedule.Action, String> actionEventMap;

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
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.FORCE_STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.FORCE_REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Stopped, VMSchedule.Action.START);
    }

    private void executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State state, VMSchedule.Action action) {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Long expectedValue = 1L;

        prepareMocksForProcessJob(vm, vmScheduledJob, state, action, expectedValue);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        actionEventUtilsMocked.verify(() -> ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), null,
                actionEventMap.get(action), true,
                String.format("Executing action (%s) for VM Id:%s", vmScheduledJob.getAction(), vm.getUuid()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0));
        Assert.assertEquals(expectedValue, jobId);
    }

    private void prepareMocksForProcessJob(VirtualMachine vm, VMScheduledJob vmScheduledJob,
                                           VirtualMachine.State vmState, VMSchedule.Action action,
                                           Long executeJobReturnValue) {
        Mockito.when(vm.getState()).thenReturn(vmState);
        Mockito.when(vmScheduledJob.getAction()).thenReturn(action);

        if (executeJobReturnValue != null) {
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeStartVMJob(
                    Mockito.any(VirtualMachine.class), Mockito.anyLong());
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeStopVMJob(
                    Mockito.any(VirtualMachine.class), Mockito.anyBoolean(), Mockito.anyLong());
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeRebootVMJob(
                    Mockito.any(VirtualMachine.class), Mockito.anyBoolean(), Mockito.anyLong());
        }
    }

    @Test
    public void testProcessJobInvalidAction() {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        prepareMocksForProcessJob(vm, vmScheduledJob, VirtualMachine.State.Running, VMSchedule.Action.START, null);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        Assert.assertNull(jobId);
    }

    @Test
    public void testProcessJobVMInInvalidState() {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        prepareMocksForProcessJob(vm, vmScheduledJob, VirtualMachine.State.Unknown, VMSchedule.Action.START, null);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        Assert.assertNull(jobId);
    }

    @Test
    public void testScheduleNextJobScheduleScheduleExists() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(startDate);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Mockito.when(vmScheduledJobDao.persist(Mockito.any())).thenThrow(EntityExistsException.class);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleFutureSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(startDate);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleFutureScheduleWithTimeZoneChecks() throws Exception {
        // Ensure that Date vmSchedulerImpl.scheduleNextJob(VMScheduleVO vmSchedule) generates
        // the correct scheduled time on basis of schedule(cron), start date
        // and the timezone of the user. The system running the test can have any timezone.
        String cron = "30 5 * * *";

        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);

        UserVm vm = Mockito.mock(UserVm.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn(cron);
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("EST").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(startDate);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);

        // The timezone of the user is EST. The cron expression is 30 5 * * *.
        // The start date is 1 day from now. The expected scheduled time is 5:30 AM EST.
        // The actual scheduled time is 10:30 AM UTC.
        // The actual scheduled time is 5:30 AM EST.
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(startDate.toInstant(), vmSchedule.getTimeZoneId());
        zonedDateTime = zonedDateTime.withHour(5).withMinute(30).withSecond(0).withNano(0);
        Date expectedScheduledTime = Date.from(zonedDateTime.toInstant());

        if (expectedScheduledTime.before(startDate)) {
            expectedScheduledTime = DateUtils.addDays(expectedScheduledTime, 1);
        }

        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule, new Date());
        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleCurrentSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(now, 1), Calendar.MINUTE);
        UserVm vm = Mockito.mock(UserVm.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule, new Date());

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleCurrentScheduleWithTimeZoneChecks() throws Exception {
        // Ensure that Date vmSchedulerImpl.scheduleNextJob(VMScheduleVO vmSchedule) generates
        // the correct scheduled time on basis of schedule(cron), start date
        // and the timezone of the user. The system running the test can have any timezone.
        String cron = "30 5 * * *";

        Date now = DateUtils.setSeconds(new Date(), 0);

        UserVm vm = Mockito.mock(UserVm.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn(cron);
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("EST").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);

        // The timezone of the user is EST. The cron expression is 30 5 * * *.
        // The start date is 1 day ago. The expected scheduled time is 5:30 AM EST.
        // The actual scheduled time is 10:30 AM UTC.
        // The actual scheduled time is 5:30 AM EST.
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(now.toInstant(), vmSchedule.getTimeZoneId());
        zonedDateTime = zonedDateTime.withHour(5).withMinute(30).withSecond(0).withNano(0);
        Date expectedScheduledTime = Date.from(zonedDateTime.toInstant());

        if (expectedScheduledTime.before(now)) {
            expectedScheduledTime = DateUtils.addDays(expectedScheduledTime, 1);
        }

        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule, new Date());
        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleExpired() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEndDate()).thenReturn(DateUtils.addMinutes(new Date(), -5));
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Date actualDate = vmScheduler.scheduleNextJob(vmSchedule, new Date());
        Assert.assertNull(actualDate);
    }

    @Test
    public void testScheduleNextJobScheduleDisabled() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(false);
        Date actualDate = vmScheduler.scheduleNextJob(vmSchedule, new Date());
        Assert.assertNull(actualDate);
    }


    @Test
    public void testExecuteJobs() {
        /*
         Test VMSchedulerImpl.executeJobs() method with a map of VMScheduledJob objects
         covering all the possible scenarios
         1. When the job is executed successfully
         2. When the job is skipped (processJob returns null)
        */

        VMScheduledJobVO job1 = Mockito.mock(VMScheduledJobVO.class);
        VMScheduledJobVO job2 = Mockito.mock(VMScheduledJobVO.class);

        UserVm vm1 = Mockito.mock(UserVm.class);
        UserVm vm2 = Mockito.mock(UserVm.class);

        Mockito.when(job2.getVmId()).thenReturn(2L);

        Mockito.when(userVmManager.getUserVm(2L)).thenReturn(vm2);

        Mockito.doReturn(null).when(vmScheduler).processJob(job2, vm2);

        Mockito.when(vmScheduledJobDao.acquireInLockTable(job1.getId())).thenReturn(job1);
        Mockito.when(vmScheduledJobDao.acquireInLockTable(job2.getId())).thenReturn(job2);

        Map<Long, VMScheduledJob> jobs = new HashMap<>();
        jobs.put(1L, job1);
        jobs.put(2L, job2);

        ReflectionTestUtils.setField(vmScheduler, "currentTimestamp", new Date());

        vmScheduler.executeJobs(jobs);

        Mockito.verify(vmScheduler, Mockito.times(2)).processJob(Mockito.any(), Mockito.any());
        Mockito.verify(vmScheduledJobDao, Mockito.times(2)).acquireInLockTable(Mockito.anyLong());
    }

    @Test
    public void testExecuteStopVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(StopVMCmd.class)).thenReturn(Mockito.mock(StopVMCmd.class));
            long jobId = vmScheduler.executeStopVMJob(vm, false, 1L);

            Assert.assertEquals(1L, jobId);
        }
    }

    @Test
    public void testExecuteRebootVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(RebootVMCmd.class)).thenReturn(Mockito.mock(RebootVMCmd.class));
            long jobId = vmScheduler.executeRebootVMJob(vm, false, 1L);

            Assert.assertEquals(1L, jobId);
        }
    }

    @Test
    public void testExecuteStartVMJob() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(asyncJobManager.submitAsyncJob(Mockito.any(AsyncJobVO.class))).thenReturn(1L);
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            when(ComponentContext.inject(StartVMCmd.class)).thenReturn(Mockito.mock(StartVMCmd.class));
            long jobId = vmScheduler.executeStartVMJob(vm, 1L);

            Assert.assertEquals(1L, jobId);
        }
    }
}
