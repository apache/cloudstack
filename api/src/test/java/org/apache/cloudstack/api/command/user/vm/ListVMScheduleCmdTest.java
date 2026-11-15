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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.schedule.ResourceScheduleManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;

public class ListVMScheduleCmdTest {
    @Mock
    public ResourceScheduleManager resourceScheduleManager;

    @InjectMocks
    private ListVMScheduleCmd listVMScheduleCmd = new ListVMScheduleCmd();
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
     * given: "We have a VMScheduleManager with 0 schedules and ListVMScheduleCmd"
     * when: "ListVMScheduleCmd is executed"
     * then: "a list of size 0 is returned"
     */
    @Test
    public void testEmptyResponse() {
        ListResponse<ResourceScheduleResponse> response = new ListResponse<ResourceScheduleResponse>();
        response.setResponses(new ArrayList<ResourceScheduleResponse>());
        Mockito.when(resourceScheduleManager.listSchedule(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(response);
        listVMScheduleCmd.execute();
        ListResponse<VMScheduleResponse> actualResponseObject = (ListResponse<VMScheduleResponse>) listVMScheduleCmd.getResponseObject();
        Assert.assertEquals(0L, actualResponseObject.getResponses().size());
    }

    /**
     * given: "We have a VMScheduleManager with 1 schedule and ListVMScheduleCmd"
     * when: "ListVMScheduleCmd is executed"
     * then: "a list of size 1 is returned"
     */
    @Test
    public void testNonEmptyResponse() {
        ListResponse<ResourceScheduleResponse> listResponse = new ListResponse<ResourceScheduleResponse>();
        ResourceScheduleResponse response = new ResourceScheduleResponse();
        response.setId("schedule-uuid");
        response.setResourceId("vm-uuid");
        listResponse.setResponses(Collections.singletonList(response));
        Mockito.when(resourceScheduleManager.listSchedule(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(listResponse);
        listVMScheduleCmd.execute();
        ListResponse<VMScheduleResponse> actualResponseObject = (ListResponse<VMScheduleResponse>) listVMScheduleCmd.getResponseObject();
        Assert.assertEquals(1L, actualResponseObject.getResponses().size());
        Assert.assertEquals("schedule-uuid",
                org.springframework.test.util.ReflectionTestUtils.getField(actualResponseObject.getResponses().get(0), "id"));
        Assert.assertEquals("vm-uuid",
                org.springframework.test.util.ReflectionTestUtils.getField(actualResponseObject.getResponses().get(0), "vmId"));
    }

    /**
     * given: "We have a VMScheduleManager and ListVMScheduleCmd"
     * when: "ListVMScheduleCmd is executed with an invalid parameter"
     * then: "an InvalidParameterValueException is thrown"
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidParameterValueException() {
        Mockito.when(resourceScheduleManager.listSchedule(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenThrow(InvalidParameterValueException.class);
        listVMScheduleCmd.execute();
        ListResponse<VMScheduleResponse> actualResponseObject = (ListResponse<VMScheduleResponse>) listVMScheduleCmd.getResponseObject();
    }
}
