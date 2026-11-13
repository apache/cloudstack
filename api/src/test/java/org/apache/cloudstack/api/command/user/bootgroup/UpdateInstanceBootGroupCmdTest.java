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

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.junit.Test;

public class UpdateInstanceBootGroupCmdTest extends BaseBootGroupCmdTest {

    private UpdateInstanceBootGroupCmd createCmd() throws Exception {
        UpdateInstanceBootGroupCmd cmd = new UpdateInstanceBootGroupCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "name", "web-tier-renamed");
        setField(cmd, "description", "updated description");
        setField(cmd, "readinessAttemptTimeoutSeconds", 180L);
        setField(cmd, "readinessMaxRetryAttempts", 5L);
        setField(cmd, "readinessRebootOnRetry", false);
        setField(cmd, "readinessInitialDelaySeconds", 30L);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals("web-tier-renamed", cmd.getName());
        assertEquals("updated description", cmd.getDescription());
        assertEquals(Long.valueOf(180L), cmd.getReadinessAttemptTimeoutSeconds());
        assertEquals(Long.valueOf(5L), cmd.getReadinessMaxRetryAttempts());
        assertEquals(Boolean.FALSE, cmd.getReadinessRebootOnRetry());
        assertEquals(Long.valueOf(30L), cmd.getReadinessInitialDelaySeconds());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceType() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();
        assertEquals(ApiCommandResourceType.InstanceBootGroup, cmd.getApiResourceType());
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getApiResourceId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();

        InstanceBootGroup group = mock(InstanceBootGroup.class);
        when(group.getId()).thenReturn(ENTITY_ID);
        InstanceBootGroupResponse mockResponse = new InstanceBootGroupResponse();

        when(instanceBootGroupService.updateInstanceBootGroup(cmd)).thenReturn(group);
        when(instanceBootGroupService.createInstanceBootGroupResponse(ENTITY_ID)).thenReturn(mockResponse);

        cmd.execute();

        InstanceBootGroupResponse response = (InstanceBootGroupResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("updateinstancebootgroupresponse", response.getResponseName());
        verify(instanceBootGroupService).updateInstanceBootGroup(cmd);
        verify(instanceBootGroupService).createInstanceBootGroupResponse(ENTITY_ID);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.updateInstanceBootGroup(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        UpdateInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.updateInstanceBootGroup(cmd)).thenThrow(new RuntimeException("db error"));
        cmd.execute();
    }
}
