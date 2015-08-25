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

package com.cloud.network.nicira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.rest.CloudstackRESTException;
import com.cloud.utils.rest.HttpMethods;
import com.cloud.utils.rest.HttpRequestMatcher;
import com.cloud.utils.rest.HttpUriRequestBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NiciraRestClient.class)
public class NiciraRestClientTest {

    private static final int HTTPS_PORT = 443;

    private static final String HTTPS = "HTTPS";
    private static final String LOGIN_PATH = "/login";
    private static final String LOCALHOST = "localhost";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "adminpassword";

    private static final HttpHost HTTP_HOST = new HttpHost(LOCALHOST, HTTPS_PORT, HTTPS);

    private static final StatusLine HTTP_200_STATUSLINE = new BasicStatusLine(new ProtocolVersion(HTTPS, 1, 1), 200, "OK");
    private static final StatusLine HTTP_401_STATUSLINE = new BasicStatusLine(new ProtocolVersion(HTTPS, 1, 1), 401, "Unauthorized");

    private static final Map<String, String> loginParameters = new HashMap<String, String>();
    private static HttpUriRequest request;
    private static HttpUriRequest loginRequest;
    private final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private final CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
    private HttpClientContext httpClientContext;
    private NiciraRestClient client;

    @BeforeClass
    public static void setupClass() {
        loginParameters.put("username", ADMIN);
        loginParameters.put("password", ADMIN_PASSWORD);
        request = HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("/path")
            .build();
        loginRequest = HttpUriRequestBuilder.create()
            .method(HttpMethods.POST)
            .methodParameters(loginParameters)
            .path(LOGIN_PATH)
            .build();
    }

    @Before
    public void setup() {
        httpClientContext = HttpClientContext.create();
        client = spy(NiciraRestClient.create()
            .client(httpClient)
            .clientContext(httpClientContext)
            .hostname(LOCALHOST)
            .username(ADMIN)
            .password(ADMIN_PASSWORD)
            .loginUrl(LOGIN_PATH)
            .executionLimit(5)
            .build());
    }

    @Test
    public void testExecuteSuccessAtFirstAttempt() throws Exception {
        when(mockResponse.getStatusLine()).thenReturn(HTTP_200_STATUSLINE);
        when(httpClient.execute(eq(HTTP_HOST), eq(request), eq(httpClientContext))).thenReturn(mockResponse);

        final CloseableHttpResponse response = client.execute(request);

        assertThat(response, notNullValue());
        assertThat(response, sameInstance(mockResponse));
        verifyPrivate(client).invoke("execute", request, 0);
    }

    @Test
    public void testExecuteUnauthorizedThenSuccess() throws Exception {
        when(mockResponse.getStatusLine())
            .thenReturn(HTTP_401_STATUSLINE)
            .thenReturn(HTTP_200_STATUSLINE)
            .thenReturn(HTTP_200_STATUSLINE);
        when(httpClient.execute(eq(HTTP_HOST), HttpRequestMatcher.eq(request), eq(httpClientContext)))
            .thenReturn(mockResponse)
            .thenReturn(mockResponse);
        when(httpClient.execute(eq(HTTP_HOST), HttpRequestMatcher.eq(loginRequest), eq(httpClientContext)))
            .thenReturn(mockResponse);

        final CloseableHttpResponse response = client.execute(request);

        assertThat(response, notNullValue());
        assertThat(response, sameInstance(mockResponse));
        verifyPrivate(client).invoke("execute", HttpRequestMatcher.eq(request), eq(0));
        verifyPrivate(client).invoke("execute", HttpRequestMatcher.eq(loginRequest), eq(401));
        verifyPrivate(client).invoke("execute", HttpRequestMatcher.eq(request), eq(200));
    }

    @Test
    public void testExecuteTwoConsecutiveUnauthorizedExecutions() throws Exception {
        when(mockResponse.getStatusLine())
            .thenReturn(HTTP_401_STATUSLINE)
            .thenReturn(HTTP_401_STATUSLINE);
        when(httpClient.execute(eq(HTTP_HOST), HttpRequestMatcher.eq(request), eq(httpClientContext)))
            .thenReturn(mockResponse);
        when(httpClient.execute(eq(HTTP_HOST), HttpRequestMatcher.eq(loginRequest), eq(httpClientContext)))
            .thenReturn(mockResponse);
        final NiciraRestClient client = spy(NiciraRestClient.create()
            .client(httpClient)
            .clientContext(httpClientContext)
            .hostname(LOCALHOST)
            .username(ADMIN)
            .password(ADMIN_PASSWORD)
            .loginUrl(LOGIN_PATH)
            .executionLimit(2)
            .build());

        try {
            client.execute(request);
            fail("Expected CloudstackRESTException exception");
        } catch (final CloudstackRESTException e) {
            assertThat(e.getMessage(), not(isEmptyOrNullString()));
            verifyPrivate(client).invoke("execute", HttpRequestMatcher.eq(request), eq(0));
            verifyPrivate(client).invoke("execute", HttpRequestMatcher.eq(loginRequest), eq(401));
        }
    }
}
