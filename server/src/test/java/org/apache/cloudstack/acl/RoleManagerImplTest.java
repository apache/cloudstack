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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class RoleManagerImplTest {

    @Spy
    @InjectMocks
    private RoleManagerImpl roleManagerImpl;
    @Mock
    private AccountManager accountManagerMock;
    @Mock
    private RoleDao roleDaoMock;

    @Mock
    private Account accountMock;
    private long accountMockId = 100l;

    @Mock
    private RoleVO roleVoMock;
    private long roleMockId = 1l;

    @Before
    public void beforeTest() {
        Mockito.doReturn(accountMockId).when(accountMock).getId();
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();

        Mockito.doReturn(roleMockId).when(roleVoMock).getId();
    }

    @Test
    public void findRoleTestIdNull() {
        Role returnedRole = roleManagerImpl.findRole(null);
        Assert.assertNull(returnedRole);
    }

    @Test
    public void findRoleTestIdZero() {
        Role returnedRole = roleManagerImpl.findRole(0l);
        Assert.assertNull(returnedRole);
    }

    @Test
    public void findRoleTestIdNegative() {
        Role returnedRole = roleManagerImpl.findRole(-1l);
        Assert.assertNull(returnedRole);
    }

    @Test
    public void findRoleTestRoleNotFound() {
        Mockito.doReturn(null).when(roleDaoMock).findById(roleMockId);
        Role returnedRole = roleManagerImpl.findRole(roleMockId);
        Assert.assertNull(returnedRole);
    }

    @Test
    public void findRoleTestNotRootAdminAndNotRoleAdminType() {
        Mockito.doReturn(RoleType.DomainAdmin).when(roleVoMock).getRoleType();
        Mockito.doReturn(roleVoMock).when(roleDaoMock).findById(roleMockId);
        Mockito.doReturn(false).when(accountManagerMock).isRootAdmin(accountMockId);

        Role returnedRole = roleManagerImpl.findRole(roleMockId);

        Assert.assertEquals(roleMockId, returnedRole.getId());
        Mockito.verify(accountManagerMock).isRootAdmin(accountMockId);
        Mockito.verify(roleVoMock, Mockito.times(1)).getRoleType();
    }

    @Test
    public void findRoleTestRootAdminAndNotRoleAdminType() {
        Mockito.lenient().doReturn(RoleType.DomainAdmin).when(roleVoMock).getRoleType();
        Mockito.doReturn(roleVoMock).when(roleDaoMock).findById(roleMockId);
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        Role returnedRole = roleManagerImpl.findRole(roleMockId);

        Assert.assertEquals(roleMockId, returnedRole.getId());
        Mockito.verify(accountManagerMock).isRootAdmin(accountMockId);
        Mockito.verify(roleVoMock, Mockito.times(0)).getRoleType();
    }

    @Test
    public void findRoleTestRootAdminAndRoleAdminType() {
        Mockito.lenient().doReturn(RoleType.Admin).when(roleVoMock).getRoleType();
        Mockito.doReturn(roleVoMock).when(roleDaoMock).findById(roleMockId);
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        Role returnedRole = roleManagerImpl.findRole(roleMockId);

        Assert.assertEquals(roleMockId, returnedRole.getId());
        Mockito.verify(accountManagerMock).isRootAdmin(accountMockId);
        Mockito.verify(roleVoMock, Mockito.times(0)).getRoleType();
    }

    @Test
    public void findRoleTestNotRootAdminAndRoleAdminType() {
        Mockito.doReturn(RoleType.Admin).when(roleVoMock).getRoleType();
        Mockito.doReturn(roleVoMock).when(roleDaoMock).findById(roleMockId);
        Mockito.doReturn(false).when(accountManagerMock).isRootAdmin(accountMockId);

        Role returnedRole = roleManagerImpl.findRole(roleMockId);

        Assert.assertNull(returnedRole);
        Mockito.verify(accountManagerMock).isRootAdmin(accountMockId);
        Mockito.verify(roleVoMock, Mockito.times(1)).getRoleType();
    }

    @Test
    public void findRolesByNameTestNullRoleName() {
        List<Role> rolesFound = roleManagerImpl.findRolesByName(null);

        Assert.assertTrue(CollectionUtils.isEmpty(rolesFound));
    }

    @Test
    public void findRolesByNameTestEmptyRoleName() {
        List<Role> rolesFound = roleManagerImpl.findRolesByName("");

        Assert.assertTrue(CollectionUtils.isEmpty(rolesFound));
    }

    @Test
    public void findRolesByNameTestBlankRoleName() {
        List<Role> rolesFound = roleManagerImpl.findRolesByName("      ");

        Assert.assertTrue(CollectionUtils.isEmpty(rolesFound));
    }

    @Test
    public void findRolesByNameTest() {
        String roleName = "roleName";
        List<Role> roles = new ArrayList<>();
        Pair<ArrayList<RoleVO>, Integer> toBeReturned = new Pair(roles, 0);
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByName(roleName, null, null, null);

        roleManagerImpl.findRolesByName(roleName);
        Mockito.verify(roleManagerImpl).removeRootAdminRolesIfNeeded(roles);
    }

    @Test
    public void removeRootAdminRolesIfNeededTestRootAdmin() {
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roleManagerImpl.removeRootAdminRolesIfNeeded(roles);

        Mockito.verify(roleManagerImpl, Mockito.times(0)).removeRootAdminRoles(roles);
    }

    @Test
    public void removeRootAdminRolesIfNeededTestNonRootAdminUser() {
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(false).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roleManagerImpl.removeRootAdminRolesIfNeeded(roles);

        Mockito.verify(roleManagerImpl, Mockito.times(1)).removeRootAdminRoles(roles);
    }

    @Test
    public void removeRootAdminRolesTest() {
        List<Role> roles = new ArrayList<>();
        Role roleRootAdmin = Mockito.mock(Role.class);
        Mockito.doReturn(RoleType.Admin).when(roleRootAdmin).getRoleType();

        Role roleDomainAdmin = Mockito.mock(Role.class);
        Mockito.doReturn(RoleType.DomainAdmin).when(roleDomainAdmin).getRoleType();

        Role roleResourceAdmin = Mockito.mock(Role.class);
        Mockito.doReturn(RoleType.ResourceAdmin).when(roleResourceAdmin).getRoleType();

        Role roleUser = Mockito.mock(Role.class);
        Mockito.doReturn(RoleType.User).when(roleUser).getRoleType();

        roles.add(roleRootAdmin);
        roles.add(roleDomainAdmin);
        roles.add(roleResourceAdmin);
        roles.add(roleUser);

        roleManagerImpl.removeRootAdminRoles(roles);

        Assert.assertEquals(3, roles.size());
        Assert.assertEquals(roleDomainAdmin, roles.get(0));
        Assert.assertEquals(roleResourceAdmin, roles.get(1));
        Assert.assertEquals(roleUser, roles.get(2));
    }

    @Test
    public void findRolesByTypeTestNullRoleType() {
        List<Role> returnedRoles = roleManagerImpl.findRolesByType(null);

        Assert.assertEquals(0, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(0)).isRootAdmin(Mockito.anyLong());
    }

    @Test
    public void findRolesByTypeTestAdminRoleNonRootAdminUser() {
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(false).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> returnedRoles = roleManagerImpl.findRolesByType(RoleType.Admin);

        Assert.assertEquals(0, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(1)).isRootAdmin(Mockito.anyLong());
        Mockito.verify(roleDaoMock, Mockito.times(0)).findAllByRoleType(Mockito.any(RoleType.class));
    }

    @Test
    public void findRolesByTypeTestAdminRoleRootAdminUser() {
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));
        Pair<ArrayList<RoleVO>, Integer> toBeReturned = new Pair(roles, 1);
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByRoleType(RoleType.Admin, null, null);
        List<Role> returnedRoles = roleManagerImpl.findRolesByType(RoleType.Admin);

        Assert.assertEquals(1, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(1)).isRootAdmin(Mockito.anyLong());
    }

    @Test
    public void findRolesByTypeTestNonAdminRoleRootAdminUser() {
        Mockito.lenient().doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.lenient().doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));
        Pair<ArrayList<RoleVO>, Integer> toBeReturned = new Pair(roles, 1);
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByRoleType(RoleType.User, null, null);
        List<Role> returnedRoles = roleManagerImpl.findRolesByType(RoleType.User);

        Assert.assertEquals(1, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(0)).isRootAdmin(Mockito.anyLong());
    }

    @Test
    public void listRolesTest() {
        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));

        Mockito.doReturn(roles).when(roleDaoMock).listAll();
        Mockito.doReturn(0).when(roleManagerImpl).removeRootAdminRolesIfNeeded(roles);

        List<Role> returnedRoles = roleManagerImpl.listRoles();

        Assert.assertEquals(roles.size(), returnedRoles.size());
        Mockito.verify(roleDaoMock).listAll();
        Mockito.verify(roleManagerImpl).removeRootAdminRolesIfNeeded(roles);
    }
}
