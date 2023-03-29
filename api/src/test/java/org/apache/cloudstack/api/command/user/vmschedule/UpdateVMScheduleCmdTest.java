// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.vmschedule;

import com.cloud.vm.schedule.VMSchedule;
import com.cloud.vm.schedule.VMScheduleManager;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

public class UpdateVMScheduleCmdTest extends TestCase {

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    VMScheduleManager vmScheduleManager;

    @InjectMocks
    UpdateVMScheduleCmd cmd = new UpdateVMScheduleCmd();

    @Override
    protected void setUp() throws Exception {
        vmScheduleManager = Mockito.spy(VMScheduleManager.class);
    }
    @Test
    public void testGetId() {
        Long id = 91L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(cmd.getId(), id);
    }

    @Test
    public void testGetIntervalType() {
        String intervalType = "HOURLY";
        ReflectionTestUtils.setField(cmd, "intervalType", intervalType);
        Assert.assertEquals(cmd.getIntervalType(), intervalType);
    }

    @Test
    public void testGetSchedule() {
        String schedule = "15";
        ReflectionTestUtils.setField(cmd, "schedule", schedule);
        Assert.assertEquals(cmd.getSchedule(), schedule);
    }

    @Test
    public void testGetDescription() {
        String description = null;
        ReflectionTestUtils.setField(cmd, "description", description);
        Assert.assertEquals(cmd.getDescription(), description);
    }

    @Test
    public void testGetTag() {
        String tag = "hello";
        ReflectionTestUtils.setField(cmd, "tag", tag);
        Assert.assertEquals(cmd.getTag(), tag);
    }

    @Test
    public void testGetTimezone() {
        String timezone = "Asia/kolkata";
        ReflectionTestUtils.setField(cmd, "timezone", timezone);
        Assert.assertEquals(cmd.getTimezone(), timezone);
    }

    @Test
    public void testExecute() {
        ReflectionTestUtils.setField(cmd, "id", 91L);
        ReflectionTestUtils.setField(cmd, "intervalType", "HOURLY");
        ReflectionTestUtils.setField(cmd, "schedule", "30");
        ReflectionTestUtils.setField(cmd,  "action", "start");
        VMSchedule vmSchedule = Mockito.mock(VMSchedule.class);
        when(vmSchedule.getId()).thenReturn(91L);
        when(vmSchedule.getUuid()).thenReturn("9a0ce999-26ec-4087-8881-7aa9eb59b29b");
        when(vmSchedule.getDescription()).thenReturn("start vm");
        when(vmSchedule.getAction()).thenReturn("start");
        when(vmSchedule.getSchedule()).thenReturn("30");
        when(vmSchedule.getScheduleType()).thenReturn("HOURLY");
        VMScheduleResponse response = Mockito.mock(VMScheduleResponse.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        when(response.getId()).thenReturn(String.valueOf(91L));
        when(response.getDescription()).thenReturn("start vm");
        when(response.getAction()).thenReturn("start");
        when(response.getSchedule()).thenReturn("30");
        when(response.getScheduleType()).thenReturn("HOURLY");
        cmd._responseGenerator = responseGenerator;
        Mockito.when(vmScheduleManager.updateVMSchedule(cmd)).thenReturn(vmSchedule);
        Mockito.when(responseGenerator.createVMScheduleResponse(vmSchedule)).thenReturn(response);

        Assert.assertEquals(response.getAction(), "start");
        Assert.assertEquals(vmSchedule.getSchedule(), "30");
    }
}