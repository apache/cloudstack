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
package org.apache.cloudstack.oauth2.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;

public class KeycloakOAuth2ProviderTest {

    @Mock
    private OauthProviderDao oauthProviderDao;

    @Mock
    private CloseableHttpClient httpClient;

    private KeycloakOAuth2Provider provider;

    private OauthProviderVO mockProviderVO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        provider = new KeycloakOAuth2Provider(httpClient);
        provider.oauthProviderDao = oauthProviderDao;

        mockProviderVO = new OauthProviderVO();
        mockProviderVO.setClientId("test-client");
        mockProviderVO.setSecretKey("test-secret");
        mockProviderVO.setTokenUrl("http://localhost/token");
        mockProviderVO.setRedirectUri("http://localhost/redirect");
    }

    @Test
    public void testGetName() {
        assertEquals("keycloak", provider.getName());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserEmptyParams() {
        provider.verifyUser("", "");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserProviderNotFound() {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(null);
        provider.verifyUser("test@example.com", "code123");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyCodeAndFetchEmailHttpError() throws IOException {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(mockProviderVO);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(statusLine.getStatusCode()).thenReturn(400);
        when(response.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("error".getBytes()));
        when(response.getEntity()).thenReturn(entity);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        provider.verifyCodeAndFetchEmail("invalid-code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyCodeAndFetchEmailNetworkFailure() throws IOException {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(mockProviderVO);
        when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("Connexion refusée"));

        provider.verifyCodeAndFetchEmail("code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedEmail() throws IOException {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(mockProviderVO);

        String testEmail = "anotheruser@example.com";
        String secretCode = "valid-auth-code";

        String header = "{\"alg\":\"none\"}";
        String payload = "{" +
                "\"aud\":[\"test-client\"]," +
                "\"email\":\"" + testEmail + "\"," +
                "\"iss\":\"http://keycloak\"," +
                "\"sub\":\"12345\"" +
                "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String fakeJwt = encodedHeader + "." + encodedPayload + ".not-checked-signature";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);

        String jsonResponseBody = "{\"id_token\":\"" + fakeJwt + "\", \"access_token\":\"acc-123\"}";
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponseBody.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        boolean result = provider.verifyUser("user@example.com", secretCode);

        assertTrue("L'utilisateur devrait être vérifié avec succès", result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedClient() throws IOException {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(mockProviderVO);

        String testEmail = "anotheruser@example.com";
        String secretCode = "valid-auth-code";

        String header = "{\"alg\":\"none\"}";
        String payload = "{" +
                "\"aud\":[\"anothertest-client\"]," +
                "\"email\":\"" + testEmail + "\"," +
                "\"iss\":\"http://keycloak\"," +
                "\"sub\":\"12345\"" +
                "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String fakeJwt = encodedHeader + "." + encodedPayload + ".not-checked-signature";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);

        String jsonResponseBody = "{\"id_token\":\"" + fakeJwt + "\", \"access_token\":\"acc-123\"}";
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponseBody.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        boolean result = provider.verifyUser(testEmail, secretCode);

        assertTrue("L'utilisateur devrait être vérifié avec succès", result);
    }

    @Test
    public void testVerifyUserEmail() throws IOException {
        when(oauthProviderDao.findByProvider("keycloak")).thenReturn(mockProviderVO);

        String testEmail = "user@example.com";
        String secretCode = "valid-auth-code";

        String header = "{\"alg\":\"none\"}";
        String payload = "{" +
                "\"aud\":[\"test-client\"]," +
                "\"email\":\"" + testEmail + "\"," +
                "\"iss\":\"http://keycloak\"," +
                "\"sub\":\"12345\"" +
                "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String fakeJwt = encodedHeader + "." + encodedPayload + ".not-checked-signature";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);

        String jsonResponseBody = "{\"id_token\":\"" + fakeJwt + "\", \"access_token\":\"acc-123\"}";
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponseBody.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        boolean result = provider.verifyUser(testEmail, secretCode);

        assertTrue("L'utilisateur devrait être vérifié avec succès", result);
    }

    @Test
    public void testGetDescription() {
        assertEquals("Keycloak OAuth2 Provider Plugin", provider.getDescription());
    }
}