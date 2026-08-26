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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Test;

public class RemoveInstanceBootGroupMemberCmdTest extends BaseBootGroupCmdTest {

    private RemoveInstanceBootGroupMemberCmd createCmd() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = new RemoveInstanceBootGroupMemberCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "id", ENTITY_ID);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceType() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        assertEquals(ApiCommandResourceType.InstanceBootGroup, cmd.getApiResourceType());
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getApiResourceId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        when(instanceBootGroupService.removeInstanceBootGroupMember(cmd)).thenReturn(true);

        cmd.execute();

        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("removeinstancebootgroupmemberresponse", response.getResponseName());
        verify(instanceBootGroupService).removeInstanceBootGroupMember(cmd);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsFalse() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        when(instanceBootGroupService.removeInstanceBootGroupMember(cmd)).thenReturn(false);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        RemoveInstanceBootGroupMemberCmd cmd = createCmd();
        when(instanceBootGroupService.removeInstanceBootGroupMember(cmd)).thenThrow(new RuntimeException("conflict"));
        cmd.execute();
    }
}
