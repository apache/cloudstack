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
package org.apache.cloudstack.saml;

import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SAML2UserAuthenticatorTest {

    @Mock
    UserAccountDao userAccountDao;

    @Mock
    UserDao userDao;

    @InjectMocks
    SAML2UserAuthenticator authenticator = new SAML2UserAuthenticator();

    private static final String USERNAME = "saml-user";
    private static final Long DOMAIN_ID = 1L;
    private static final Long USER_ID = 5L;

    @Before
    @After
    public void clearThreadFlag() {
        SAML2UserAuthenticator.clearAssertionValidated();
    }

    private void mockSaml2User() {
        UserAccount account = Mockito.mock(UserAccount.class);
        Mockito.when(account.getSource()).thenReturn(User.Source.SAML2);
        Mockito.when(account.getId()).thenReturn(USER_ID);
        Mockito.when(userAccountDao.getUserAccount(USERNAME, DOMAIN_ID)).thenReturn(account);

        UserVO user = Mockito.mock(UserVO.class);
        Mockito.when(user.getSource()).thenReturn(User.Source.SAML2);
        Mockito.when(user.getExternalEntity()).thenReturn("https://idp.example.com");
        Mockito.when(userDao.getUser(USER_ID)).thenReturn(user);
    }

    // Legitimate SAML flow: the caller marked the thread after validating the assertion.
    @Test
    public void testAuthenticateSucceedsWhenAssertionValidated() {
        mockSaml2User();
        SAML2UserAuthenticator.markAssertionValidated();
        Pair<Boolean, ?> result = authenticator.authenticate(USERNAME, "ignored-password", DOMAIN_ID, null);
        Assert.assertTrue("SAML2 user must authenticate when a validated SAML flow drives the login", result.first());
    }

    // No SAML flow marked the thread (e.g. command=login). The fail-open must not trigger.
    @Test
    public void testAuthenticateFailsWhenNotDrivenBySamlFlow() {
        mockSaml2User();
        Pair<Boolean, ?> result = authenticator.authenticate(USERNAME, "ignored-password", DOMAIN_ID, null);
        Assert.assertFalse("SAML2 user must not authenticate without a validated SAML flow", result.first());
    }

    // The flag is single-shot: once cleared, a later login on the same thread is denied.
    @Test
    public void testAuthenticateFailsAfterFlagCleared() {
        mockSaml2User();
        SAML2UserAuthenticator.markAssertionValidated();
        SAML2UserAuthenticator.clearAssertionValidated();
        Pair<Boolean, ?> result = authenticator.authenticate(USERNAME, "ignored-password", DOMAIN_ID, null);
        Assert.assertFalse("Cleared flag must not authenticate", result.first());
    }

    // Empty password is rejected before any lookup, regardless of the thread flag.
    @Test
    public void testAuthenticateFailsOnEmptyPassword() {
        SAML2UserAuthenticator.markAssertionValidated();
        Pair<Boolean, ?> result = authenticator.authenticate(USERNAME, "", DOMAIN_ID, null);
        Assert.assertFalse("Empty password must be rejected", result.first());
    }
}
