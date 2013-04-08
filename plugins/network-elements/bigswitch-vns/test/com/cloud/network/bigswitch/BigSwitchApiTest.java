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
package com.cloud.network.bigswitch;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class BigSwitchApiTest {
    BigSwitchVnsApi _api;
    HttpClient _client = mock(HttpClient.class);
    HttpMethod _method;

    @Before
    public void setUp() {
        HttpClientParams hmp = mock(HttpClientParams.class);
        when (_client.getParams()).thenReturn(hmp);
        _api = new BigSwitchVnsApi() {
            @Override
            protected HttpClient createHttpClient() {
                return _client;
            }

            @Override
            protected HttpMethod createMethod(String type, String uri, int port) {
                return _method;
            }
        };
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteUpdateObjectWithoutHostname() throws BigSwitchVnsApiException {
        _api.setControllerAddress(null);
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteCreateObjectWithoutHostname() throws BigSwitchVnsApiException {
        _api.setControllerAddress(null);
        _api.executeCreateObject(new String(), String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteDeleteObjectWithoutHostname() throws BigSwitchVnsApiException {
        _api.setControllerAddress(null);
        _api.executeDeleteObject("/");
    }

    @Test
    public void executeMethodTest() throws BigSwitchVnsApiException {
        GetMethod gm = mock(GetMethod.class);

        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    /* Bit of a roundabout way to ensure that login is called after an un authorized result
     * It not possible to properly mock login()
     */
    @Test (expected=BigSwitchVnsApiException.class)
    public void executeMethodTestWithLogin() throws BigSwitchVnsApiException, HttpException, IOException {
        GetMethod gm = mock(GetMethod.class);
        when(_client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        _api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    @Test
    public void testExecuteCreateObject() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeCreateObject(network, NetworkData.class, "/", Collections.<String, String> emptyMap());
        verify(_method, times(1)).releaseConnection();

    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteCreateObjectFailure() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-Type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeCreateObject(network, NetworkData.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteCreateObjectException() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        when(_client.executeMethod((HttpMethod) any())).thenThrow(new HttpException());
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-Type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        try {
            _api.executeCreateObject(network, NetworkData.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteUpdateObject() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteUpdateObjectFailure() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-Type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteUpdateObjectException() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(_client.executeMethod((HttpMethod) any())).thenThrow(new IOException());
        try {
            _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteDeleteObject() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeDeleteObject("/");
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteDeleteObjectFailure() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-Type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeDeleteObject("/");
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test (expected=BigSwitchVnsApiException.class)
    public void testExecuteDeleteObjectException() throws BigSwitchVnsApiException, IOException {
        _api.setControllerAddress("10.10.0.10");
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(_client.executeMethod((HttpMethod) any())).thenThrow(new HttpException());
        try {
            _api.executeDeleteObject("/");
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

}
