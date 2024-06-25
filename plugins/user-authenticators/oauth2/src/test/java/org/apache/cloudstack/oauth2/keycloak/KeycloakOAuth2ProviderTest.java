// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.oauth2.keycloak;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class KeycloakOAuth2ProviderTest {

    @Mock
    private OauthProviderDao _oauthProviderDao;

    @Spy
    @InjectMocks
    private KeycloakOAuth2Provider _keycloakOAuth2Provider;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullEmail() {
        _keycloakOAuth2Provider.verifyUser(null, "secretCode");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullSecretCode() {
        _keycloakOAuth2Provider.verifyUser("email@example.com", null);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithUnregisteredProvider() {
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(null);
        _keycloakOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithInvalidSecretCode() {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getClientId()).thenReturn("testClientId");
        when(providerVO.getSecretKey()).thenReturn("testSecretKey");
        when(providerVO.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");
        when(providerVO.getRealm()).thenReturn("testRealm");

        Keycloak keycloak = mock(Keycloak.class);
        when(Keycloak.getInstance(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(keycloak);

        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(keycloak.tokenManager().getAccessToken()).thenReturn(tokenResponse);

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);

        UserResource userResource = mock(UserResource.class);
        when(realmResource.users().get(anyString())).thenReturn(userResource);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", null);
        when(userResource.toRepresentation().getAttributes()).thenReturn(attributes);

        _keycloakOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedEmail() {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getClientId()).thenReturn("testClientId");
        when(providerVO.getSecretKey()).thenReturn("testSecretKey");
        when(providerVO.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");
        when(providerVO.getRealm()).thenReturn("testRealm");

        Keycloak keycloak = mock(Keycloak.class);
        when(Keycloak.getInstance(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(keycloak);

        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(keycloak.tokenManager().getAccessToken()).thenReturn(tokenResponse);

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);

        UserResource userResource = mock(UserResource.class);
        when(realmResource.users().get(anyString())).thenReturn(userResource);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "otheremail@example.com");
        when(userResource.toRepresentation().getAttributes()).thenReturn(attributes);

        _keycloakOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test
    public void testVerifyUserEmail() {
        OauthProviderVO providerVO = mock(OauthProviderVO.class);
        when(_oauthProviderDao.findByProvider(anyString())).thenReturn(providerVO);
        when(providerVO.getClientId()).thenReturn("testClientId");
        when(providerVO.getSecretKey()).thenReturn("testSecretKey");
        when(providerVO.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");
        when(providerVO.getRealm()).thenReturn("testRealm");

        Keycloak keycloak = mock(Keycloak.class);
        when(Keycloak.getInstance(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(keycloak);

        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(keycloak.tokenManager().getAccessToken()).thenReturn(tokenResponse);

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);

        UserResource userResource = mock(UserResource.class);
        when(realmResource.users().get(anyString())).thenReturn(userResource);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "email@example.com");
        when(userResource.toRepresentation().getAttributes()).thenReturn(attributes);

        boolean result = _keycloakOAuth2Provider.verifyUser("email@example.com", "secretCode");

        assertTrue(result);
        assertNull(_keycloakOAuth2Provider.accessToken);
    }
}
