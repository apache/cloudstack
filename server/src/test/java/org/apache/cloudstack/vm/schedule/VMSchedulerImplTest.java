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
import com.cloud.event.EventTypes;
import com.cloud.user.User;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.vm.schedule.dao.VMScheduledJobDao;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityExistsException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionEventUtils.class})
public class VMSchedulerImplTest {

    @Spy
    @InjectMocks
    private VMSchedulerImpl vmScheduler = new VMSchedulerImpl();
    @Mock
    private VirtualMachineManager virtualMachineManager;
    @Mock
    private VMScheduleDao vmScheduleDao;
    @Mock
    private VMScheduledJobDao vmScheduledJobDao;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onScheduledActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())).thenReturn(1L);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
    }


    private void prepareMocksForProcessJob(VirtualMachine vm, VMScheduledJob vmScheduledJob, VirtualMachine.State vmState, VMSchedule.Action action, Long executeJobReturnValue) {
        Mockito.when(vm.getState()).thenReturn(vmState);
        Mockito.when(vmScheduledJob.getAction()).thenReturn(action);

        if (executeJobReturnValue != null) {
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeStartVMJob(vm, 1L);
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeStopVMJob(vm, false, 1L);
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeStopVMJob(vm, true, 1L);
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeRebootVMJob(vm, false, 1L);
            Mockito.doReturn(executeJobReturnValue).when(vmScheduler).executeRebootVMJob(vm, true, 1L);
        }
    }

    private void executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State state, VMSchedule.Action action) {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Long expectedValue = 1L;

        prepareMocksForProcessJob(vm, vmScheduledJob, state, action, expectedValue);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        PowerMockito.verifyStatic(ActionEventUtils.class);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), null,
                EventTypes.EVENT_VM_SCHEDULE_EXECUTE, true,
                String.format("Executing action (%s) for VM Id:%s", vmScheduledJob.getAction(), vm.getUuid()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        Assert.assertEquals(expectedValue, jobId);
    }

    @Test
    public void testProcessJobRunning() {
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.FORCE_STOP);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Running, VMSchedule.Action.FORCE_REBOOT);
        executeProcessJobWithVMStateAndActionNonSkipped(VirtualMachine.State.Stopped, VMSchedule.Action.START);
    }

    @Test
    public void testProcessJobInvalidAction() {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        prepareMocksForProcessJob(vm, vmScheduledJob, VirtualMachine.State.Running, VMSchedule.Action.START, null);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        PowerMockito.verifyStatic(ActionEventUtils.class);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), null,
                EventTypes.EVENT_VM_SCHEDULE_EXECUTE, true,
                String.format("Executing action (%s) for VM Id:%s", vmScheduledJob.getAction(), vm.getUuid()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), null,
                EventTypes.EVENT_VM_SCHEDULE_SKIPPED, true,
                String.format("Skipping action (%s) for [vmId:%s scheduleId: %s] because VM is invalid state: %s", vmScheduledJob.getAction(), vm.getUuid(), vmScheduledJob.getVmScheduleId(), vm.getState()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        Assert.assertNull(jobId);
    }

    @Test
    public void testProcessJobVMInInvalidState() {
        VMScheduledJob vmScheduledJob = Mockito.mock(VMScheduledJob.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        prepareMocksForProcessJob(vm, vmScheduledJob, VirtualMachine.State.Unknown, VMSchedule.Action.START, null);

        Long jobId = vmScheduler.processJob(vmScheduledJob, vm);

        PowerMockito.verifyStatic(ActionEventUtils.class);
        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, vm.getAccountId(), null,
                EventTypes.EVENT_VM_SCHEDULE_SKIPPED, true,
                String.format("Skipping action (%s) for [vmId:%s scheduleId: %s] because VM is invalid state: %s", vmScheduledJob.getAction(), vm.getUuid(), vmScheduledJob.getVmScheduleId(), vm.getState()),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);

        Assert.assertNull(jobId);
    }

    @Test
    public void testScheduleNextJobScheduleScheduleExists() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(startDate);
        Mockito.when(virtualMachineManager.findById(Mockito.anyLong())).thenReturn(vm);
        Mockito.when(vmScheduledJobDao.persist(Mockito.any())).thenThrow(EntityExistsException.class);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule);

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleFutureSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date startDate = DateUtils.addDays(now, 1);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(startDate, 1), Calendar.MINUTE);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(startDate);
        Mockito.when(virtualMachineManager.findById(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule);

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleCurrentSchedule() {
        Date now = DateUtils.setSeconds(new Date(), 0);
        Date expectedScheduledTime = DateUtils.round(DateUtils.addMinutes(now, 1), Calendar.MINUTE);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Mockito.when(vmSchedule.getSchedule()).thenReturn("* * * * *");
        Mockito.when(vmSchedule.getTimeZoneId()).thenReturn(TimeZone.getTimeZone("UTC").toZoneId());
        Mockito.when(vmSchedule.getStartDate()).thenReturn(DateUtils.addDays(now, -1));
        Mockito.when(virtualMachineManager.findById(Mockito.anyLong())).thenReturn(vm);
        Date actualScheduledTime = vmScheduler.scheduleNextJob(vmSchedule);

        Assert.assertEquals(expectedScheduledTime, actualScheduledTime);
    }

    @Test
    public void testScheduleNextJobScheduleExpired() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEndDate()).thenReturn(DateUtils.addMinutes(new Date(), -5));
        Mockito.when(vmSchedule.getEnabled()).thenReturn(true);
        Date actualDate = vmScheduler.scheduleNextJob(vmSchedule);
        Assert.assertNull(actualDate);
    }

    @Test
    public void testScheduleNextJobScheduleDisabled() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        Mockito.when(vmSchedule.getEnabled()).thenReturn(false);
        Date actualDate = vmScheduler.scheduleNextJob(vmSchedule);
        Assert.assertNull(actualDate);
    }

    @Test
    public void testScheduleNextJobScheduleIdNotExists() {
        long vmScheduleId = 1;
        Mockito.when(vmScheduleDao.findById(vmScheduleId)).thenReturn(null);
        Date actualDate = vmScheduler.scheduleNextJob(vmScheduleId);
        Assert.assertNull(actualDate);
    }

    @Test
    public void testScheduleNextJobScheduleIdExists() {
        long vmScheduleId = 1;
        VMScheduleVO vmScheduleVO = Mockito.mock(VMScheduleVO.class);
        Date date = Mockito.mock(Date.class);
        Mockito.when(vmScheduleDao.findById(vmScheduleId)).thenReturn(vmScheduleVO);
        Mockito.doReturn(date).when(vmScheduler).scheduleNextJob(vmScheduleVO);
        Date actualDate = vmScheduler.scheduleNextJob(vmScheduleId);
        Assert.assertEquals(date, actualDate);
    }
}
