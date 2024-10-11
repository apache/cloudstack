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
package org.apache.cloudstack.acl;

import com.cloud.user.Account;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class RoleTypeTest {

    @Test
    public void testValidRoleTypeFromString() {
        for (RoleType roleType : RoleType.values()) {
            Assert.assertEquals(RoleType.fromString(roleType.name()), roleType);
        }
    }

    @Test
    public void testInvalidRoleTypeFromString() {
        for (String roleType : Arrays.asList(null, "", "admin", "12345%&^*")) {
            try {
                RoleType.fromString(roleType);
                Assert.fail("Invalid roletype provided, exception was expected");
            } catch (IllegalStateException e) {
                Assert.assertEquals(e.getMessage(), "Illegal RoleType name provided");
            }
        }
    }

    @Test
    public void testDefaultRoleMaskByValue() {
        Assert.assertEquals(RoleType.fromMask(1), RoleType.Admin);
        Assert.assertEquals(RoleType.fromMask(2), RoleType.ResourceAdmin);
        Assert.assertEquals(RoleType.fromMask(4), RoleType.DomainAdmin);
        Assert.assertEquals(RoleType.fromMask(8), RoleType.User);
        Assert.assertEquals(RoleType.fromMask(0), RoleType.Unknown);
    }

    @Test
    public void testGetByAccountType() {
        Assert.assertEquals(RoleType.getByAccountType(Account.Type.NORMAL), RoleType.User);
        Assert.assertEquals(RoleType.getByAccountType(Account.Type.ADMIN), RoleType.Admin);
        Assert.assertEquals(RoleType.getByAccountType(Account.Type.DOMAIN_ADMIN), RoleType.DomainAdmin);
        Assert.assertEquals(RoleType.getByAccountType(Account.Type.RESOURCE_DOMAIN_ADMIN), RoleType.ResourceAdmin);
        Assert.assertEquals(RoleType.getByAccountType(Account.Type.PROJECT), RoleType.Unknown);
    }

    @Test
    public void testGetRoleByAccountTypeWhenRoleIdIsProvided() {
        Assert.assertEquals(RoleType.getRoleByAccountType(123L, Account.Type.ADMIN), Long.valueOf(123L));
        Assert.assertEquals(RoleType.getRoleByAccountType(1234L, null), Long.valueOf(1234L));
    }

    @Test
    public void testGetRoleByAccountTypeForDefaultAccountTypes() {
        Assert.assertEquals(RoleType.getRoleByAccountType(null, Account.Type.ADMIN), (Long) RoleType.Admin.getId());
        Assert.assertEquals(RoleType.getRoleByAccountType(null, Account.Type.NORMAL), (Long) RoleType.User.getId());
        Assert.assertEquals(RoleType.getRoleByAccountType(null, Account.Type.DOMAIN_ADMIN), (Long) RoleType.DomainAdmin.getId());
        Assert.assertEquals(RoleType.getRoleByAccountType(null, Account.Type.RESOURCE_DOMAIN_ADMIN), (Long) RoleType.ResourceAdmin.getId());
        Assert.assertEquals(RoleType.getRoleByAccountType(null, Account.Type.PROJECT), null);
    }

    @Test
    public void testGetAccountTypeByRoleWhenRoleIsNull() {
        for (Account.Type accountType: Arrays.asList(
                Account.Type.NORMAL,
                Account.Type.ADMIN,
                Account.Type.DOMAIN_ADMIN,
                Account.Type.RESOURCE_DOMAIN_ADMIN,
                Account.Type.PROJECT,
                null)) {
            Assert.assertEquals(RoleType.getAccountTypeByRole(null, accountType), accountType);
        }
    }

    @Test
    public void testGetAccountTypeByRole() {
        Role role = Mockito.mock(Role.class);
        Mockito.when(role.getRoleType()).thenReturn(RoleType.Admin);
        Mockito.when(role.getId()).thenReturn(100L);
        Assert.assertEquals(RoleType.getAccountTypeByRole(role, null), RoleType.Admin.getAccountType());
    }
}
