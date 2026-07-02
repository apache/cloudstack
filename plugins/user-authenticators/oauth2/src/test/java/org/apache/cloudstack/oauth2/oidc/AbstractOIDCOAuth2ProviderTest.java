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
package org.apache.cloudstack.oauth2.oidc;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;

public class AbstractOIDCOAuth2ProviderTest {

    private static final String PROVIDER_NAME = "oidc-test";

    @Mock
    private OauthProviderDao oauthProviderDao;

    @Mock
    private CloseableHttpClient httpClient;

    private TestOIDCProvider provider;

    private OauthProviderVO mockProviderVO;

    private static class TestOIDCProvider extends AbstractOIDCOAuth2Provider {
        TestOIDCProvider(CloseableHttpClient httpClient) {
            super(httpClient);
        }

        @Override
        public String getName() {
            return PROVIDER_NAME;
        }

        @Override
        public String getDescription() {
            return "Test OIDC Provider Plugin";
        }
    }

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        provider = new TestOIDCProvider(httpClient);
        provider.oauthProviderDao = oauthProviderDao;

        mockProviderVO = new OauthProviderVO();
        mockProviderVO.setClientId("test-client");
        mockProviderVO.setSecretKey("test-secret");
        mockProviderVO.setTokenUrl("http://localhost/token");
        mockProviderVO.setRedirectUri("http://localhost/redirect");
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private CloseableHttpResponse mockTokenResponse(int statusCode, String body) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);

        return response;
    }

    private String idTokenWith(String audience, String email) {
        String header = "{\"alg\":\"none\"}";
        String payload = "{" +
                "\"aud\":[\"" + audience + "\"]," +
                "\"email\":\"" + email + "\"," +
                "\"iss\":\"http://oidc\"," +
                "\"sub\":\"12345\"" +
                "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        return encodedHeader + "." + encodedPayload + ".not-checked-signature";
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserEmptyParams() {
        provider.verifyUser("", "");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserProviderNotFound() {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(null);
        provider.verifyUser("test@example.com", "code123");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyCodeAndFetchEmailHttpError() throws IOException {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(mockProviderVO);
        CloseableHttpResponse response = mockTokenResponse(400, "error");
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        provider.verifyCodeAndFetchEmail("invalid-code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyCodeAndFetchEmailNetworkFailure() throws IOException {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(mockProviderVO);
        when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("Connection refused"));

        provider.verifyCodeAndFetchEmail("code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedEmail() throws IOException {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(mockProviderVO);

        String jsonResponseBody = "{\"id_token\":\"" + idTokenWith("test-client", "anotheruser@example.com") + "\", \"access_token\":\"acc-123\"}";
        CloseableHttpResponse response = mockTokenResponse(200, jsonResponseBody);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        provider.verifyUser("user@example.com", "valid-auth-code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedClient() throws IOException {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(mockProviderVO);

        String jsonResponseBody = "{\"id_token\":\"" + idTokenWith("anothertest-client", "anotheruser@example.com") + "\", \"access_token\":\"acc-123\"}";
        CloseableHttpResponse response = mockTokenResponse(200, jsonResponseBody);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        provider.verifyUser("anotheruser@example.com", "valid-auth-code");
    }

    @Test
    public void testVerifyUserEmail() throws IOException {
        when(oauthProviderDao.findByProvider(PROVIDER_NAME)).thenReturn(mockProviderVO);

        String testEmail = "user@example.com";
        String jsonResponseBody = "{\"id_token\":\"" + idTokenWith("test-client", testEmail) + "\", \"access_token\":\"acc-123\"}";
        CloseableHttpResponse response = mockTokenResponse(200, jsonResponseBody);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        boolean result = provider.verifyUser(testEmail, "valid-auth-code");

        assertTrue("User successfully verified", result);
    }
}
