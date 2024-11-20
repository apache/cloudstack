//
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
//
package org.apache.cloudstack.oauth2;

import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class OAuth2UserAuthenticatorTest {

    @Mock
    private UserAccountDao userAccountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private OAuth2AuthManager userOAuth2mgr;

    @InjectMocks
    @Spy
    private OAuth2UserAuthenticator authenticator;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        doReturn(true).when(authenticator).isOAuthPluginEnabled();
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }


    @Test
    public void testAuthenticateWithValidCredentials() {
        String username = "testuser";
        Long domainId = 1L;
        String[] provider = {"testprovider"};
        String[] email = {"testemail"};
        String[] secretCode = {"testsecretcode"};

        UserAccount userAccount = mock(UserAccount.class);
        UserVO user = mock(UserVO.class);
        UserOAuth2Authenticator userOAuth2Authenticator = mock(UserOAuth2Authenticator.class);

        when(userAccountDao.getUserAccount(username, domainId)).thenReturn(userAccount);
        when(userDao.getUser(userAccount.getId())).thenReturn(user);
        when(userOAuth2mgr.getUserOAuth2AuthenticationProvider(provider[0])).thenReturn(userOAuth2Authenticator);
        when(userOAuth2Authenticator.verifyUser(email[0], secretCode[0])).thenReturn(true);

        Map<String, Object[]> requestParameters = new HashMap<>();
        requestParameters.put("provider", provider);
        requestParameters.put("email", email);
        requestParameters.put("secretcode", secretCode);

        Pair<Boolean, OAuth2UserAuthenticator.ActionOnFailedAuthentication> result = authenticator.authenticate(username, null, domainId, requestParameters);

        assertTrue(result.first());
        assertNull(result.second());

        verify(userAccountDao).getUserAccount(username, domainId);
        verify(userDao).getUser(userAccount.getId());
        verify(userOAuth2mgr).getUserOAuth2AuthenticationProvider(provider[0]);
        verify(userOAuth2Authenticator).verifyUser(email[0], secretCode[0]);
    }

    @Test
    public void testAuthenticateWithInvalidCredentials() {
        String username = "testuser";
        Long domainId = 1L;
        String[] provider = {"testprovider"};
        String[] email = {"testemail"};
        String[] secretCode = {"testsecretcode"};

        UserAccount userAccount = mock(UserAccount.class);
        UserVO user = mock(UserVO.class);
        UserOAuth2Authenticator userOAuth2Authenticator = mock(UserOAuth2Authenticator.class);

        when(userAccountDao.getUserAccount(username, domainId)).thenReturn(userAccount);
        when(userDao.getUser(userAccount.getId())).thenReturn(user);
        when(userOAuth2mgr.getUserOAuth2AuthenticationProvider(provider[0])).thenReturn(userOAuth2Authenticator);
        when(userOAuth2Authenticator.verifyUser(email[0], secretCode[0])).thenReturn(false);

        Map<String, Object[]> requestParameters = new HashMap<>();
        requestParameters.put("provider", provider);
        requestParameters.put("email", email);
        requestParameters.put("secretcode", secretCode);

        Pair<Boolean, OAuth2UserAuthenticator.ActionOnFailedAuthentication> result = authenticator.authenticate(username, null, domainId, requestParameters);

        assertFalse(result.first());
        assertEquals(OAuth2UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT, result.second());

        verify(userAccountDao).getUserAccount(username, domainId);
        verify(userDao).getUser(userAccount.getId());
        verify(userOAuth2mgr).getUserOAuth2AuthenticationProvider(provider[0]);
        verify(userOAuth2Authenticator).verifyUser(email[0], secretCode[0]);
    }

    @Test
    public void testAuthenticateWithInvalidUserAccount() {
        String username = "testuser";
        Long domainId = 1L;
        String[] provider = {"testprovider"};
        String[] email = {"testemail"};
        String[] secretCode = {"testsecretcode"};

        when(userAccountDao.getUserAccount(username, domainId)).thenReturn(null);

        Map<String, Object[]> requestParameters = new HashMap<>();
        requestParameters.put("provider", provider);
        requestParameters.put("email", email);
        requestParameters.put("secretcode", secretCode);

        Pair<Boolean, OAuth2UserAuthenticator.ActionOnFailedAuthentication> result = authenticator.authenticate(username, null, domainId, requestParameters);

        assertFalse(result.first());
        assertNull(result.second());

        verify(userAccountDao).getUserAccount(username, domainId);
        verify(userDao, never()).getUser(anyLong());
        verify(userOAuth2mgr, never()).getUserOAuth2AuthenticationProvider(anyString());
    }
}
