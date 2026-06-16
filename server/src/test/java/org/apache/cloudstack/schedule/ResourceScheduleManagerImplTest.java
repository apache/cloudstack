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
package org.apache.cloudstack.schedule;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDetailsDao;
import org.apache.cloudstack.schedule.autoscale.AutoScaleScheduleAction;
import org.apache.cloudstack.schedule.autoscale.AutoScaleScheduleWorker;
import org.apache.cloudstack.schedule.vm.VMScheduleAction;
import org.apache.cloudstack.schedule.vm.VMScheduleWorker;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

public class ResourceScheduleManagerImplTest {

    @Spy
    @InjectMocks
    ResourceScheduleManagerImpl resourceScheduleManager = new ResourceScheduleManagerImpl();

    @Mock
    ResourceScheduleDao resourceScheduleDao;

    @Mock
    ResourceScheduleDetailsDao resourceScheduleDetailsDao;

    @Mock
    VMScheduleWorker vmScheduleWorker;

    @Mock
    AutoScaleScheduleWorker autoScaleScheduleWorker;

    @Mock
    UserVmManager userVmManager;

    @Mock
    AccountManager accountManager;

    @Mock
    EntityManager entityManager;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        Account callingAccount = Mockito.mock(Account.class);
        User callingUser = Mockito.mock(User.class);
        CallContext.register(callingUser, callingAccount);

        Mockito.when(vmScheduleWorker.getApiResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(autoScaleScheduleWorker.getApiResourceType()).thenReturn(ApiCommandResourceType.AutoScaleVmGroup);
        Map<ApiCommandResourceType, BaseScheduleWorker> workerMap = new HashMap<>();
        workerMap.put(ApiCommandResourceType.VirtualMachine, vmScheduleWorker);
        workerMap.put(ApiCommandResourceType.AutoScaleVmGroup, autoScaleScheduleWorker);
        ReflectionTestUtils.setField(resourceScheduleManager, "workerMap", workerMap);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private void validateResponse(ResourceScheduleResponse response, ResourceScheduleVO schedule, VirtualMachine vm) {
        assertNotNull(response);
        Assert.assertEquals(schedule.getUuid(), ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals(schedule.getResourceType(), ReflectionTestUtils.getField(response, "resourceType"));
        Assert.assertEquals(vm.getUuid(), ReflectionTestUtils.getField(response, "resourceId"));
        Assert.assertEquals(schedule.getSchedule(), ReflectionTestUtils.getField(response, "schedule"));
        Assert.assertEquals(schedule.getTimeZone(), ReflectionTestUtils.getField(response, "timeZone"));
        ResourceSchedule.Action actionField = (ResourceSchedule.Action) ReflectionTestUtils.getField(response, "action");
        Assert.assertEquals(schedule.getActionName(), actionField == null ? null : actionField.name());
        Assert.assertEquals(schedule.getStartDate(), ReflectionTestUtils.getField(response, "startDate"));
        Assert.assertEquals(schedule.getEndDate(), ReflectionTestUtils.getField(response, "endDate"));
    }

    @Test
    public void createSchedule() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Account ownerAccount = Mockito.mock(Account.class);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(entityManager.findByUuid(VirtualMachine.class, "1")).thenReturn(null);
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);
        Mockito.when(vmScheduleWorker.isResourceValid(1L)).thenReturn(true);
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(vmScheduleWorker.parseAction("START")).thenReturn(VMScheduleAction.START);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(ownerAccount);
        Mockito.when(resourceScheduleDao.persist(Mockito.any(ResourceScheduleVO.class))).thenReturn(schedule);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getActionName()).thenReturn("START");
        Mockito.when(vmScheduleWorker.parseAction((String) Mockito.isNull())).thenReturn(null);

        ResourceScheduleResponse response = resourceScheduleManager.createSchedule(
                ApiCommandResourceType.VirtualMachine, "1", null,
                "0 0 * * *", "UTC", "START",
                DateUtils.addDays(new Date(), 1), DateUtils.addDays(new Date(), 2),
                true, null);

