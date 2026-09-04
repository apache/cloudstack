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
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Test;

public class ListInstanceBootGroupsCmdTest extends BaseBootGroupCmdTest {

    private ListInstanceBootGroupsCmd createCmd() throws Exception {
        ListInstanceBootGroupsCmd cmd = new ListInstanceBootGroupsCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "keyword", "web");
        setField(cmd, "virtualMachineId", 500L);
        setField(cmd, "instanceGroupId", 600L);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        ListInstanceBootGroupsCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals("web", cmd.getKeyword());
        assertEquals(Long.valueOf(500L), cmd.getVirtualMachineId());
        assertEquals(Long.valueOf(600L), cmd.getInstanceGroupId());
    }

    @Test
    public void testExecute() throws Exception {
        ListInstanceBootGroupsCmd cmd = createCmd();

        ListResponse<InstanceBootGroupResponse> mockListResponse = new ListResponse<>();
        when(instanceBootGroupService.listInstanceBootGroups(cmd)).thenReturn(mockListResponse);

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<InstanceBootGroupResponse> response = (ListResponse<InstanceBootGroupResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listinstancebootgroupsresponse", response.getResponseName());
    }
}
