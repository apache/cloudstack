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

package org.apache.cloudstack.oauth2.github;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GithubOAuth2ProviderTest {

    private GithubOAuth2Provider githubOAuth2Provider;

    @Mock
    private HttpURLConnection connection;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        githubOAuth2Provider = new GithubOAuth2Provider();
    }

    @Test
    public void testGetAccessToken() throws Exception {
        String secretCode = "secretCode";
        String accessToken = "accessToken";
        String clientId = "clientId";
        String secretKey = "secretKey";
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String jsonParams = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + secretKey + "\",\"code\":\"" + secretCode + "\"}";
        String response = "access_token=" + accessToken;

        OauthProviderVO oauthProviderVO = new OauthProviderVO();
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setSecretKey(secretKey);

        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));

        URL url = new URL(tokenUrl);
        when(url.openConnection()).thenReturn(connection);

        OauthProviderDao oauthProviderDao = mock(OauthProviderDao.class);
        when(oauthProviderDao.findByProvider(githubOAuth2Provider.getName())).thenReturn(oauthProviderVO);

        githubOAuth2Provider._oauthProviderDao = oauthProviderDao;

        String result = githubOAuth2Provider.getAccessToken(secretCode);

        assertEquals(accessToken, result);

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty("Content-Type", "application/json");
        verify(connection).setDoOutput(true);

        verify(connection.getOutputStream()).write(jsonParams.getBytes("utf-8"));

        verify(connection).getResponseCode();

        verify(connection).getInputStream();

        verify(connection).disconnect();
    }

    @Test
    public void testGetAccessTokenWithInvalidCode() throws Exception {
        String secretCode = "secretCode";
        String clientId = "clientId";
        String secretKey = "secretKey";
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String jsonParams = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + secretKey + "\",\"code\":\"" + secretCode + "\"}";
        String response = "invalid_response";

        OauthProviderVO oauthProviderVO = new OauthProviderVO();
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setSecretKey(secretKey);

        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));

        URL url = mock(URL.class);
        when(url.openConnection()).thenReturn(connection);

        OauthProviderDao oauthProviderDao = mock(OauthProviderDao.class);
        when(oauthProviderDao.findByProvider(githubOAuth2Provider.getName())).thenReturn(oauthProviderVO);

        githubOAuth2Provider._oauthProviderDao = oauthProviderDao;

        assertThrows(CloudRuntimeException.class, () -> {
            githubOAuth2Provider.getAccessToken(secretCode);
        });

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty("Content-Type", "application/json");
        verify(connection).setDoOutput(true);

        verify(connection.getOutputStream()).write(jsonParams.getBytes("utf-8"));

        verify(connection).getResponseCode();

        verify(connection).getInputStream();

        verify(connection).disconnect();
    }

    @Test
    public void testGetAccessTokenWithHttpError() throws Exception {
        String secretCode = "secretCode";
        String clientId = "clientId";
        String secretKey = "secretKey";
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String jsonParams = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + secretKey + "\",\"code\":\"" + secretCode + "\"}";

        OauthProviderVO oauthProviderVO = new OauthProviderVO();
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setSecretKey(secretKey);

        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        URL url = new URL(tokenUrl);
        when(url.openConnection()).thenReturn(connection);

        OauthProviderDao oauthProviderDao = mock(OauthProviderDao.class);
        when(oauthProviderDao.findByProvider(githubOAuth2Provider.getName())).thenReturn(oauthProviderVO);

        githubOAuth2Provider._oauthProviderDao = oauthProviderDao;

        assertThrows(CloudRuntimeException.class, () -> {
            githubOAuth2Provider.getAccessToken(secretCode);
        });

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty("Content-Type", "application/json");
        verify(connection).setDoOutput(true);

        verify(connection.getOutputStream()).write(jsonParams.getBytes("utf-8"));

        verify(connection).getResponseCode();

        verify(connection).disconnect();
    }

    @Test
    public void testGetAccessTokenWithIOException() throws Exception {
        String secretCode = "secretCode";
        String clientId = "clientId";
        String secretKey = "secretKey";
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String jsonParams = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + secretKey + "\",\"code\":\"" + secretCode + "\"}";

        OauthProviderVO oauthProviderVO = new OauthProviderVO();
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setSecretKey(secretKey);

        URL url = new URL(tokenUrl);
        when(url.openConnection()).thenReturn(connection);

        OauthProviderDao oauthProviderDao = mock(OauthProviderDao.class);
        when(oauthProviderDao.findByProvider(githubOAuth2Provider.getName())).thenReturn(oauthProviderVO);

        githubOAuth2Provider._oauthProviderDao = oauthProviderDao;

        assertThrows(CloudRuntimeException.class, () -> {
            githubOAuth2Provider.getAccessToken(secretCode);
        });

        verify(connection).setRequestMethod("POST");
        verify(connection).setRequestProperty("Content-Type", "application/json");
        verify(connection).setDoOutput(true);

        verify(connection.getOutputStream()).write(jsonParams.getBytes("utf-8"));

        verify(connection).disconnect();
    }
}