        Mockito.verify(resourceScheduleDao, Mockito.times(1)).persist(Mockito.any(ResourceScheduleVO.class));
        validateResponse(response, schedule, vm);
    }

    @Test
    public void createResponse() {
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(schedule.getActionName()).thenReturn("START");
        Mockito.when(vmScheduleWorker.parseAction("START")).thenReturn(VMScheduleAction.START);
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);

        ResourceScheduleResponse response = resourceScheduleManager.createResponse(schedule, null);
        validateResponse(response, schedule, vm);
    }

    @Test
    public void listSchedule() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        ResourceScheduleVO schedule1 = Mockito.mock(ResourceScheduleVO.class);
        ResourceScheduleVO schedule2 = Mockito.mock(ResourceScheduleVO.class);
        List<ResourceScheduleVO> scheduleList = new ArrayList<>();
        scheduleList.add(schedule1);
        scheduleList.add(schedule2);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(entityManager.findByUuid(VirtualMachine.class, "1")).thenReturn(null);
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(Mockito.mock(Account.class));

        Mockito.when(resourceScheduleDao.searchAndCount(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(new Pair<>(scheduleList, scheduleList.size()));
        for (ResourceScheduleVO schedule : scheduleList) {
            Mockito.when(schedule.getResourceId()).thenReturn(1L);
            Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        }

        ListResponse<ResourceScheduleResponse> responseList = resourceScheduleManager.listSchedule(
                null, null, ApiCommandResourceType.VirtualMachine, "1", null, null, 0L, 100L);

        Mockito.verify(resourceScheduleDao, Mockito.times(1)).searchAndCount(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());
        assertNotNull(responseList);
        Assert.assertEquals(2, (int) responseList.getCount());
        Assert.assertEquals(2, responseList.getResponses().size());

        for (int i = 0; i < responseList.getResponses().size(); i++) {
            validateResponse(responseList.getResponses().get(i), scheduleList.get(i), vm);
        }
    }

    @Test
    public void updateSchedule() {
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(resourceScheduleDao.findById(Mockito.anyLong())).thenReturn(schedule);
        Mockito.when(resourceScheduleDao.update(Mockito.eq(1L), Mockito.any(ResourceScheduleVO.class))).thenReturn(true);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(schedule.getActionName()).thenReturn("START");
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(vmScheduleWorker.parseAction("START")).thenReturn(VMScheduleAction.START);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(Mockito.mock(Account.class));
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);

        ResourceScheduleResponse response = resourceScheduleManager.updateSchedule(
                1L, null, "0 0 * * *", "UTC",
                DateUtils.addDays(new Date(), 1), DateUtils.addDays(new Date(), 2), null, null);
        Mockito.verify(resourceScheduleDao, Mockito.times(1)).update(Mockito.eq(1L), Mockito.any(ResourceScheduleVO.class));

        validateResponse(response, schedule, vm);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateScheduleEnableWithPastEndDateThrows() {
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);

        Mockito.when(resourceScheduleDao.findById(1L)).thenReturn(schedule);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(schedule.getSchedule()).thenReturn("0 0 * * *");
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(schedule.getEndDate()).thenReturn(DateUtils.addDays(new Date(), -1));
        Mockito.when(schedule.getTimeZone()).thenReturn("UTC");
        Mockito.when(schedule.getActionName()).thenReturn("START");
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(vmScheduleWorker.parseAction("START")).thenReturn(VMScheduleAction.START);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(Mockito.mock(Account.class));

        resourceScheduleManager.updateSchedule(1L, null, null, null, null, null, true, null);
    }

    @Test
    public void updateScheduleEnableWithFutureEndDateSucceeds() {
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(vm.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(resourceScheduleDao.findById(1L)).thenReturn(schedule);
        Mockito.when(resourceScheduleDao.update(Mockito.eq(1L), Mockito.any(ResourceScheduleVO.class))).thenReturn(true);
        Mockito.when(schedule.getResourceId()).thenReturn(1L);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(schedule.getSchedule()).thenReturn("0 0 * * *");
        Mockito.when(schedule.getStartDate()).thenReturn(DateUtils.addDays(new Date(), 1));
        Mockito.when(schedule.getEndDate()).thenReturn(DateUtils.addDays(new Date(), 2));
        Mockito.when(schedule.getTimeZone()).thenReturn("UTC");
        Mockito.when(schedule.getActionName()).thenReturn("START");
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(vmScheduleWorker.parseAction("START")).thenReturn(VMScheduleAction.START);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(Mockito.mock(Account.class));
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);

        resourceScheduleManager.updateSchedule(1L, null, null, null, null, null, true, null);

        Mockito.verify(resourceScheduleDao, Mockito.times(1)).update(Mockito.eq(1L), Mockito.any(ResourceScheduleVO.class));
        Mockito.verify(schedule).setEnabled(true);
    }

    @Test
    public void createScheduleAutoScale() {
        AutoScaleVmGroup group = Mockito.mock(AutoScaleVmGroup.class);
        ResourceScheduleVO schedule = Mockito.mock(ResourceScheduleVO.class);
        Account ownerAccount = Mockito.mock(Account.class);
        Map<String, String> details = new HashMap<>();
        details.put("minmembers", "2");
        details.put("maxmembers", "5");

        Mockito.when(group.getId()).thenReturn(21L);
        Mockito.when(group.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(entityManager.findByUuid(AutoScaleVmGroup.class, "asg-uuid")).thenReturn(group);
        Mockito.when(autoScaleScheduleWorker.isResourceValid(21L)).thenReturn(true);
        Mockito.when(autoScaleScheduleWorker.getEntityOwnerId(21L)).thenReturn(2L);
        Mockito.when(autoScaleScheduleWorker.parseAction("UPDATE")).thenReturn(AutoScaleScheduleAction.UPDATE);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(ownerAccount);
        Mockito.when(resourceScheduleDao.persist(Mockito.any(ResourceScheduleVO.class))).thenReturn(schedule);
        Mockito.when(schedule.getId()).thenReturn(99L);
        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.AutoScaleVmGroup);
        Mockito.when(schedule.getResourceId()).thenReturn(21L);
        Mockito.when(schedule.getActionName()).thenReturn("UPDATE");
        Mockito.when(autoScaleScheduleWorker.parseAction("UPDATE")).thenReturn(AutoScaleScheduleAction.UPDATE);
        Mockito.when(entityManager.findById(AutoScaleVmGroup.class, 21L)).thenReturn(group);

        ResourceScheduleResponse response = resourceScheduleManager.createSchedule(
                ApiCommandResourceType.AutoScaleVmGroup, "asg-uuid", null,
                "0 0 * * *", "UTC", "UPDATE",
                DateUtils.addDays(new Date(), 1), DateUtils.addDays(new Date(), 2),
                true, details);

        Assert.assertEquals(ApiCommandResourceType.AutoScaleVmGroup, ReflectionTestUtils.getField(response, "resourceType"));
        Assert.assertEquals("21", ReflectionTestUtils.getField(response, "resourceId"));
        Assert.assertEquals("2", ((Map<String, String>) ReflectionTestUtils.getField(response, "details")).get("minmembers"));
        Assert.assertEquals("5", ((Map<String, String>) ReflectionTestUtils.getField(response, "details")).get("maxmembers"));
        Mockito.verify(autoScaleScheduleWorker, Mockito.times(1)).validateDetails(Mockito.eq(AutoScaleScheduleAction.UPDATE), Mockito.eq(details));
    }

    @Test
    public void removeSchedulesForResource() {
        ResourceScheduleVO schedule1 = Mockito.mock(ResourceScheduleVO.class);
        ResourceScheduleVO schedule2 = Mockito.mock(ResourceScheduleVO.class);
        List<ResourceScheduleVO> scheduleList = new ArrayList<>();
        scheduleList.add(schedule1);
        scheduleList.add(schedule2);
        SearchCriteria<ResourceScheduleVO> sc = Mockito.mock(SearchCriteria.class);

        Mockito.when(resourceScheduleDao.getSearchCriteriaForResource(
                ApiCommandResourceType.VirtualMachine, 1L)).thenReturn(sc);
        Mockito.when(resourceScheduleDao.search(sc, null)).thenReturn(scheduleList);
        Mockito.when(schedule1.getId()).thenReturn(1L);
        Mockito.when(schedule2.getId()).thenReturn(2L);
        Mockito.when(resourceScheduleDao.removeAllSchedulesForResource(Mockito.any(), Mockito.anyLong())).thenReturn(2L);

        resourceScheduleManager.removeSchedulesForResource(ApiCommandResourceType.VirtualMachine, 1L);

        Mockito.verify(resourceScheduleDao, Mockito.times(1)).removeAllSchedulesForResource(Mockito.any(), Mockito.anyLong());
        Mockito.verify(vmScheduleWorker, Mockito.times(1)).removeScheduledJobs(Mockito.anyList());
    }

    @Test
    public void removeSchedule() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(entityManager.findByUuid(VirtualMachine.class, "1")).thenReturn(null);
        Mockito.when(entityManager.findById(VirtualMachine.class, 1L)).thenReturn(vm);
        Mockito.when(vmScheduleWorker.getEntityOwnerId(1L)).thenReturn(2L);
        Mockito.when(accountManager.getAccount(2L)).thenReturn(Mockito.mock(Account.class));
        Mockito.when(resourceScheduleDao.removeSchedulesForResourceAndIds(
                Mockito.any(), Mockito.anyLong(), Mockito.anyList())).thenReturn(1L);

        ResourceScheduleVO schedule1 = Mockito.mock(ResourceScheduleVO.class);
        ResourceScheduleVO schedule2 = Mockito.mock(ResourceScheduleVO.class);
        List<ResourceScheduleVO> scheduleList = new ArrayList<>();
        scheduleList.add(schedule1);
        scheduleList.add(schedule2);

        Mockito.when(resourceScheduleDao.searchAndCount(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(new Pair<>(scheduleList, scheduleList.size()));

        Long rowsRemoved = resourceScheduleManager.removeSchedule(
                ApiCommandResourceType.VirtualMachine, "1", 10L, null);

        Mockito.verify(resourceScheduleDao, Mockito.times(1)).removeSchedulesForResourceAndIds(
                Mockito.any(), Mockito.anyLong(), Mockito.anyList());
        Assert.assertEquals(1L, (long) rowsRemoved);
    }

    @Test
    public void validateStartDateEndDate() {
        // Valid scenario 1
        // Start date is before end date
        Date startDate = DateUtils.addDays(new Date(), 1);
        Date endDate = DateUtils.addDays(new Date(), 2);
        resourceScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());

        // Valid Scenario 2
        // Start date is before current date and end date is null
        endDate = null;
        resourceScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());

        // Invalid Scenario 2
        // Start date is before current date
        startDate = DateUtils.addDays(new Date(), -1);
        try {
            resourceScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
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
            resourceScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
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
            resourceScheduleManager.validateStartDateEndDate(startDate, endDate, TimeZone.getDefault());
            Assert.fail("Should have thrown InvalidParameterValueException");
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid value for end date. End date") &&
                    e.getMessage().contains("can't be before current time"));
        }
    }

    @Test
    public void getConfigKeys() {
        ConfigKey<?>[] configKeys = resourceScheduleManager.getConfigKeys();
        Assert.assertEquals(1, configKeys.length);
        Assert.assertEquals(BaseScheduleWorker.ScheduledJobExpireInterval.key(), configKeys[0].key());
    }
}
