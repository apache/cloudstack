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
package org.apache.cloudstack.api.command.user.bootgroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.junit.Test;

public class CreateInstanceBootGroupCmdTest extends BaseBootGroupCmdTest {

    private CreateInstanceBootGroupCmd createCmd() throws Exception {
        CreateInstanceBootGroupCmd cmd = new CreateInstanceBootGroupCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "_accountService", accountService);
        setField(cmd, "name", "web-tier");
        setField(cmd, "description", "web tier boot group");
        setField(cmd, "readinessAttemptTimeoutSeconds", 120L);
        setField(cmd, "readinessMaxRetryAttempts", 3L);
        setField(cmd, "readinessRebootOnRetry", true);
        setField(cmd, "readinessInitialDelaySeconds", 15L);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();
        assertEquals("web-tier", cmd.getName());
        assertEquals("web tier boot group", cmd.getDescription());
        assertEquals(Long.valueOf(120L), cmd.getReadinessAttemptTimeoutSeconds());
        assertEquals(Long.valueOf(3L), cmd.getReadinessMaxRetryAttempts());
        assertEquals(Boolean.TRUE, cmd.getReadinessRebootOnRetry());
        assertEquals(Long.valueOf(15L), cmd.getReadinessInitialDelaySeconds());
    }

    @Test
    public void testGetEntityOwnerIdResolvedFromAccount() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();
        setField(cmd, "accountName", "someaccount");
        setField(cmd, "domainId", 5L);
        when(accountService.finalizeAccountId("someaccount", 5L, null, true)).thenReturn(200L);

        assertEquals(200L, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetEntityOwnerIdFallsBackToCaller() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();
        when(accountService.finalizeAccountId(null, null, null, true)).thenReturn(null);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();

        InstanceBootGroup group = mock(InstanceBootGroup.class);
        when(group.getId()).thenReturn(ENTITY_ID);
        InstanceBootGroupResponse mockResponse = new InstanceBootGroupResponse();

        when(instanceBootGroupService.createInstanceBootGroup(cmd)).thenReturn(group);
        when(instanceBootGroupService.createInstanceBootGroupResponse(ENTITY_ID)).thenReturn(mockResponse);

        cmd.execute();

        InstanceBootGroupResponse response = (InstanceBootGroupResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("createinstancebootgroupresponse", response.getResponseName());
        verify(instanceBootGroupService).createInstanceBootGroup(cmd);
        verify(instanceBootGroupService).createInstanceBootGroupResponse(ENTITY_ID);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.createInstanceBootGroup(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        CreateInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.createInstanceBootGroup(cmd)).thenThrow(new RuntimeException("db error"));
        cmd.execute();
    }
}
