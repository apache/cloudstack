//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.oauth2.google;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoogleOAuth2ProviderTest {

    @Mock
    private OauthProviderDao _oauthProviderDao;

    @Spy
    @InjectMocks
    private GoogleOAuth2Provider _googleOAuth2Provider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullEmail() {
        _googleOAuth2Provider.verifyUser(null, "secretCode");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullSecretCode() {
        _googleOAuth2Provider.verifyUser("email@example.com", null);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithUnregisteredProvider() {
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(null);
        _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithInvalidSecretCode() throws IOException {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getProvider()).thenReturn("testProvider");
        when(providerVO.getSecretKey()).thenReturn("testSecret");
        when(providerVO.getClientId()).thenReturn("testClientid");
        _googleOAuth2Provider.accessToken = "testAccessToken";
        _googleOAuth2Provider.refreshToken = "testRefreshToken";
        Oauth2 oauth2 = mock(Oauth2.class);
        try (MockedConstruction<Oauth2.Builder> ignored = Mockito.mockConstruction(Oauth2.Builder.class,
                (mock, context) -> when(mock.build()).thenReturn(oauth2))) {
            Userinfo userinfo = mock(Userinfo.class);
            Oauth2.Userinfo userinfo1 = mock(Oauth2.Userinfo.class);
            when(oauth2.userinfo()).thenReturn(userinfo1);
            Oauth2.Userinfo.Get userinfoGet = mock(Oauth2.Userinfo.Get.class);
            when(userinfo1.get()).thenReturn(userinfoGet);
            when(userinfoGet.execute()).thenReturn(userinfo);
            when(userinfo.getEmail()).thenReturn(null);

            _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedEmail() throws IOException {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getProvider()).thenReturn("testProvider");
        when(providerVO.getSecretKey()).thenReturn("testSecret");
        when(providerVO.getClientId()).thenReturn("testClientid");
        _googleOAuth2Provider.accessToken = "testAccessToken";
        _googleOAuth2Provider.refreshToken = "testRefreshToken";
        Oauth2 oauth2 = mock(Oauth2.class);
        try (MockedConstruction<Oauth2.Builder> ignored = Mockito.mockConstruction(Oauth2.Builder.class,
                (mock, context) -> when(mock.build()).thenReturn(oauth2))) {
            Userinfo userinfo = mock(Userinfo.class);
            Oauth2.Userinfo userinfo1 = mock(Oauth2.Userinfo.class);
            when(oauth2.userinfo()).thenReturn(userinfo1);
            Oauth2.Userinfo.Get userinfoGet = mock(Oauth2.Userinfo.Get.class);
            when(userinfo1.get()).thenReturn(userinfoGet);
            when(userinfoGet.execute()).thenReturn(userinfo);
            when(userinfo.getEmail()).thenReturn("otheremail@example.com");

            _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
        }
    }

    @Test
    public void testVerifyUserEmail() throws IOException {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getProvider()).thenReturn("testProvider");
        when(providerVO.getSecretKey()).thenReturn("testSecret");
        when(providerVO.getClientId()).thenReturn("testClientid");
        _googleOAuth2Provider.accessToken = "testAccessToken";
        _googleOAuth2Provider.refreshToken = "testRefreshToken";
        Oauth2 oauth2 = mock(Oauth2.class);
        try (MockedConstruction<Oauth2.Builder> ignored = Mockito.mockConstruction(Oauth2.Builder.class,
                (mock, context) -> when(mock.build()).thenReturn(oauth2))) {
            Userinfo userinfo = mock(Userinfo.class);
            Oauth2.Userinfo userinfo1 = mock(Oauth2.Userinfo.class);
            when(oauth2.userinfo()).thenReturn(userinfo1);
            Oauth2.Userinfo.Get userinfoGet = mock(Oauth2.Userinfo.Get.class);
            when(userinfo1.get()).thenReturn(userinfoGet);
            when(userinfoGet.execute()).thenReturn(userinfo);
            when(userinfo.getEmail()).thenReturn("email@example.com");

            boolean result = _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");

            assertTrue(result);
            assertNull(_googleOAuth2Provider.accessToken);
            assertNull(_googleOAuth2Provider.refreshToken);
        }
    }
}
