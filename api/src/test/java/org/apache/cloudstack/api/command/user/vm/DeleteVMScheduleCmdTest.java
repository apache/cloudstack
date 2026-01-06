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
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.vm.schedule.VMSchedule;
import org.apache.cloudstack.vm.schedule.VMScheduleManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class DeleteVMScheduleCmdTest {
    @Mock
    public VMScheduleManager vmScheduleManager;
    @Mock
    public EntityManager entityManager;

    @InjectMocks
    private DeleteVMScheduleCmd deleteVMScheduleCmd = new DeleteVMScheduleCmd();

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
     * given: "We have a VMScheduleManager and DeleteVMScheduleCmd"
     * when: "VMScheduleManager.removeSchedule() is executed returning 1 row"
     * then: "a Success response is created"
     */
    @Test
    public void testSuccessfulExecution() {
        final SuccessResponse response = new SuccessResponse();
        response.setResponseName(deleteVMScheduleCmd.getCommandName());
        response.setObjectName(VMSchedule.class.getSimpleName().toLowerCase());

        Mockito.when(vmScheduleManager.removeSchedule(deleteVMScheduleCmd)).thenReturn(1L);
        deleteVMScheduleCmd.execute();
        SuccessResponse actualResponse = (SuccessResponse) deleteVMScheduleCmd.getResponseObject();
        Assert.assertEquals(response.getResponseName(), actualResponse.getResponseName());
        Assert.assertEquals(response.getObjectName(), actualResponse.getObjectName());
    }

    /**
     * given: "We have a VMScheduleManager and DeleteVMScheduleCmd"
     * when: "VMScheduleManager.removeSchedule() is executed returning 0 row"
     * then: "ServerApiException is thrown"
     */
    @Test(expected = ServerApiException.class)
    public void testServerApiException() {
        Mockito.when(vmScheduleManager.removeSchedule(deleteVMScheduleCmd)).thenReturn(0L);
        deleteVMScheduleCmd.execute();
    }

    /**
     * given: "We have a VMScheduleManager and DeleteVMScheduleCmd"
     * when: "DeleteVMScheduleCmd is executed with an invalid parameter"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidParameterValueException() {
        Mockito.when(vmScheduleManager.removeSchedule(deleteVMScheduleCmd)).thenThrow(InvalidParameterValueException.class);
        deleteVMScheduleCmd.execute();
    }

    /**
     * given: "We have an EntityManager and DeleteVMScheduleCmd"
     * when: "DeleteVMScheduleCmd.getEntityOwnerId is executed for a VM which does exist"
     * then: "owner of that VM is returned"
     */
    @Test
    public void testSuccessfulGetEntityOwnerId() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(entityManager.findById(VirtualMachine.class, deleteVMScheduleCmd.getVmId())).thenReturn(vm);
        long ownerId = deleteVMScheduleCmd.getEntityOwnerId();
        Assert.assertEquals(vm.getAccountId(), ownerId);
    }

    /**
     * given: "We have an EntityManager and DeleteVMScheduleCmd"
     * when: "DeleteVMScheduleCmd.getEntityOwnerId is executed for a VM which doesn't exist"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testFailureGetEntityOwnerId() {
        Mockito.when(entityManager.findById(VirtualMachine.class, deleteVMScheduleCmd.getVmId())).thenReturn(null);
        long ownerId = deleteVMScheduleCmd.getEntityOwnerId();
    }
}
