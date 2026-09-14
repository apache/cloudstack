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
import com.cloud.exception.OriginDeniedException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.user.UserPasswordResetManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.cloudstack.user.UserPasswordResetManager.UserPasswordResetEnabled;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    @Mock
    UserPasswordResetManager userPasswordResetManager;

    @Mock
    AccountManager accountMgr;

    private static final String DEFAULT_CIDR_CHECKS_ENABLED = ApiServiceConfiguration.ApiSourceCidrChecksEnabled.defaultValue();
    private static final String DEFAULT_ALLOWED_CIDRS = ApiServiceConfiguration.ApiAllowedSourceCidrList.defaultValue();

    @BeforeClass
    public static void beforeClass() throws Exception {
        overrideDefaultConfigValue(UserPasswordResetEnabled, "_value", true);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        overrideDefaultConfigValue(UserPasswordResetEnabled, "_value", false);
    }

    @Before
    public void setupCommandAvailableChecks() {
        apiServer.setApiAccessCheckers(Collections.emptyList());
    }

    @After
    public void resetCidrConfig() throws Exception {
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiSourceCidrChecksEnabled, "_defaultValue", DEFAULT_CIDR_CHECKS_ENABLED);
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiAllowedSourceCidrList, "_defaultValue", DEFAULT_ALLOWED_CIDRS);
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
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

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
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledUser() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledAccount() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureInactiveDomain() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Inactive);
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test
    public void testVerifyApiKeyAccessAllowed() {
        Long domainId = 1L;
        User user = Mockito.mock(User.class);
        Account account = Mockito.mock(Account.class);

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
    public void testCheckCommandAvailableSkipsCidrLookupWhenDisabled() throws Exception {
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiSourceCidrChecksEnabled, "_defaultValue", "false");
        User user = Mockito.mock(User.class);

        ReflectionTestUtils.invokeMethod(apiServer, "checkCommandAvailable", user, "listVirtualMachines", InetAddress.getByName("127.0.0.1"));

        Mockito.verify(accountMgr, Mockito.never()).getAccount(Mockito.anyLong());
    }

    @Test
    public void testCheckCommandAvailableAllowsMatchingCidr() throws Exception {
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiSourceCidrChecksEnabled, "_defaultValue", "true");
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiAllowedSourceCidrList, "_defaultValue", "127.0.0.1/32");
        User user = Mockito.mock(User.class);
        Mockito.when(user.getAccountId()).thenReturn(1L);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountMgr.getAccount(1L)).thenReturn(account);

        ReflectionTestUtils.invokeMethod(apiServer, "checkCommandAvailable", user, "listVirtualMachines", InetAddress.getByName("127.0.0.1"));

        Mockito.verify(accountMgr).getAccount(1L);
    }

    @Test(expected = OriginDeniedException.class)
    public void testCheckCommandAvailableDeniesNonMatchingCidr() throws Exception {
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiSourceCidrChecksEnabled, "_defaultValue", "true");
        overrideDefaultConfigValue(ApiServiceConfiguration.ApiAllowedSourceCidrList, "_defaultValue", "10.0.0.0/8");
        User user = Mockito.mock(User.class);
        Mockito.when(user.getAccountId()).thenReturn(1L);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountMgr.getAccount(1L)).thenReturn(account);

        ReflectionTestUtils.invokeMethod(apiServer, "checkCommandAvailable", user, "listVirtualMachines", InetAddress.getByName("127.0.0.1"));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckCommandAvailableThrowsWhenUserNull() throws Exception {
        ReflectionTestUtils.invokeMethod(apiServer, "checkCommandAvailable", null, "listVirtualMachines", InetAddress.getByName("127.0.0.1"));
    }
}
