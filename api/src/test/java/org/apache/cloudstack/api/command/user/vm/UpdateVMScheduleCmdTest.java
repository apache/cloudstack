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
import org.apache.cloudstack.vm.schedule.VMSchedule;
import org.apache.cloudstack.vm.schedule.VMScheduleManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.security.InvalidParameterException;

public class UpdateVMScheduleCmdTest {
    @Mock
    public VMScheduleManager vmScheduleManager;
    @Mock
    public EntityManager entityManager;
    @InjectMocks
    private UpdateVMScheduleCmd updateVMScheduleCmd = new UpdateVMScheduleCmd();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * given: "We have a VMScheduleManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd is executed successfully"
     * then: "a VMSchedule response is created"
     */
    @Test
    public void testSuccessfulExecution() {
        VMScheduleResponse vmScheduleResponse = Mockito.mock(VMScheduleResponse.class);
        Mockito.when(vmScheduleManager.updateSchedule(updateVMScheduleCmd)).thenReturn(vmScheduleResponse);
        updateVMScheduleCmd.execute();
        Assert.assertEquals(vmScheduleResponse, updateVMScheduleCmd.getResponseObject());
    }

    /**
     * given: "We have a VMScheduleManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd is executed with an invalid parameter"
     * then: "an InvalidParameterException is thrown"
     */
    @Test(expected = InvalidParameterException.class)
    public void testInvalidParameterException() {
        Mockito.when(vmScheduleManager.updateSchedule(updateVMScheduleCmd)).thenThrow(InvalidParameterException.class);
        updateVMScheduleCmd.execute();
    }

    /**
     * given: "We have an EntityManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd.getEntityOwnerId is executed for a VM which does exist"
     * then: "owner of that VM is returned"
     */
    @Test
    public void testSuccessfulGetEntityOwnerId() {
        VMSchedule vmSchedule = Mockito.mock(VMSchedule.class);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);

        Mockito.when(entityManager.findById(VMSchedule.class, updateVMScheduleCmd.getId())).thenReturn(vmSchedule);
        Mockito.when(entityManager.findById(VirtualMachine.class, vmSchedule.getVmId())).thenReturn(vm);

        long ownerId = updateVMScheduleCmd.getEntityOwnerId();
        Assert.assertEquals(vm.getAccountId(), ownerId);
    }

    /**
     * given: "We have an EntityManager and UpdateVMScheduleCmd"
     * when: "UpdateVMScheduleCmd.getEntityOwnerId is executed for a VM Schedule which doesn't exist"
     * then: "an InvalidParameterException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testFailureGetEntityOwnerId() {
        VMSchedule vmSchedule = Mockito.mock(VMSchedule.class);
        Mockito.when(entityManager.findById(VMSchedule.class, updateVMScheduleCmd.getId())).thenReturn(null);
        long ownerId = updateVMScheduleCmd.getEntityOwnerId();
    }
}
