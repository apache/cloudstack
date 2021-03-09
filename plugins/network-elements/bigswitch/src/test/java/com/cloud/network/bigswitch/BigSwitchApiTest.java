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

package com.cloud.network.bigswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

public class BigSwitchApiTest {
    BigSwitchBcfApi _api;
    HttpClient _client = mock(HttpClient.class);
    HttpMethod _method;

    @Before
    public void setUp() {
        HttpClientParams hmp = mock(HttpClientParams.class);
        when(_client.getParams()).thenReturn(hmp);
        _api = new BigSwitchBcfApi(){
            @Override
            protected HttpClient createHttpClient() {
                return _client;
            }

            @Override
            protected HttpMethod createMethod(String type, String uri, int port) {
                return _method;
            }
        };
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithoutHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress(null);
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithEmptyHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress("");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithoutUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername(null);
        _api.setControllerPassword("mypassword");
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithEmptyUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("");
        _api.setControllerPassword("mypassword");
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithoutPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword(null);
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectWithEmptyPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("");
        _api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithoutHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress(null);
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithEmptyHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress("");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithoutUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername(null);
        _api.setControllerPassword("mypassword");
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithEmptyUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("");
        _api.setControllerPassword("mypassword");
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithoutPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword(null);
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectWithEmptyPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("");
        _api.executeCreateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithoutHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress(null);
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeDeleteObject("/");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithEmptyHostname() throws BigSwitchBcfApiException {
        _api.setControllerAddress("");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("mypassword");
        _api.executeDeleteObject("/");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithoutUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername(null);
        _api.setControllerPassword("mypassword");
        _api.executeDeleteObject("/");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithEmptyUsername() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("");
        _api.setControllerPassword("mypassword");
        _api.executeDeleteObject("/");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithoutPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword(null);
        _api.executeDeleteObject("/");
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectWithEmptyPassword() throws BigSwitchBcfApiException {
        _api.setControllerAddress("10.10.0.10");
        _api.setControllerUsername("myname");
        _api.setControllerPassword("");
        _api.executeDeleteObject("/");
    }

    @Test
    public void executeMethodTestOK() throws BigSwitchBcfApiException, HttpException, IOException {
        GetMethod gm = mock(GetMethod.class);
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void executeMethodTestUnauthorized() throws BigSwitchBcfApiException, IOException {
        GetMethod gm = mock(GetMethod.class);
        when(_client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        _api.executeMethod(gm);
    }

    @Test
    public void testExecuteCreateObjectOK() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getResponseHeader("X-BSN-BVS-HASH-MATCH")).thenReturn(new Header("X-BSN-BVS-HASH-MATCH", UUID.randomUUID().toString()));
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        String hash = _api.executeCreateObject(network, "/", Collections.<String, String> emptyMap());
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);assertNotEquals(hash, "");
        assertNotEquals(hash, BigSwitchBcfApi.HASH_CONFLICT);
        assertNotEquals(hash, BigSwitchBcfApi.HASH_IGNORE);
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectConflict() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);
        _api.executeCreateObject(network, "/", Collections.<String, String> emptyMap());
    }

    @Test
    public void testExecuteCreateObjectSecondary() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_SEE_OTHER);
        String hash = _api.executeCreateObject(network, "/", Collections.<String, String> emptyMap());
        assertEquals(hash, BigSwitchBcfApi.HASH_IGNORE);
        assertEquals(_api.getControllerData().isPrimary(), false);
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectFailure() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeCreateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteCreateObjectException() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        when(_client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        _method = mock(PostMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        try {
            _api.executeCreateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteUpdateObjectOK() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getResponseHeader("X-BSN-BVS-HASH-MATCH")).thenReturn(new Header("X-BSN-BVS-HASH-MATCH", UUID.randomUUID().toString()));
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        String hash = _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
        assertNotEquals(hash, "");
        assertNotEquals(hash, BigSwitchBcfApi.HASH_CONFLICT);
        assertNotEquals(hash, BigSwitchBcfApi.HASH_IGNORE);
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectConflict() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);
        _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
    }

    @Test
    public void testExecuteUpdateObjectSecondary() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_SEE_OTHER);
        String hash = _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        assertEquals(hash, BigSwitchBcfApi.HASH_IGNORE);
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectFailure() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteUpdateObjectException() throws BigSwitchBcfApiException, IOException {
        NetworkData network = new NetworkData();
        _method = mock(PutMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(_client.executeMethod((HttpMethod)any())).thenThrow(new IOException());
        try {
            _api.executeUpdateObject(network, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteDeleteObject() throws BigSwitchBcfApiException, IOException {
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        _api.executeDeleteObject("/");
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectFailure() throws BigSwitchBcfApiException, IOException {
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(_method.getResponseHeader("Content-type")).thenReturn(header);
        when(_method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(_method.isRequestSent()).thenReturn(true);
        try {
            _api.executeDeleteObject("/");
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test(expected = BigSwitchBcfApiException.class)
    public void testExecuteDeleteObjectException() throws BigSwitchBcfApiException, IOException {
        _method = mock(DeleteMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(_client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            _api.executeDeleteObject("/");
        } finally {
            verify(_method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteRetrieveControllerPrimaryStatus() throws BigSwitchBcfApiException, IOException {
        _method = mock(GetMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(((HttpMethodBase)_method).getResponseBodyAsString(2048)).thenReturn("{'healthy': true, 'topologySyncRequested': false}");
        _api.executeRetrieveObject(new TypeToken<ControlClusterStatus>() {
        }.getType(), "/", null);
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
        assertEquals(_api.getControllerData().isPrimary(), true);
    }

    @Test
    public void testExecuteRetrieveControllerPrimaryStatusWithTopoConflict() throws BigSwitchBcfApiException, IOException {
        _method = mock(GetMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);
        when(((HttpMethodBase)_method).getResponseBodyAsString(2048)).thenReturn("{'healthy': true, 'topologySyncRequested': true}");
        _api.executeRetrieveObject(new TypeToken<ControlClusterStatus>() {
        }.getType(), "/", null);
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
        assertEquals(_api.getControllerData().isPrimary(), true);
    }

    @Test
    public void testExecuteRetrieveControllerSecondaryStatus() throws BigSwitchBcfApiException, IOException {
        _method = mock(GetMethod.class);
        when(_method.getStatusCode()).thenReturn(HttpStatus.SC_SEE_OTHER);
        when(((HttpMethodBase)_method).getResponseBodyAsString(1024)).thenReturn("{'healthy': true, 'topologySyncRequested': false}");
        _api.executeRetrieveObject(new TypeToken<ControlClusterStatus>() {
        }.getType(), "/", null);
        verify(_method, times(1)).releaseConnection();
        verify(_client, times(1)).executeMethod(_method);
        assertEquals(_api.getControllerData().isPrimary(), false);
    }
}
