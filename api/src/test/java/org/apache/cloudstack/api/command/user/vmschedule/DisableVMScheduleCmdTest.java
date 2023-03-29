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

import com.cloud.vm.schedule.VMScheduleManager;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class DisableVMScheduleCmdTest extends TestCase {
    @Mock
    VMScheduleManager vmScheduleManager;

    @InjectMocks
    DisableVMScheduleCmd cmd = new DisableVMScheduleCmd();

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
    public void testExecute() {
        ReflectionTestUtils.setField(cmd, "id", 91L);
        Mockito.when(vmScheduleManager.disableVMSchedule(cmd.getId())).thenReturn(true);
    }
}