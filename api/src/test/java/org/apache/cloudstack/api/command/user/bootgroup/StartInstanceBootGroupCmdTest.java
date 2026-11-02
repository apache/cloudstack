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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroup;
import org.junit.Test;

import com.cloud.event.EventTypes;
import com.cloud.utils.exception.CloudRuntimeException;

public class StartInstanceBootGroupCmdTest extends BaseBootGroupCmdTest {

    private StartInstanceBootGroupCmd createCmd() throws Exception {
        StartInstanceBootGroupCmd cmd = new StartInstanceBootGroupCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "id", ENTITY_ID);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceType() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        assertEquals(ApiCommandResourceType.InstanceBootGroup, cmd.getApiResourceType());
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getApiResourceId());
    }

    @Test
    public void testEventType() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_INSTANCE_BOOT_GROUP_START, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        when(CallContext.current().getApiResourceUuid(ApiConstants.ID)).thenReturn("group-uuid");

        assertEquals("Starting Instance Boot Group with ID: group-uuid", cmd.getEventDescription());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();

        InstanceBootGroup group = mock(InstanceBootGroup.class);
        when(group.getId()).thenReturn(ENTITY_ID);
        InstanceBootGroupResponse mockResponse = new InstanceBootGroupResponse();

        when(instanceBootGroupService.startInstanceBootGroup(cmd)).thenReturn(group);
        when(instanceBootGroupService.createInstanceBootGroupResponse(ENTITY_ID)).thenReturn(mockResponse);

        cmd.execute();

        InstanceBootGroupResponse response = (InstanceBootGroupResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("startinstancebootgroupresponse", response.getResponseName());
        verify(instanceBootGroupService).startInstanceBootGroup(cmd);
        verify(instanceBootGroupService).createInstanceBootGroupResponse(ENTITY_ID);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.startInstanceBootGroup(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.startInstanceBootGroup(cmd)).thenThrow(new RuntimeException("db error"));
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteWrapsCloudRuntimeException() throws Exception {
        StartInstanceBootGroupCmd cmd = createCmd();
        when(instanceBootGroupService.startInstanceBootGroup(cmd)).thenThrow(new CloudRuntimeException("cannot start"));
        cmd.execute();
    }
}
