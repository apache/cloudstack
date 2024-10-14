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
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.cloudstack.acl.dao.RolePermissionsDao;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private RolePermissionsDao rolePermissionsDaoMock;
    @Mock
    private RolePermission rolePermission1Mock;
    @Mock
    private RolePermission rolePermission2Mock;
    @Mock
    private Account callerAccountMock;
    @Mock
    private Role callerAccountRoleMock;
    @Mock
    private Role lessPermissionsRoleMock;
    @Mock
    private Role morePermissionsRoleMock;
    @Mock
    private Role differentPermissionsRoleMock;

    @Mock
    private Account accountMock;
    private long accountMockId = 100l;

    @Mock
    private RoleVO roleVoMock;
    private long roleMockId = 1l;

    private Map<String, Permission> rolePermissions = new HashMap<>();

    public void setUpRoleVisibilityTests() {
        Mockito.doReturn(List.of("api1", "api2", "api3")).when(accountManagerMock).getApiNameList();

        Mockito.doReturn(1L).when(callerAccountRoleMock).getId();
        Mockito.doReturn(callerAccountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(callerAccountRoleMock.getId()).when(callerAccountMock).getRoleId();

        Mockito.when(rolePermission1Mock.getRule()).thenReturn(new Rule("api1"));
        Mockito.when(rolePermission2Mock.getRule()).thenReturn(new Rule("api2"));
        Mockito.doReturn(RolePermissionEntity.Permission.ALLOW).when(rolePermission1Mock).getPermission();
        Mockito.doReturn(RolePermissionEntity.Permission.ALLOW).when(rolePermission2Mock).getPermission();

        List<RolePermission> lessPermissionsRolePermissions = Collections.singletonList(rolePermission1Mock);
        Mockito.doReturn(1L).when(lessPermissionsRoleMock).getId();
        Mockito.when(roleManagerImpl.findAllPermissionsBy(1L)).thenReturn(lessPermissionsRolePermissions);

        List<RolePermission> morePermissionsRolePermissions = List.of(rolePermission1Mock, rolePermission2Mock);
        Mockito.doReturn(2L).when(morePermissionsRoleMock).getId();
        Mockito.when(roleManagerImpl.findAllPermissionsBy(morePermissionsRoleMock.getId())).thenReturn(morePermissionsRolePermissions);

        List<RolePermission> differentPermissionsRolePermissions = Collections.singletonList(rolePermission2Mock);
        Mockito.doReturn(3L).when(differentPermissionsRoleMock).getId();
        Mockito.when(roleManagerImpl.findAllPermissionsBy(differentPermissionsRoleMock.getId())).thenReturn(differentPermissionsRolePermissions);
    }

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
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByName(roleName, null, null, null, null, false);

        roleManagerImpl.findRolesByName(roleName);
        Mockito.verify(roleManagerImpl).removeRolesIfNeeded(roles);
    }

    @Test
    public void removeRolesIfNeededTestRoleWithMoreAndSamePermissionsKeepRoles() {
        setUpRoleVisibilityTests();
        List<Role> roles = new ArrayList<>();

        List<RolePermission> callerAccountRolePermissions = List.of(rolePermission1Mock, rolePermission2Mock);
        Mockito.when(roleManagerImpl.findAllPermissionsBy(callerAccountRoleMock.getId())).thenReturn(callerAccountRolePermissions);

        roles.add(callerAccountRoleMock);
        roles.add(lessPermissionsRoleMock);

        roleManagerImpl.removeRolesIfNeeded(roles);

        Assert.assertEquals(2, roles.size());
        Assert.assertEquals(callerAccountRoleMock, roles.get(0));
        Assert.assertEquals(lessPermissionsRoleMock, roles.get(1));
    }

    @Test
    public void removeRolesIfNeededTestRoleWithLessPermissionsRemoveRoles() {
        setUpRoleVisibilityTests();
        List<Role> roles = new ArrayList<>();

        List<RolePermission> callerAccountRolePermissions = Collections.singletonList(rolePermission1Mock);
        Mockito.when(roleManagerImpl.findAllPermissionsBy(callerAccountRoleMock.getId())).thenReturn(callerAccountRolePermissions);


        roles.add(callerAccountRoleMock);
        roles.add(morePermissionsRoleMock);

        roleManagerImpl.removeRolesIfNeeded(roles);

        Assert.assertEquals(1, roles.size());
        Assert.assertEquals(callerAccountRoleMock, roles.get(0));
    }

    @Test
    public void removeRolesIfNeededTestRoleWithDifferentPermissionsRemoveRoles() {
        setUpRoleVisibilityTests();
        List<Role> roles = new ArrayList<>();

        List<RolePermission> callerAccountRolePermissions = Collections.singletonList(rolePermission1Mock);
        Mockito.when(roleManagerImpl.findAllPermissionsBy(callerAccountRoleMock.getId())).thenReturn(callerAccountRolePermissions);

        roles.add(callerAccountRoleMock);
        roles.add(differentPermissionsRoleMock);

        roleManagerImpl.removeRolesIfNeeded(roles);

        Assert.assertEquals(1, roles.size());
        Assert.assertEquals(callerAccountRoleMock, roles.get(0));
    }

    @Test
    public void roleHasPermissionTestRoleWithMoreAndSamePermissionsReturnsTrue() {
        setUpRoleVisibilityTests();
        rolePermissions.put("api1", Permission.ALLOW);
        rolePermissions.put("api2", Permission.ALLOW);

        boolean result = roleManagerImpl.roleHasPermission(rolePermissions, lessPermissionsRoleMock);

        Assert.assertTrue(result);
    }

    @Test
    public void roleHasPermissionTestRoleAllowedApisDoesNotContainRoleToAccessAllowedApiReturnsFalse() {
        setUpRoleVisibilityTests();
        rolePermissions.put("api2", Permission.ALLOW);
        rolePermissions.put("api3", Permission.ALLOW);

        boolean result = roleManagerImpl.roleHasPermission(rolePermissions, morePermissionsRoleMock);

        Assert.assertFalse(result);
    }

    @Test
    public void roleHasPermissionTestRolePermissionsDeniedApiContainRoleToAccessAllowedApiReturnsFalse() {
        setUpRoleVisibilityTests();
        rolePermissions.put("api1", Permission.ALLOW);
        rolePermissions.put("api2", Permission.DENY);

        boolean result = roleManagerImpl.roleHasPermission(rolePermissions, morePermissionsRoleMock);

        Assert.assertFalse(result);
    }

    @Test
    public void getRolePermissionsTestRoleReturnsRolePermissions() {
        setUpRoleVisibilityTests();

        Map<String, Permission> roleRulesAndPermissions = roleManagerImpl.getRoleRulesAndPermissions(morePermissionsRoleMock.getId());

        Assert.assertEquals(2, roleRulesAndPermissions.size());
        Assert.assertEquals(roleRulesAndPermissions.get("api1"), Permission.ALLOW);
        Assert.assertEquals(roleRulesAndPermissions.get("api2"), Permission.ALLOW);
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
        Mockito.verify(roleDaoMock, Mockito.times(0)).findAllByRoleType(Mockito.any(RoleType.class), Mockito.anyBoolean());
    }

    @Test
    public void findRolesByTypeTestAdminRoleRootAdminUser() {
        Mockito.doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));
        Pair<ArrayList<RoleVO>, Integer> toBeReturned = new Pair(roles, 1);
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByRoleType(RoleType.Admin, null, null, null, true);
        List<Role> returnedRoles = roleManagerImpl.findRolesByType(RoleType.Admin);

        Assert.assertEquals(1, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(2)).isRootAdmin(Mockito.anyLong());
    }

    @Test
    public void findRolesByTypeTestNonAdminRoleRootAdminUser() {
        Mockito.lenient().doReturn(accountMock).when(roleManagerImpl).getCurrentAccount();
        Mockito.lenient().doReturn(true).when(accountManagerMock).isRootAdmin(accountMockId);

        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));
        Pair<ArrayList<RoleVO>, Integer> toBeReturned = new Pair(roles, 1);
        Mockito.doReturn(toBeReturned).when(roleDaoMock).findAllByRoleType(RoleType.User, null, null, null, true);
        List<Role> returnedRoles = roleManagerImpl.findRolesByType(RoleType.User);

        Assert.assertEquals(1, returnedRoles.size());
        Mockito.verify(accountManagerMock, Mockito.times(1)).isRootAdmin(Mockito.anyLong());
    }

    @Test
    public void listRolesTest() {
        List<Role> roles = new ArrayList<>();
        roles.add(Mockito.mock(Role.class));

        Mockito.doReturn(roles).when(roleDaoMock).listAll();
        Mockito.doReturn(0).when(roleManagerImpl).removeRolesIfNeeded(roles);

        List<Role> returnedRoles = roleManagerImpl.listRoles();

        Assert.assertEquals(roles.size(), returnedRoles.size());
        Mockito.verify(roleDaoMock).listAll();
        Mockito.verify(roleManagerImpl).removeRolesIfNeeded(roles);
    }
}
