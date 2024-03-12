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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.command.user.vm.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

public class VMScheduleManagerImplTest {

    @Spy
    @InjectMocks
    VMScheduleManagerImpl vmScheduleManager = new VMScheduleManagerImpl();

    @Mock
    VMScheduleDao vmScheduleDao;

    @Mock
    VMScheduler vmScheduler;

    @Mock
    UserVmManager userVmManager;

    @Mock
    AccountManager accountManager;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        Account callingAccount = Mockito.mock(Account.class);
        User callingUser = Mockito.mock(User.class);
        CallContext.register(callingUser, callingAccount);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private void validateResponse(VMScheduleResponse response, VMSchedule vmSchedule, VirtualMachine vm) {
        assertNotNull(response);
        Assert.assertEquals(ReflectionTestUtils.getField(response, "id"), vmSchedule.getUuid());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "vmId"), vm.getUuid());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "schedule"), vmSchedule.getSchedule());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "timeZone"), vmSchedule.getTimeZone());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "action"), vmSchedule.getAction());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "startDate"), vmSchedule.getStartDate());
        Assert.assertEquals(ReflectionTestUtils.getField(response, "endDate"), vmSchedule.getEndDate());
    }

    @Test
    public void createSchedule() {
        UserVm vm = Mockito.mock(UserVm.class);
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        CreateVMScheduleCmd cmd = Mockito.mock(CreateVMScheduleCmd.class);

        Mockito.when(cmd.getVmId()).thenReturn(1L);
        Mockito.when(cmd.getSchedule()).thenReturn("0 0 * * *");
        Mockito.when(cmd.getTimeZone()).thenReturn("UTC");
        Mockito.when(cmd.getAction()).thenReturn("start");
        Mockito.when(cmd.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(cmd.getEndDate()).thenReturn(DateUtils.addDays(new Date(), 2));
        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(vmScheduleDao.persist(Mockito.any(VMScheduleVO.class))).thenReturn(vmSchedule);
        Mockito.when(userVmManager.getUserVm(Mockito.anyLong())).thenReturn(vm);
        Mockito.doNothing().when(accountManager).checkAccess(Mockito.any(Account.class), Mockito.isNull(), Mockito.eq(false), Mockito.eq(vm));
        VMScheduleResponse response = vmScheduleManager.createSchedule(cmd);
        Mockito.verify(vmScheduleDao, Mockito.times(1)).persist(Mockito.any(VMScheduleVO.class));

        validateResponse(response, vmSchedule, vm);
    }

    @Test
    public void createResponse() {
        VMSchedule vmSchedule = Mockito.mock(VMSchedule.class);
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vmSchedule.getVmId()).thenReturn(1L);
        Mockito.when(userVmManager.getUserVm(vmSchedule.getVmId())).thenReturn(vm);

        VMScheduleResponse response = vmScheduleManager.createResponse(vmSchedule);
        validateResponse(response, vmSchedule, vm);
    }

    @Test
    public void listSchedule() {
        UserVm vm = Mockito.mock(UserVm.class);
        VMScheduleVO vmSchedule1 = Mockito.mock(VMScheduleVO.class);
        VMScheduleVO vmSchedule2 = Mockito.mock(VMScheduleVO.class);
        List<VMScheduleVO> vmScheduleList = new ArrayList<>();
        vmScheduleList.add(vmSchedule1);
        vmScheduleList.add(vmSchedule2);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(userVmManager.getUserVm(1L)).thenReturn(vm);
        Mockito.when(
                vmScheduleDao.searchAndCount(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                        Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyLong())
        ).thenReturn(new Pair<>(vmScheduleList, vmScheduleList.size()));
        Mockito.when(vmSchedule1.getVmId()).thenReturn(1L);
        Mockito.when(vmSchedule2.getVmId()).thenReturn(1L);

        ListVMScheduleCmd cmd = Mockito.mock(ListVMScheduleCmd.class);
        Mockito.when(cmd.getVmId()).thenReturn(1L);

        ListResponse<VMScheduleResponse> responseList = vmScheduleManager.listSchedule(cmd);
        Mockito.verify(vmScheduleDao, Mockito.times(1)).searchAndCount(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyLong());
        assertNotNull(responseList);
        Assert.assertEquals(2, (int) responseList.getCount());
        Assert.assertEquals(2, responseList.getResponses().size());

        for (int i = 0; i < responseList.getResponses().size(); i++) {
            VMScheduleResponse response = responseList.getResponses().get(i);
            VMScheduleVO vmSchedule = vmScheduleList.get(i);
            validateResponse(response, vmSchedule, vm);
        }
    }

    @Test
    public void updateSchedule() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        UpdateVMScheduleCmd cmd = Mockito.mock(UpdateVMScheduleCmd.class);
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getSchedule()).thenReturn("0 0 * * *");
        Mockito.when(cmd.getTimeZone()).thenReturn("UTC");
        Mockito.when(cmd.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(cmd.getEndDate()).thenReturn(DateUtils.addDays(new Date(), 2));
        Mockito.when(vmScheduleDao.findById(Mockito.anyLong())).thenReturn(vmSchedule);
        Mockito.when(vmScheduleDao.update(Mockito.eq(cmd.getId()), Mockito.any(VMScheduleVO.class))).thenReturn(true);
        Mockito.when(vmSchedule.getVmId()).thenReturn(1L);
        Mockito.when(vmSchedule.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(userVmManager.getUserVm(vmSchedule.getVmId())).thenReturn(vm);

        VMScheduleResponse response = vmScheduleManager.updateSchedule(cmd);
        Mockito.verify(vmScheduleDao, Mockito.times(1)).update(Mockito.eq(cmd.getId()), Mockito.any(VMScheduleVO.class));

        validateResponse(response, vmSchedule, vm);
    }

    @Test
    public void removeScheduleByVmId() {
        UserVm vm = Mockito.mock(UserVm.class);
        VMScheduleVO vmSchedule1 = Mockito.mock(VMScheduleVO.class);
        VMScheduleVO vmSchedule2 = Mockito.mock(VMScheduleVO.class);
        List<VMScheduleVO> vmScheduleList = new ArrayList<>();
        vmScheduleList.add(vmSchedule1);
        vmScheduleList.add(vmSchedule2);
        SearchCriteria<VMScheduleVO> sc = Mockito.mock(SearchCriteria.class);

        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vmScheduleDao.getSearchCriteriaForVMId(vm.getId())).thenReturn(sc);
        Mockito.when(vmScheduleDao.search(sc, null)).thenReturn(vmScheduleList);
        Mockito.when(vmSchedule1.getId()).thenReturn(1L);
        Mockito.when(vmSchedule2.getId()).thenReturn(2L);
        Mockito.when(vmScheduleDao.remove(sc)).thenReturn(2);

        long rowsRemoved = vmScheduleManager.removeScheduleByVmId(vm.getId(), false);

        Mockito.verify(vmScheduleDao, Mockito.times(1)).remove(sc);
        Assert.assertEquals(2, rowsRemoved);
    }

    @Test
    public void removeSchedule() {
        VMScheduleVO vmSchedule = Mockito.mock(VMScheduleVO.class);
        UserVm vm = Mockito.mock(UserVm.class);
        DeleteVMScheduleCmd cmd = Mockito.mock(DeleteVMScheduleCmd.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getVmId()).thenReturn(1L);
        Mockito.when(vmSchedule.getVmId()).thenReturn(1L);
        Mockito.when(userVmManager.getUserVm(cmd.getVmId())).thenReturn(vm);
        Mockito.when(vmScheduleDao.findById(Mockito.anyLong())).thenReturn(vmSchedule);
        Mockito.when(vmScheduleDao.removeSchedulesForVmIdAndIds(Mockito.anyLong(), Mockito.anyList())).thenReturn(1L);

        Long rowsRemoved = vmScheduleManager.removeSchedule(cmd);

        Mockito.verify(vmScheduleDao, Mockito.times(1)).removeSchedulesForVmIdAndIds(Mockito.anyLong(), Mockito.anyList());
        Assert.assertEquals(1L, (long) rowsRemoved);
    }

    @Test
    public void validateStartDateEndDate() {
        // Valid scenario 1
        // Start date is before end date
        Date startDate = DateUtils.addDays(new Date(), 1);
        Date endDate = DateUtils.addDays(new Date(), 2);
        vmScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());

        // Valid Scenario 2
        // Start date is before current date and end date is null
        endDate = null;
        vmScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());

        // Invalid Scenario 2
        // Start date is before current date
        startDate = DateUtils.addDays(new Date(), -1);
        try {
            vmScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
            Assert.fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid value for start date. Start date") &&
                    e.getMessage().contains("can't be before current time"));
        }

        // Invalid Scenario 2
        // Start date is after end date
        startDate = DateUtils.addDays(new Date(), 2);
        endDate = DateUtils.addDays(new Date(), 1);
        try {
            vmScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
            Assert.fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid value for end date. End date") &&
                    e.getMessage().contains("can't be before start date"));
        }

        // Invalid Scenario 3
        // End date is before current date
        startDate = DateUtils.addDays(new Date(), 1);
        endDate = DateUtils.addDays(new Date(), -1);
        try {
            vmScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
            Assert.fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid value for end date. End date") &&
                    e.getMessage().contains("can't be before current time"));
        }
    }
}
