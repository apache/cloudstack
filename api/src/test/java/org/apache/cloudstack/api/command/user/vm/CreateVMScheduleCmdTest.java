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
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.vm.schedule.VMScheduleManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CreateVMScheduleCmdTest {
    @Mock
    public VMScheduleManager vmScheduleManager;
    @Mock
    public EntityManager entityManager;
    @InjectMocks
    private CreateVMScheduleCmd createVMScheduleCmd = new CreateVMScheduleCmd();

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
     * given: "We have a VMScheduleManager and CreateVMScheduleCmd"
     * when: "CreateVMScheduleCmd is executed successfully"
     * then: "a VMSchedule response is created"
     */
    @Test
    public void testSuccessfulExecution() {
        VMScheduleResponse vmScheduleResponse = Mockito.mock(VMScheduleResponse.class);
        Mockito.when(vmScheduleManager.createSchedule(createVMScheduleCmd)).thenReturn(vmScheduleResponse);
        createVMScheduleCmd.execute();
        Assert.assertEquals(vmScheduleResponse, createVMScheduleCmd.getResponseObject());
    }

    /**
     * given: "We have a VMScheduleManager and CreateVMScheduleCmd"
     * when: "CreateVMScheduleCmd is executed with an invalid parameter"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidParameterValueException() {
        Mockito.when(vmScheduleManager.createSchedule(createVMScheduleCmd)).thenThrow(InvalidParameterValueException.class);
        createVMScheduleCmd.execute();
    }

    /**
     * given: "We have an EntityManager and CreateVMScheduleCmd"
     * when: "CreateVMScheduleCmd.getEntityOwnerId is executed for a VM which does exist"
     * then: "owner of that VM is returned"
     */
    @Test
    public void testSuccessfulGetEntityOwnerId() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(entityManager.findById(VirtualMachine.class, createVMScheduleCmd.getVmId())).thenReturn(vm);
        long ownerId = createVMScheduleCmd.getEntityOwnerId();
        Assert.assertEquals(vm.getAccountId(), ownerId);
    }

    /**
     * given: "We have an EntityManager and CreateVMScheduleCmd"
     * when: "CreateVMScheduleCmd.getEntityOwnerId is executed for a VM which doesn't exist"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testFailureGetEntityOwnerId() {
        Mockito.when(entityManager.findById(VirtualMachine.class, createVMScheduleCmd.getVmId())).thenReturn(null);
        long ownerId = createVMScheduleCmd.getEntityOwnerId();
    }
}
