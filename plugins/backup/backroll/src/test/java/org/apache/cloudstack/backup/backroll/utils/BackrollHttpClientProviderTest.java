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
package org.apache.cloudstack.backup.backroll.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.cloudstack.backup.backroll.model.response.api.LoginApiResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClientProvider.NotOkBodyException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BackrollHttpClientProviderTest {
    @Spy
    @InjectMocks
    BackrollHttpClientProvider backupHttpClientProvider;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private RequestConfig config;

    @Spy
    private SSLUtils sslUtils;

    @Spy
    private HttpClientBuilder httpClientBuilder;

    @Mock
    private SSLContext sslContext;

    @Mock
    private SSLConnectionSocketFactory sslConnectionSocketFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        backupHttpClientProvider = BackrollHttpClientProvider.createProvider(backupHttpClientProvider,
                "http://api.backup.demo.ccc:5050/api/v1", "backroll-api", "VviX8dALauSyYJMqVYJqf3UyZOpO3joS", true,
                300, 600);
    }

    private void defaultTestHttpClient(String path)
            throws BackrollApiException, ClientProtocolException, IOException, NotOkBodyException {

        LoginApiResponse responseLogin = new LoginApiResponse();
        responseLogin.accessToken = "dummyToken";
        responseLogin.expiresIn = 3600;
        responseLogin.notBeforePolicy = "dummyNotBeforePolicy";
        responseLogin.refreshExpiresIn = "7200";
        responseLogin.scope = "dummyScope";

        String virtualMachineResponseString = "{ \"state\": \"SUCCESS\", \"info\": { \"archives\": [ { \"archive\": \"ROOT-00000\", \"barchive\": \"ROOT-00000\", \"id\": \"25d55ad283aa400af464c76d713c07ad7d163abdd3b8fbcdbdc46b827e5e0457\", \"name\": \"ROOT-00000\", \"start\": \"2024-11-08T18:24:48.000000\", \"time\": \"2024-11-08T18:24:48.000000\" } ], \"encryption\": { \"mode\": \"none\" }, \"repository\": { \"id\": \"36a11ebc0775a097c927735cc7015d19be7309be69fc15b896c5b1fd87fcbd79\", \"last_modified\": \"2024-11-29T09:53:09.000000\", \"location\": \"/mnt/backup/backup1\" } } }";

        CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);

        StatusLine statusLine = mock(StatusLine.class);

        doReturn(httpClient).when(backupHttpClientProvider).createHttpClient();

        doReturn(response).when(httpClient)
                .execute(argThat(argument -> argument != null && argument.getURI().toString().contains("login")));

        doReturn(response2).when(httpClient)
                .execute(argThat(argument -> argument != null && argument.getURI().toString().contains(path)));

        doReturn(new ObjectMapper().writeValueAsString(responseLogin)).when(backupHttpClientProvider).okBody(response);
        doReturn(virtualMachineResponseString).when(backupHttpClientProvider).okBody(response2);

        doReturn(statusLine).when(response).getStatusLine();
        doReturn(HttpStatus.SC_OK).when(statusLine).getStatusCode();

        doNothing().when(response).close();

        doReturn(new StringEntity("{\"mockKey\": \"mockValue\"}", ContentType.APPLICATION_JSON)).when(response)
                .getEntity();
    }

    @Test
    public void testCreateHttpClient_WithValidateCertificateTrue()
            throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, BackrollApiException {
        backupHttpClientProvider = BackrollHttpClientProvider.createProvider(backupHttpClientProvider,
                "http://api.backup.demo.ccc:5050/api/v1", "backroll-api", "VviX8dALauSyYJMqVYJqf3UyZOpO3joS", false,
                300, 600);

        // Mock HttpClientBuilder
        HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
        try (MockedStatic<HttpClientBuilder> utilities = Mockito.mockStatic(HttpClientBuilder.class)) {
            utilities.when(HttpClientBuilder::create).thenReturn(mockBuilder);
            when(mockBuilder.setDefaultRequestConfig(config)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(httpClient);
        }



        // Test the method
        CloseableHttpClient client = backupHttpClientProvider.createHttpClient();

        // Verify and assert
        //verify(mockBuilder).setDefaultRequestConfig(config);
        assertNotNull(client);
        //assertTrue(client.getClass() == CloseableHttpClient.class);
        //assertEquals(mockHttpClient, client);
    }
    @Test
    public void NotOkBodyException_Test(){
        BackrollHttpClientProvider.NotOkBodyException exception = backupHttpClientProvider.new NotOkBodyException();
        assertNotNull(exception);
    }

    @Test
    public void get_Test_success() throws Exception, BackrollApiException, IOException {
        // Arrange
        String path = "/test";
        defaultTestHttpClient(path);

        // Act
        VirtualMachineBackupsResponse result = backupHttpClientProvider.get(path,
                VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClientProvider, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
        verify(response, times(1)).close();
    }

    @Test
    public void delete_Test_success() throws Exception, BackrollApiException, IOException {
        // Arrange
        String path = "/test";
        defaultTestHttpClient(path);

        // Act
        VirtualMachineBackupsResponse result = backupHttpClientProvider.delete(path,
                VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClientProvider, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpDelete.class));
        verify(response, times(1)).close();
    }

    @Test
    public void okBody_Test() throws BackrollApiException, IOException, NotOkBodyException {

        StatusLine statusLine = mock(StatusLine.class);
        doReturn(statusLine).when(response).getStatusLine();
        doReturn(HttpStatus.SC_OK).when(statusLine).getStatusCode();
        doReturn(new StringEntity("{\"mockKey\": \"mockValue\"}", ContentType.APPLICATION_JSON)).when(response)
                .getEntity();
        doNothing().when(response).close();
        String result = backupHttpClientProvider.okBody(response);
        assertNotNull(result);

    }

    @Test
    public void waitGet_Test() throws Exception, BackrollApiException, IOException {
        String path = "/test";
        defaultTestHttpClient(path);
        doReturn(response).when(httpClient)
                .execute(argThat(argument -> argument != null && argument.getURI().toString().contains("/auth")));

        // Act
        VirtualMachineBackupsResponse result = backupHttpClientProvider.waitGet(path,
                VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClientProvider, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
        verify(response, times(1)).close();
    }

    @Test
    public void waitGetWithoutParseResponse_Test() throws Exception, BackrollApiException, IOException {
        String path = "/test";
        defaultTestHttpClient(path);
        doReturn(response).when(httpClient)
                .execute(argThat(argument -> argument != null && argument.getURI().toString().contains("/auth")));

        // Act
        String result = backupHttpClientProvider.waitGetWithoutParseResponse(path);

        // Assert
        assertNotNull(result);
        verify(backupHttpClientProvider, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
        verify(response, times(1)).close();
    }

    @Test
    public void testPost_success() throws Exception, BackrollApiException, IOException {
        // Arrange
        String path = "/test";
        JSONObject json = new JSONObject();

        defaultTestHttpClient(path);
        // Act
        VirtualMachineBackupsResponse result = backupHttpClientProvider.post(path, json,
                VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClientProvider, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(2)).execute(Mockito.any(HttpPost.class));
        verify(response, times(1)).close();
    }

}
