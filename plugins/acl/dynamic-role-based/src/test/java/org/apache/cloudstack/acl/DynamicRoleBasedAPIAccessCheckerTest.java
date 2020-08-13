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

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class DynamicRoleBasedAPIAccessCheckerTest extends TestCase {

    @Mock
    private AccountService accountService;
    @Mock
    private RoleService roleService;

    private DynamicRoleBasedAPIAccessChecker apiAccessChecker;

    private User getTestUser() {
        return new UserVO(12L, "some user", "password", "firstName", "lastName",
                "email@gmail.com", "GMT", "uuid", User.Source.UNKNOWN);
    }

    private Account getTestAccount() {
        return new AccountVO("some name", 1L, "network-domain", (short)0, "some-uuid");
    }

    private Role getTestRole() {
        return new RoleVO(4L, "SomeRole", RoleType.User, "some description");
    }

    private void setupMockField(final Object obj, final String fieldName, final Object mock) throws NoSuchFieldException, IllegalAccessException {
        Field roleDaoField = DynamicRoleBasedAPIAccessChecker.class.getDeclaredField(fieldName);
        roleDaoField.setAccessible(true);
        roleDaoField.set(obj, mock);
    }

    @Override
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        apiAccessChecker = Mockito.spy(new DynamicRoleBasedAPIAccessChecker());
        setupMockField(apiAccessChecker, "accountService", accountService);
        setupMockField(apiAccessChecker, "roleService", roleService);

        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(getTestAccount());
        Mockito.when(roleService.findRole(Mockito.anyLong())).thenReturn((RoleVO) getTestRole());

        // Enabled plugin
        Mockito.doReturn(false).when(apiAccessChecker).isDisabled();
        Mockito.doCallRealMethod().when(apiAccessChecker).checkAccess(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testInvalidAccountCheckAccess() {
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(null);
        try {
            apiAccessChecker.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testInvalidAccountRoleCheckAccess() {
        Mockito.when(roleService.findRole(Mockito.anyLong())).thenReturn(null);
        try {
            apiAccessChecker.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testDefaultRootAdminAccess() {
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(new AccountVO("root admin", 1L, null, (short)1, "some-uuid"));
        Mockito.when(roleService.findRole(Mockito.anyLong())).thenReturn(new RoleVO(1L, "SomeRole", RoleType.Admin, "default root admin role"));
        assertTrue(apiAccessChecker.checkAccess(getTestUser(), "anyApi"));
    }

    @Test
    public void testInvalidRolePermissionsCheckAccess() {
        Mockito.when(roleService.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.<RolePermission>emptyList());
        try {
            apiAccessChecker.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testValidAllowRolePermissionApiCheckAccess() {
        final String allowedApiName = "someAllowedApi";
        final RolePermission permission = new RolePermissionVO(1L, allowedApiName, Permission.ALLOW, null);
        Mockito.when(roleService.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        assertTrue(apiAccessChecker.checkAccess(getTestUser(), allowedApiName));
    }

    @Test
    public void testValidAllowRolePermissionWildcardCheckAccess() {
        final String allowedApiName = "someAllowedApi";
        final RolePermission permission = new RolePermissionVO(1L, "some*", Permission.ALLOW, null);
        Mockito.when(roleService.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        assertTrue(apiAccessChecker.checkAccess(getTestUser(), allowedApiName));
    }

    @Test
    public void testValidDenyRolePermissionApiCheckAccess() {
        final String denyApiName = "someDeniedApi";
        final RolePermission permission = new RolePermissionVO(1L, denyApiName, Permission.DENY, null);
        Mockito.when(roleService.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        try {
            apiAccessChecker.checkAccess(getTestUser(), denyApiName);
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testValidDenyRolePermissionWildcardCheckAccess() {
        final String denyApiName = "someDenyApi";
        final RolePermission permission = new RolePermissionVO(1L, "*Deny*", Permission.DENY, null);
        Mockito.when(roleService.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        try {
            apiAccessChecker.checkAccess(getTestUser(), denyApiName);
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testAnnotationFallbackCheckAccess() {
        final String allowedApiName = "someApiWithAnnotations";
        apiAccessChecker.addApiToRoleBasedAnnotationsMap(getTestRole().getRoleType(), allowedApiName);
        assertTrue(apiAccessChecker.checkAccess(getTestUser(), allowedApiName));
    }

}