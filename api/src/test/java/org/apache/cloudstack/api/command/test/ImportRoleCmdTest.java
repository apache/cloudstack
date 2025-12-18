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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.acl.ImportRoleCmd;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cloud.exception.InvalidParameterValueException;

import java.util.HashMap;
import java.util.Map;

public class ImportRoleCmdTest {
    private ImportRoleCmd importRoleCmd;
    private RoleService roleService;
    private Role role;

    @Before
    public void setUp() {
        roleService = Mockito.spy(RoleService.class);
        importRoleCmd = new ImportRoleCmd();
        ReflectionTestUtils.setField(importRoleCmd,"roleService",roleService);
        ReflectionTestUtils.setField(importRoleCmd,"roleName","Test User");
        ReflectionTestUtils.setField(importRoleCmd,"roleType", "User");
        ReflectionTestUtils.setField(importRoleCmd,"roleDescription","test user imported");
        role = Mockito.mock(Role.class);
    }

    @Test
    public void testImportRoleSuccess() {
        Map<String, Map<String, String>> rules = new HashMap<String, Map<String, String>>();

        //Rule 1
        Map<String, String> rule1 = new HashMap<String, String>();
        rule1.put(ApiConstants.RULE, "list*");
        rule1.put(ApiConstants.PERMISSION, "allow");
        rule1.put(ApiConstants.DESCRIPTION, "listing apis");
        rules.put("key1", rule1);

        //Rule 2
        Map<String, String> rule2 = new HashMap<String, String>();
        rule2.put(ApiConstants.RULE, "update*");
        rule2.put(ApiConstants.PERMISSION, "deny");
        rule2.put(ApiConstants.DESCRIPTION, "no update allowed");
        rules.put("key2", rule2);

        //Rule 3
        Map<String, String> rule3 = new HashMap<String, String>();
        rule3.put(ApiConstants.RULE, "get*");
        rule3.put(ApiConstants.PERMISSION, "allow");
        rule3.put(ApiConstants.DESCRIPTION, "get details");
        rules.put("key3", rule3);

        ReflectionTestUtils.setField(importRoleCmd,"rules",rules);

        when(role.getUuid()).thenReturn("12345-abcgdkajd");
        when(role.getDescription()).thenReturn("test user imported");
        when(role.getName()).thenReturn("Test User");
        when(role.getRoleType()).thenReturn(RoleType.User);
        when(role.getState()).thenReturn(Role.State.ENABLED);
        when(roleService.importRole(anyString(), any(), anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(role);

        importRoleCmd.execute();
        RoleResponse response = (RoleResponse) importRoleCmd.getResponseObject();
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleName"), role.getName());
        Assert.assertEquals((String) ReflectionTestUtils.getField(response, "roleDescription"), role.getDescription());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testImportRoleInvalidRule() {
        Map<String, Map<String, String>> rules = new HashMap<String, Map<String, String>>();
        Map<String, String> rule = new HashMap<String, String>();
        rule.put(ApiConstants.RULE, "*?+test*");
        rule.put(ApiConstants.PERMISSION, "allow");
        rule.put(ApiConstants.DESCRIPTION, "listing apis");
        rules.put("key1", rule);
        ReflectionTestUtils.setField(importRoleCmd,"rules",rules);

        importRoleCmd.execute();
        Assert.fail("An exception should have been thrown: " + InvalidParameterValueException.class);
    }

    @Test(expected = ServerApiException.class)
    public void testImportRoleInvalidPermission() {
        Map<String, Map<String, String>> rules = new HashMap<String, Map<String, String>>();
        Map<String, String> rule = new HashMap<String, String>();
        rule.put(ApiConstants.RULE, "list*");
        rule.put(ApiConstants.PERMISSION, "pass");
        rule.put(ApiConstants.DESCRIPTION, "listing apis");
        rules.put("key1", rule);
        ReflectionTestUtils.setField(importRoleCmd,"rules",rules);

        importRoleCmd.execute();
        Assert.fail("An exception should have been thrown: " + ServerApiException.class);
    }
}
