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
import com.cloud.user.UserAccount;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.user.PasswordResetManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetToken;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenExpiryDate;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    @Mock
    PasswordResetManager passwordResetManager;

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
    public void testForgotPasswordSuccessFirstRequest() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Collections.emptyMap());
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Active);
        Mockito.doNothing().when(passwordResetManager).setResetTokenAndSend(userAccount);
        Assert.assertTrue(apiServer.forgotPassword(userAccount, domain));
        Mockito.verify(passwordResetManager).setResetTokenAndSend(userAccount);
    }

    @Test
    public void testForgotPasswordSuccessSecondRequestExpired() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Map.of(
                PasswordResetToken, "token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() - 5 * 60 * 1000)
        ));
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.doNothing().when(passwordResetManager).setResetTokenAndSend(userAccount);
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Active);
        Assert.assertTrue(apiServer.forgotPassword(userAccount, domain));
        Mockito.verify(passwordResetManager).setResetTokenAndSend(userAccount);
    }

    @Test
    public void testForgotPasswordSuccessSecondRequestUnexpired() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Map.of(
                PasswordResetToken, "token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000)
        ));
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Active);
        Assert.assertTrue(apiServer.forgotPassword(userAccount, domain));
        Mockito.verify(passwordResetManager, Mockito.never()).setResetTokenAndSend(userAccount);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureNoEmail() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Collections.emptyMap());
        Mockito.when(userAccount.getEmail()).thenReturn("");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledUser() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Map.of(
                PasswordResetToken, "token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000)
        ));
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureDisabledAccount() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Map.of(
                PasswordResetToken, "token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000)
        ));
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("DISABLED");
        apiServer.forgotPassword(userAccount, domain);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testForgotPasswordFailureInactiveDomain() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Domain domain = Mockito.mock(Domain.class);

        Mockito.when(userAccount.getDetails()).thenReturn(Map.of(
                PasswordResetToken, "token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000)
        ));
        Mockito.when(userAccount.getEmail()).thenReturn("test@test.com");
        Mockito.when(userAccount.getState()).thenReturn("ENABLED");
        Mockito.when(userAccount.getAccountState()).thenReturn("ENABLED");
        Mockito.when(domain.getState()).thenReturn(Domain.State.Inactive);
        apiServer.forgotPassword(userAccount, domain);
    }
}
