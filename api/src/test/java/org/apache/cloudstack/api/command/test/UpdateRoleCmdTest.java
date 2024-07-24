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

package org.apache.cloudstack.api.command.test;

import junit.framework.TestCase;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.admin.acl.UpdateRoleCmd;
import org.apache.cloudstack.api.response.RoleResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;


public class UpdateRoleCmdTest extends TestCase{

    private UpdateRoleCmd updateRoleCmd;
    private RoleService roleService;
    private Role role;

    @Override
    @Before
    public void setUp() {
        roleService = Mockito.spy(RoleService.class);
        updateRoleCmd = new UpdateRoleCmd();
        ReflectionTestUtils.setField(updateRoleCmd,"roleService",roleService);
        ReflectionTestUtils.setField(updateRoleCmd,"roleId",1L);
        ReflectionTestUtils.setField(updateRoleCmd,"roleName","user");
        ReflectionTestUtils.setField(updateRoleCmd,"roleType", "User");
        ReflectionTestUtils.setField(updateRoleCmd,"roleDescription","Description Initial");
        role = Mockito.mock(Role.class);
    }

    @Test
    public void testUpdateSuccess() {
        when(roleService.findRole(updateRoleCmd.getRoleId())).thenReturn(role);
        when(role.getId()).thenReturn(1L);
        when(role.getUuid()).thenReturn("12345-abcgdkajd");
        when(role.getDescription()).thenReturn("Default user");
        when(role.getName()).thenReturn("User");
        when(role.getRoleType()).thenReturn(RoleType.User);
        when(roleService.updateRole(role, updateRoleCmd.getRoleName(), updateRoleCmd.getRoleType(), updateRoleCmd.getRoleDescription(), updateRoleCmd.isPublicRole())).thenReturn(role);
        when(role.getId()).thenReturn(1L);
        when(role.getDescription()).thenReturn("Description Initial");
        when(role.getName()).thenReturn("User");
        updateRoleCmd.execute();
        RoleResponse response = (RoleResponse) updateRoleCmd.getResponseObject();
        assertEquals((String)ReflectionTestUtils.getField(response, "roleName"),role.getName());
        assertEquals((String)ReflectionTestUtils.getField(response, "roleDescription"),role.getDescription());
    }
}
