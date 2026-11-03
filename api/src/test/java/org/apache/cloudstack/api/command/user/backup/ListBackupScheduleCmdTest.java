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
package org.apache.cloudstack.api.command.user.backup;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupSchedule;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ListBackupScheduleCmdTest {

    @Mock
    private BackupManager backupManager;

    @Mock
    private ResponseGenerator responseGenerator;

    private ListBackupScheduleCmd cmd;

    @Before
    public void setUp() {
        cmd = new ListBackupScheduleCmd();
        cmd.backupManager = backupManager;
        cmd._responseGenerator = responseGenerator;
    }

    @Test
    public void testExecuteWithSchedules() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, NetworkRuleConflictException {
        BackupSchedule schedule = Mockito.mock(BackupSchedule.class);
        BackupScheduleResponse scheduleResponse = Mockito.mock(BackupScheduleResponse.class);
        List<BackupSchedule> schedules = new ArrayList<>();
        schedules.add(schedule);

        Mockito.when(backupManager.listBackupSchedules(cmd)).thenReturn(schedules);
        Mockito.when(responseGenerator.createBackupScheduleResponse(schedule)).thenReturn(scheduleResponse);

        Account mockAccount = Mockito.mock(Account.class);
        CallContext callContext = Mockito.mock(CallContext.class);
        try (org.mockito.MockedStatic<CallContext> mocked = Mockito.mockStatic(CallContext.class)) {
            cmd.execute();
        }

        ListResponse<?> response = (ListResponse<?>) cmd.getResponseObject();
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResponses().size());
        Assert.assertEquals(scheduleResponse, response.getResponses().get(0));
    }

    @Test
    public void testExecuteWithNoSchedules() {
        Mockito.when(backupManager.listBackupSchedules(cmd)).thenReturn(new ArrayList<>());
        CallContext callContext = Mockito.mock(CallContext.class);

        try (org.mockito.MockedStatic<CallContext> mocked = Mockito.mockStatic(CallContext.class)) {
            mocked.when(CallContext::current).thenReturn(callContext);
            cmd.execute();
        } catch (ResourceUnavailableException | InsufficientCapacityException | ResourceAllocationException |
                 NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }

        ListResponse<?> response = (ListResponse<?>) cmd.getResponseObject();
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getResponses().size());
    }
}
