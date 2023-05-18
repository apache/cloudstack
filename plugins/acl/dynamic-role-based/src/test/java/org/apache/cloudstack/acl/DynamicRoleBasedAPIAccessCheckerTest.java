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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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
    private RoleService roleServiceMock;
    @Spy
    @InjectMocks
    private DynamicRoleBasedAPIAccessChecker apiAccessCheckerSpy;

    List<String> apiNames = new ArrayList<>(Arrays.asList("apiName"));

    private User getTestUser() {
        return new UserVO(12L, "some user", "password", "firstName", "lastName",
                "email@gmail.com", "GMT", "uuid", User.Source.UNKNOWN);
    }

    private Account getTestAccount() {
        return new AccountVO("some name", 1L, "network-domain", Account.Type.NORMAL, "some-uuid");
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
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(getTestAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn((RoleVO) getTestRole());

        // Enabled plugin
        Mockito.doReturn(true).when(apiAccessCheckerSpy).isEnabled();
        Mockito.doCallRealMethod().when(apiAccessCheckerSpy).checkAccess(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testInvalidAccountCheckAccess() {
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(null);
        try {
            apiAccessCheckerSpy.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testInvalidAccountRoleCheckAccess() {
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(null);
        try {
            apiAccessCheckerSpy.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testDefaultRootAdminAccess() {
        Mockito.when(accountService.getAccount(Mockito.anyLong())).thenReturn(new AccountVO("root admin", 1L, null, Account.Type.ADMIN, "some-uuid"));
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(new RoleVO(1L, "SomeRole", RoleType.Admin, "default root admin role"));
        assertTrue(apiAccessCheckerSpy.checkAccess(getTestUser(), "anyApi"));
    }

    @Test
    public void testInvalidRolePermissionsCheckAccess() {
        Mockito.when(roleServiceMock.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.<RolePermission>emptyList());
        try {
            apiAccessCheckerSpy.checkAccess(getTestUser(), "someApi");
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testValidAllowRolePermissionApiCheckAccess() {
        final String allowedApiName = "someAllowedApi";
        final RolePermission permission = new RolePermissionVO(1L, allowedApiName, Permission.ALLOW, null);
        Mockito.when(roleServiceMock.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        assertTrue(apiAccessCheckerSpy.checkAccess(getTestUser(), allowedApiName));
    }

    @Test
    public void testValidAllowRolePermissionWildcardCheckAccess() {
        final String allowedApiName = "someAllowedApi";
        final RolePermission permission = new RolePermissionVO(1L, "some*", Permission.ALLOW, null);
        Mockito.when(roleServiceMock.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        assertTrue(apiAccessCheckerSpy.checkAccess(getTestUser(), allowedApiName));
    }

    @Test
    public void testValidDenyRolePermissionApiCheckAccess() {
        final String denyApiName = "someDeniedApi";
        final RolePermission permission = new RolePermissionVO(1L, denyApiName, Permission.DENY, null);
        Mockito.when(roleServiceMock.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        try {
            apiAccessCheckerSpy.checkAccess(getTestUser(), denyApiName);
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testValidDenyRolePermissionWildcardCheckAccess() {
        final String denyApiName = "someDenyApi";
        final RolePermission permission = new RolePermissionVO(1L, "*Deny*", Permission.DENY, null);
        Mockito.when(roleServiceMock.findAllPermissionsBy(Mockito.anyLong())).thenReturn(Collections.singletonList(permission));
        try {
            apiAccessCheckerSpy.checkAccess(getTestUser(), denyApiName);
            fail("Exception was expected");
        } catch (PermissionDeniedException ignored) {
        }
    }

    @Test
    public void testAnnotationFallbackCheckAccess() {
        final String allowedApiName = "someApiWithAnnotations";
        apiAccessCheckerSpy.addApiToRoleBasedAnnotationsMap(getTestRole().getRoleType(), allowedApiName);
        assertTrue(apiAccessCheckerSpy.checkAccess(getTestUser(), allowedApiName));
    }

    @Test
    public void getApisAllowedToUserTestRoleServiceIsDisabledShouldReturnUnchangedList() {
        Mockito.doReturn(false).when(apiAccessCheckerSpy).isEnabled();

        List<String> apisReceived = apiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }

    @Test
    public void getApisAllowedToUserTestPermissionAllowForGivenApiShouldReturnUnchangedList() {
        final RolePermission permission = new RolePermissionVO(1L, "apiName", Permission.ALLOW, null);
        Mockito.doReturn(Collections.singletonList(permission)).when(roleServiceMock).findAllPermissionsBy(Mockito.anyLong());

        List<String> apisReceived = apiAccessCheckerSpy.getApisAllowedToUser(getTestRole(), getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }

    @Test
    public void getApisAllowedToUserTestPermissionDenyForGivenApiShouldReturnEmptyList() {
        final RolePermission permission = new RolePermissionVO(1L, "apiName", Permission.DENY, null);
        Mockito.doReturn(Collections.singletonList(permission)).when(roleServiceMock).findAllPermissionsBy(Mockito.anyLong());

        List<String> apisReceived = apiAccessCheckerSpy.getApisAllowedToUser(getTestRole(), getTestUser(), apiNames);
        Assert.assertEquals(0, apisReceived.size());
    }
}
