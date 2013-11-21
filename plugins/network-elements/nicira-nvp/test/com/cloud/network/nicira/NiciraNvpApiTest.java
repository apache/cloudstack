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
package com.cloud.network.nicira;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

public class NiciraNvpApiTest {
    NiciraNvpApi api;
    HttpClient client = mock(HttpClient.class);
    HttpMethod method;

    @Before
    public void setUp() {
        HttpClientParams hmp = mock(HttpClientParams.class);
        when(client.getParams()).thenReturn(hmp);
        api = new NiciraNvpApi() {
            @Override
            protected HttpClient createHttpClient() {
                return client;
            }

            @Override
            protected HttpMethod createMethod(String type, String uri) {
                return method;
            }
        };
        api.setAdminCredentials("admin", "adminpass");
        api.setControllerAddress("localhost");
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteLoginWithoutHostname() throws NiciraNvpApiException {
        api.setControllerAddress(null);
        api.login();
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteLoginWithoutCredentials() throws NiciraNvpApiException {
        api.setAdminCredentials(null, null);
        api.login();
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteUpdateObjectWithoutHostname() throws NiciraNvpApiException {
        api.setControllerAddress(null);
        api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteUpdateObjectWithoutCredentials() throws NiciraNvpApiException {
        api.setAdminCredentials(null, null);
        api.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteCreateObjectWithoutHostname() throws NiciraNvpApiException {
        api.setControllerAddress(null);
        api.executeCreateObject(new String(), String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteCreateObjectWithoutCredentials() throws NiciraNvpApiException {
        api.setAdminCredentials(null, null);
        api.executeCreateObject(new String(), String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteDeleteObjectWithoutHostname() throws NiciraNvpApiException {
        api.setControllerAddress(null);
        api.executeDeleteObject("/");
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteDeleteObjectWithoutCredentials() throws NiciraNvpApiException {
        api.setAdminCredentials(null, null);
        api.executeDeleteObject("/");
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteRetrieveObjectWithoutHostname() throws NiciraNvpApiException {
        api.setControllerAddress(null);
        api.executeRetrieveObject(String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteRetrieveObjectWithoutCredentials() throws NiciraNvpApiException {
        api.setAdminCredentials(null, null);
        api.executeDeleteObject("/");
    }

    @Test
    public void executeMethodTest() throws NiciraNvpApiException {
        GetMethod gm = mock(GetMethod.class);

        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    /* Bit of a roundabout way to ensure that login is called after an un authorized result
     * It not possible to properly mock login()
     */
    @Test(expected = NiciraNvpApiException.class)
    public void executeMethodTestWithLogin() throws NiciraNvpApiException, HttpException, IOException {
        GetMethod gm = mock(GetMethod.class);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    @Test
    public void testExecuteCreateObject() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(method.getResponseBodyAsString()).thenReturn("{ \"uuid\" : \"aaaa\" }");
        ls = api.executeCreateObject(ls, LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        assertTrue("aaaa".equals(ls.getUuid()));
        verify(method, times(1)).releaseConnection();

    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteCreateObjectFailure() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            ls = api.executeCreateObject(ls, LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteCreateObjectException() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        try {
            ls = api.executeCreateObject(ls, LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteUpdateObject() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        api.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteUpdateObjectFailure() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            api.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteUpdateObjectException() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new IOException());
        try {
            api.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteDeleteObject() throws NiciraNvpApiException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        api.executeDeleteObject("/");
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteDeleteObjectFailure() throws NiciraNvpApiException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            api.executeDeleteObject("/");
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteDeleteObjectException() throws NiciraNvpApiException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            api.executeDeleteObject("/");
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteRetrieveObject() throws NiciraNvpApiException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn("{ \"uuid\" : \"aaaa\" }");
        api.executeRetrieveObject(LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteRetrieveObjectFailure() throws NiciraNvpApiException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(method.getResponseBodyAsString()).thenReturn("{ \"uuid\" : \"aaaa\" }");
        Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            api.executeRetrieveObject(LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteRetrieveObjectException() throws NiciraNvpApiException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn("{ \"uuid\" : \"aaaa\" }");
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            api.executeRetrieveObject(LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

}
