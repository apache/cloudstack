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
package com.cloud.api;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.user.UserPasswordResetManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.api.ApiConstants.PASSWORD_CHANGE_REQUIRED;
import static org.apache.cloudstack.user.UserPasswordResetManager.UserPasswordResetEnabled;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    @Mock
    UserPasswordResetManager userPasswordResetManager;

    @Mock
    DomainManager domainManager;

    @Mock
    AccountManager accountManager;

    @Mock
    HttpSession session;

    @BeforeClass
    public static void beforeClass() throws Exception {
        overrideDefaultConfigValue(UserPasswordResetEnabled, "_value", true);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        overrideDefaultConfigValue(UserPasswordResetEnabled, "_value", false);
    }

    private static void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    private void runTestSetupIntegrationPortListenerInvalidPorts(Integer port) {
        try (MockedConstruction<ApiServer.ListenerThread> mocked =
                     Mockito.mockConstruction(ApiServer.ListenerThread.class)) {
            apiServer.setupIntegrationPortListener(port);
            Assert.assertTrue(mocked.constructed().isEmpty());
        }
    }

    @Test
    public void testSetupIntegrationPortListenerInvalidPorts() {
        List<Integer> ports = new ArrayList<>(List.of(-1, -10, 0));
        ports.add(null);
        for (Integer port : ports) {
            runTestSetupIntegrationPortListenerInvalidPorts(port);
        }
    }

    @Test
    public void testSetupIntegrationPortListenerValidPort() {
        Integer validPort = 8080;
        try (MockedConstruction<ApiServer.ListenerThread> mocked =
                     Mockito.mockConstruction(ApiServer.ListenerThread.class)) {
            apiServer.setupIntegrationPortListener(validPort);
            Assert.assertFalse(mocked.constructed().isEmpty());
            ApiServer.ListenerThread listenerThread = mocked.constructed().get(0);
            Mockito.verify(listenerThread).start();
        }
    }

    @Test
    public void testForgotPasswordSuccess() {
        UserAccount userAccount = mock(UserAccount.class);
        Domain domain = mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Active);
        Mockito.doNothing().when(userPasswordResetManager).setResetTokenAndSend(userAccount);
        Assert.assertTrue(apiServer.forgotPassword(userAccount, domain));
        Mockito.verify(userPasswordResetManager).setResetTokenAndSend(userAccount);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureNoEmail() {
        UserAccount userAccount = mock(UserAccount.class);
        Domain domain = mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledUser() {
        UserAccount userAccount = mock(UserAccount.class);
        Domain domain = mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledAccount() {
        UserAccount userAccount = mock(UserAccount.class);
        Domain domain = mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureInactiveDomain() {
        UserAccount userAccount = mock(UserAccount.class);
        Domain domain = mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Inactive);
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test
    public void testVerifyApiKeyAccessAllowed() {
        Long domainId = 1L;
        User user = mock(User.class);
        Account account = mock(Account.class);

        Mockito.when(user.getApiKeyAccess()).thenReturn(true);
        Assert.assertEquals(true, apiServer.verifyApiKeyAccessAllowed(user, account));
        Mockito.verify(account, Mockito.never()).getApiKeyAccess();

        Mockito.when(user.getApiKeyAccess()).thenReturn(false);
        Assert.assertEquals(false, apiServer.verifyApiKeyAccessAllowed(user, account));
        Mockito.verify(account, Mockito.never()).getApiKeyAccess();

        Mockito.when(user.getApiKeyAccess()).thenReturn(null);
        Mockito.when(account.getApiKeyAccess()).thenReturn(true);
        Assert.assertEquals(true, apiServer.verifyApiKeyAccessAllowed(user, account));

        Mockito.when(user.getApiKeyAccess()).thenReturn(null);
        Mockito.when(account.getApiKeyAccess()).thenReturn(false);
        Assert.assertEquals(false, apiServer.verifyApiKeyAccessAllowed(user, account));

        Mockito.when(user.getApiKeyAccess()).thenReturn(null);
        Mockito.when(account.getApiKeyAccess()).thenReturn(null);
        Assert.assertEquals(true, apiServer.verifyApiKeyAccessAllowed(user, account));
    }

    @Test
    public void testLoginUserSuccess() throws Exception {
        String username = "user";
        String password = "password";
        Long domainId = 1L;
        String domainPath = "/";
        InetAddress loginIp = InetAddress.getByName("127.0.0.1");
        Map<String, Object[]> requestParams = new HashMap<>();

        DomainVO domain = mock(DomainVO.class);
        Mockito.when(domain.getId()).thenReturn(domainId);
        Mockito.when(domain.getUuid()).thenReturn("domain-uuid");

        Mockito.when(domainManager.findDomainByIdOrPath(domainId, domainPath)).thenReturn(domain);
        Mockito.when(domainManager.getDomain(domainId)).thenReturn(domain);

        UserAccount userAccount = mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(100L);
        Mockito.when(userAccount.getAccountId()).thenReturn(200L);
        Mockito.when(userAccount.getUsername()).thenReturn(username);
        Mockito.when(userAccount.getFirstname()).thenReturn("First");
        Mockito.when(userAccount.getLastname()).thenReturn("Last");
        Mockito.when(userAccount.getTimezone()).thenReturn("UTC");
        Mockito.when(userAccount.getRegistrationToken()).thenReturn("token");
        Mockito.when(userAccount.isRegistered()).thenReturn(true);
        Mockito.when(userAccount.getDomainId()).thenReturn(domainId);
        Map<String, String> userAccDetails = new HashMap<>();
        userAccDetails.put(UserDetailVO.PasswordChangeRequired, "true");
        Mockito.when(userAccount.getDetails()).thenReturn(userAccDetails);

        Mockito.when(accountManager.authenticateUser(username, password, domainId, loginIp, requestParams)).thenReturn(userAccount);
        Mockito.when(accountManager.clearUserTwoFactorAuthenticationInSetupStateOnLogin(userAccount)).thenReturn(userAccount);

        Account account = mock(Account.class);
        Mockito.when(account.getAccountName()).thenReturn("account");
        Mockito.when(account.getDomainId()).thenReturn(domainId);
        Mockito.when(account.getType()).thenReturn(Account.Type.NORMAL);
        Mockito.when(account.getType()).thenReturn(Account.Type.NORMAL);
        Mockito.when(accountManager.getAccount(200L)).thenReturn(account);

        UserVO userVO = mock(UserVO.class);
        Mockito.when(userVO.getUuid()).thenReturn("user-uuid");
        Mockito.when(accountManager.getActiveUser(100L)).thenReturn(userVO);

        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of(PASSWORD_CHANGE_REQUIRED)));
        Mockito.when(session.getAttribute(PASSWORD_CHANGE_REQUIRED)).thenReturn(Boolean.TRUE);

        ResponseObject response = apiServer.loginUser(session, username, password, domainId, domainPath, loginIp, requestParams);
        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof LoginCmdResponse);
        Mockito.verify(session).setAttribute(eq("userid"), eq(100L));
        Mockito.verify(session).setAttribute(eq(ApiConstants.SESSIONKEY), anyString());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testLoginUserDomainNotFound() throws Exception {
        Mockito.when(domainManager.findDomainByIdOrPath(anyLong(), anyString())).thenReturn(null);
        apiServer.loginUser(session, "user", "pass", 1L, "/", null, null);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testLoginUserAuthFailed() throws Exception {
        DomainVO domain = mock(DomainVO.class);
        Mockito.when(domain.getId()).thenReturn(1L);
        Mockito.when(domainManager.findDomainByIdOrPath(anyLong(), anyString())).thenReturn(domain);
        Mockito.when(accountManager.authenticateUser(anyString(), anyString(), anyLong(), any(), any())).thenReturn(null);
        apiServer.loginUser(session, "user", "pass", 1L, "/", null, null);
    }
}
