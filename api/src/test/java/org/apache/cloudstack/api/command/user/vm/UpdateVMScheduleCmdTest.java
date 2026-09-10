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
package org.apache.cloudstack.api.command.user.vm;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.schedule.ResourceSchedule;
import org.apache.cloudstack.schedule.ResourceScheduleManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class UpdateVMScheduleCmdTest {
    @Mock
    public ResourceScheduleManager resourceScheduleManager;

    @Mock
    public EntityManager entityManager;

    @InjectMocks
    private UpdateVMScheduleCmd updateVMScheduleCmd = new UpdateVMScheduleCmd();

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    /**
     * given: "We have a VMScheduleManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd is executed successfully"
     * then: "a VMSchedule response is created"
     */
    @Test
    public void testSuccessfulExecution() {
        ResourceScheduleResponse scheduleResponse = new ResourceScheduleResponse();
        scheduleResponse.setId("schedule-uuid");
        scheduleResponse.setResourceId("vm-uuid");
        Mockito.when(resourceScheduleManager.updateSchedule(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(scheduleResponse);
        updateVMScheduleCmd.execute();
        VMScheduleResponse response = (VMScheduleResponse) updateVMScheduleCmd.getResponseObject();
        Assert.assertNotNull(response);
        Assert.assertEquals("schedule-uuid", org.springframework.test.util.ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals("vm-uuid", org.springframework.test.util.ReflectionTestUtils.getField(response, "vmId"));
    }

    /**
     * given: "We have a VMScheduleManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd is executed with an invalid parameter"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidParameterValueException() {
        Mockito.when(resourceScheduleManager.updateSchedule(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenThrow(new InvalidParameterValueException("Invalid schedule"));
        updateVMScheduleCmd.execute();
    }

    /**
     * given: "We have an EntityManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd.getEntityOwnerId is executed for a VM which does exist"
     * then: "owner of that VM is returned"
     */
    @Test
    public void testSuccessfulGetEntityOwnerId() {
        ResourceSchedule schedule = Mockito.mock(ResourceSchedule.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(schedule.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine);
        Mockito.when(entityManager.findById(ResourceSchedule.class, updateVMScheduleCmd.getId())).thenReturn(schedule);
        Mockito.when(entityManager.findById(VirtualMachine.class, schedule.getResourceId())).thenReturn(vm);

        long ownerId = updateVMScheduleCmd.getEntityOwnerId();
        Assert.assertEquals(vm.getAccountId(), ownerId);
    }

    /**
     * given: "We have an EntityManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd.getEntityOwnerId is executed for a VM Schedule which doesn't exist"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testFailureGetEntityOwnerId() {
        Mockito.when(entityManager.findById(ResourceSchedule.class, updateVMScheduleCmd.getId())).thenReturn(null);
        updateVMScheduleCmd.getEntityOwnerId();
    }
}
