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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Test;

public class ListInstanceBootGroupMembersCmdTest extends BaseBootGroupCmdTest {

    private ListInstanceBootGroupMembersCmd createCmd() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = new ListInstanceBootGroupMembersCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "bootGroupId", ENTITY_ID);
        setField(cmd, "memberType", "VirtualMachine");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getBootGroupId());
        assertEquals("VirtualMachine", cmd.getMemberType());
    }

    @Test
    public void testIsIgnoreInstanceStateNull() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        assertFalse(cmd.isIgnoreInstanceState());
    }

    @Test
    public void testIsIgnoreInstanceStateFalse() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        setField(cmd, "ignoreInstanceState", false);
        assertFalse(cmd.isIgnoreInstanceState());
    }

    @Test
    public void testIsIgnoreInstanceStateTrue() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        setField(cmd, "ignoreInstanceState", true);
        assertTrue(cmd.isIgnoreInstanceState());
    }

    @Test
    public void testDetailFlagsNullList() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        assertFalse(cmd.isReadinessDetailRequested());
        assertFalse(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsEmptyList() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        setField(cmd, "viewDetails", Collections.emptyList());
        assertFalse(cmd.isReadinessDetailRequested());
        assertFalse(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsListWithoutKeyword() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        setField(cmd, "viewDetails", Collections.singletonList("other"));
        assertFalse(cmd.isReadinessDetailRequested());
        assertFalse(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsReadinessOnly() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        List<String> details = Collections.singletonList("readiness");
        setField(cmd, "viewDetails", details);
        assertTrue(cmd.isReadinessDetailRequested());
        assertFalse(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsChildrenOnly() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        List<String> details = Collections.singletonList("children");
        setField(cmd, "viewDetails", details);
        assertFalse(cmd.isReadinessDetailRequested());
        assertTrue(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsAll() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        List<String> details = Collections.singletonList("all");
        setField(cmd, "viewDetails", details);
        assertTrue(cmd.isReadinessDetailRequested());
        assertTrue(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testDetailFlagsBothExplicit() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();
        List<String> details = Arrays.asList("readiness", "children");
        setField(cmd, "viewDetails", details);
        assertTrue(cmd.isReadinessDetailRequested());
        assertTrue(cmd.isChildrenDetailRequested());
    }

    @Test
    public void testExecute() throws Exception {
        ListInstanceBootGroupMembersCmd cmd = createCmd();

        ListResponse<InstanceBootGroupMemberResponse> mockListResponse = new ListResponse<>();
        when(instanceBootGroupService.listInstanceBootGroupMembers(cmd)).thenReturn(mockListResponse);

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<InstanceBootGroupMemberResponse> response = (ListResponse<InstanceBootGroupMemberResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listinstancebootgroupmembersresponse", response.getResponseName());
    }
}
