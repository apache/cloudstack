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

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.admin.acl.CreateRoleCmd;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

public class CreateRoleCmdTest {
    private CreateRoleCmd createRoleCmd;
    private RoleService roleService;
    private Role role;

    @Before
    public void setUp() {
        roleService = Mockito.spy(RoleService.class);
        createRoleCmd = new CreateRoleCmd();
        ReflectionTestUtils.setField(createRoleCmd,"roleService",roleService);
        ReflectionTestUtils.setField(createRoleCmd,"roleName","testuser");
        ReflectionTestUtils.setField(createRoleCmd,"roleDescription","User test");
        role = Mockito.mock(Role.class);
    }

    @Test
    public void testCreateRoleWithRoleType() {
        ReflectionTestUtils.setField(createRoleCmd,"roleType", "User");
        when(role.getId()).thenReturn(1L);
        when(role.getUuid()).thenReturn("12345-abcgdkajd");
        when(role.getDescription()).thenReturn("User test");
        when(role.getName()).thenReturn("testuser");
        when(role.getRoleType()).thenReturn(RoleType.User);
        when(role.getState()).thenReturn(Role.State.ENABLED);
        when(roleService.createRole(createRoleCmd.getRoleName(), createRoleCmd.getRoleType(), createRoleCmd.getRoleDescription(), true)).thenReturn(role);
        createRoleCmd.execute();
        RoleResponse response = (RoleResponse) createRoleCmd.getResponseObject();
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleName"), role.getName());
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleDescription"), role.getDescription());
    }

    @Test
    public void testCreateRoleWithExistingRole() {
        ReflectionTestUtils.setField(createRoleCmd,"roleId",1L);
        when(roleService.findRole(createRoleCmd.getRoleId())).thenReturn(role);
        Role newRole = Mockito.mock(Role.class);
        when(newRole.getId()).thenReturn(2L);
        when(newRole.getUuid()).thenReturn("67890-xyztestid");
        when(newRole.getDescription()).thenReturn("User test");
        when(newRole.getName()).thenReturn("testuser");
        when(newRole.getRoleType()).thenReturn(RoleType.User);
        when(newRole.getState()).thenReturn(Role.State.ENABLED);
        when(roleService.createRole(createRoleCmd.getRoleName(), role, createRoleCmd.getRoleDescription(), true)).thenReturn(newRole);
        createRoleCmd.execute();
        RoleResponse response = (RoleResponse) createRoleCmd.getResponseObject();
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleName"), newRole.getName());
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleDescription"), newRole.getDescription());
    }

    @Test(expected = ServerApiException.class)
    public void testCreateRoleWithNonExistingRole() {
        ReflectionTestUtils.setField(createRoleCmd,"roleId",1L);
        when(roleService.findRole(createRoleCmd.getRoleId())).thenReturn(null);
        createRoleCmd.execute();
        Assert.fail("An exception should have been thrown: " + ServerApiException.class);
    }

    @Test(expected = ServerApiException.class)
    public void testCreateRoleValidateNeitherRoleIdNorTypeParameters() {
        createRoleCmd.execute();
        Assert.fail("An exception should have been thrown: " + ServerApiException.class);
    }

    @Test(expected = ServerApiException.class)
    public void testCreateRoleValidateBothRoleIdAndTypeParameters() {
        ReflectionTestUtils.setField(createRoleCmd,"roleId",1L);
        ReflectionTestUtils.setField(createRoleCmd,"roleType", "User");
        createRoleCmd.execute();
        Assert.fail("An exception should have been thrown: " + ServerApiException.class);
    }
}
