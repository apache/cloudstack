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
package org.apache.cloudstack.discovery;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserVO;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.RoleVO;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordChangeRequired;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.Setup2FADetail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

@RunWith(MockitoJUnitRunner.class)
public class ApiDiscoveryTest {
    @Mock
    AccountService accountServiceMock;

    @Mock
    RoleService roleServiceMock;

    @Mock
    APIChecker apiCheckerMock;

    @Mock
    Map<String, ApiDiscoveryResponse> apiNameDiscoveryResponseMapMock;

    @Mock
    List<APIChecker> apiAccessCheckersMock;

    @Spy
    @InjectMocks
    ApiDiscoveryServiceImpl discoveryServiceSpy;

    @Mock
    UserAccount mockUserAccount;

    @Before
    public void setup() {
        discoveryServiceSpy.s_apiNameDiscoveryResponseMap = apiNameDiscoveryResponseMapMock;
        discoveryServiceSpy._apiAccessCheckers = apiAccessCheckersMock;

        Mockito.when(discoveryServiceSpy._apiAccessCheckers.iterator()).thenReturn(Arrays.asList(apiCheckerMock).iterator());
        Mockito.when(mockUserAccount.getDetails()).thenReturn(null);
        Mockito.when(accountServiceMock.getUserAccountById(anyLong())).thenReturn(mockUserAccount);
    }

    private User getTestUser() {
        return new UserVO(12L, "some user", "password", "firstName", "lastName",
                "email@gmail.com", "GMT", "uuid", User.Source.UNKNOWN);
    }

    private Account getNormalAccount() {
        return new AccountVO("some name", 1L, "network-domain", Account.Type.NORMAL, "some-uuid");
    }

    @Test (expected = PermissionDeniedException.class)
    public void listApisTestThrowPermissionDeniedExceptionOnAccountNull() throws PermissionDeniedException {
        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(null);
        discoveryServiceSpy.listApis(getTestUser(), null);
    }

    @Test (expected = PermissionDeniedException.class)
    public void listApisTestThrowPermissionDeniedExceptionOnRoleNull() throws PermissionDeniedException {
        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(getNormalAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(null);

        discoveryServiceSpy.listApis(getTestUser(), null);
    }

    @Test (expected = PermissionDeniedException.class)
    public void listApisTestThrowPermissionDeniedExceptionOnRoleUnknown() throws PermissionDeniedException {
        RoleVO unknownRoleVO = new RoleVO(-1L,"name", RoleType.Unknown, "description");

        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(getNormalAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(unknownRoleVO);

        discoveryServiceSpy.listApis(getTestUser(), null);
    }

    @Test
    public void listApisTestDoesNotGetApisAllowedToUserOnAdminRole() throws PermissionDeniedException {
        AccountVO adminAccountVO = new AccountVO("some name", 1L, "network-domain", Account.Type.ADMIN, "some-uuid");
        RoleVO adminRoleVO = new RoleVO(1L,"name", RoleType.Admin, "description");

        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(adminAccountVO);
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(adminRoleVO);

        discoveryServiceSpy.listApis(getTestUser(), null);

        Mockito.verify(apiCheckerMock, Mockito.times(0)).getApisAllowedToUser(any(Role.class), any(User.class), anyList());
    }

    @Test
    public void listApisTestGetsApisAllowedToUserOnUserRole() throws PermissionDeniedException {
        RoleVO userRoleVO = new RoleVO(4L, "name", RoleType.User, "description");

        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(getNormalAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(userRoleVO);

        discoveryServiceSpy.listApis(getTestUser(), null);

        Mockito.verify(apiCheckerMock, Mockito.times(1)).getApisAllowedToUser(any(Role.class), any(User.class), anyList());
    }

    @Test
    public void listApisForUserWithoutEnforcedPwdChange() throws PermissionDeniedException {
        RoleVO userRoleVO = new RoleVO(4L, "name", RoleType.User, "description");
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(Setup2FADetail, UserAccountVO.Setup2FAstatus.ENABLED.name());
        Mockito.when(mockUserAccount.getDetails()).thenReturn(userDetails);
        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(getNormalAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(userRoleVO);
        discoveryServiceSpy.listApis(getTestUser(), null);
        Mockito.verify(apiCheckerMock, Mockito.times(1)).getApisAllowedToUser(any(Role.class), any(User.class), anyList());
    }

    @Test
    public void listApisForUserEnforcedPwdChange() throws PermissionDeniedException {
        RoleVO userRoleVO = new RoleVO(4L, "name", RoleType.User, "description");
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(PasswordChangeRequired, "true");
        Mockito.when(mockUserAccount.getDetails()).thenReturn(userDetails);
        Mockito.when(accountServiceMock.getAccount(Mockito.anyLong())).thenReturn(getNormalAccount());
        Mockito.when(roleServiceMock.findRole(Mockito.anyLong())).thenReturn(userRoleVO);
        Mockito.when(apiNameDiscoveryResponseMapMock.get(Mockito.anyString())).thenReturn(Mockito.mock(ApiDiscoveryResponse.class));
        ListResponse<ApiDiscoveryResponse> response = (ListResponse<ApiDiscoveryResponse>) discoveryServiceSpy.listApis(getTestUser(), null);
        Assert.assertEquals(4, response.getResponses().size());
    }
}
