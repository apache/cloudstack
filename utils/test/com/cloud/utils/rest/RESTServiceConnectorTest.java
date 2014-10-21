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

package com.cloud.utils.rest;

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

public class RESTServiceConnectorTest {
    protected static final String UUID = "aaaa";
    protected static final String UUID_JSON_RESPONSE = "{\"uuid\" : \"aaaa\"}";

    RESTServiceConnector connector;
    HttpClient client = mock(HttpClient.class);
    HttpMethod method;
    String type;
    String uri;

    @Before
    public void setUp() {
        final HttpClientParams hmp = mock(HttpClientParams.class);
        when(client.getParams()).thenReturn(hmp);
        connector = new RESTServiceConnector(null) {
            @Override
            public HttpClient createHttpClient() {
                return client;
            }

            @Override
            public HttpMethod createMethod(final String newType, final String newUri) {
                type = newType;
                uri = newUri;
                return method;
            }
        };

        connector.validation = new RESTValidationStrategy();
        connector.setAdminCredentials("admin", "adminpass");
        connector.setControllerAddress("localhost");
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteLoginWithoutHostname() throws CloudstackRESTException {
        connector.setControllerAddress(null);
        connector.validation.login(RESTServiceConnector.protocol, client);
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteLoginWithoutCredentials() throws CloudstackRESTException {
        method = mock(PutMethod.class);
        connector.setAdminCredentials(null, null);
        connector.validation.login(RESTServiceConnector.protocol, client);
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteUpdateObjectWithoutHostname() throws CloudstackRESTException {
        method = mock(PutMethod.class);
        connector.setControllerAddress(null);
        connector.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteUpdateObjectWithoutCredentials() throws CloudstackRESTException {
        method = mock(PutMethod.class);
        connector.setAdminCredentials(null, null);
        connector.executeUpdateObject(new String(), "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteCreateObjectWithoutHostname() throws CloudstackRESTException {
        method = mock(PostMethod.class);
        connector.setControllerAddress(null);
        connector.executeCreateObject(new String(), String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteCreateObjectWithoutCredentials() throws CloudstackRESTException {
        method = mock(PostMethod.class);
        connector.setAdminCredentials(null, null);
        connector.executeCreateObject(new String(), String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteDeleteObjectWithoutHostname() throws CloudstackRESTException {
        method = mock(DeleteMethod.class);
        connector.setControllerAddress(null);
        connector.executeDeleteObject("/");
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteDeleteObjectWithoutCredentials() throws CloudstackRESTException {
        method = mock(DeleteMethod.class);
        connector.setAdminCredentials(null, null);
        connector.executeDeleteObject("/");
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteRetrieveObjectWithoutHostname() throws CloudstackRESTException {
        method = mock(GetMethod.class);
        connector.setControllerAddress(null);
        connector.executeRetrieveObject(String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteRetrieveObjectWithoutCredentials() throws CloudstackRESTException {
        method = mock(GetMethod.class);
        connector.setAdminCredentials(null, null);
        connector.executeRetrieveObject(String.class, "/", Collections.<String, String> emptyMap());
    }

    @Test
    public void testExecuteMethod() throws CloudstackRESTException {
        final GetMethod gm = mock(GetMethod.class);

        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        connector.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteMethodWithLogin() throws CloudstackRESTException, HttpException, IOException {
        final GetMethod gm = mock(GetMethod.class);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        connector.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    /* Bit of a roundabout way to ensure that login is called after an un authorized result
     * It not possible to properly mock login()
     */
    public void testExecuteMethodWithLoginSucced2ndAttempt() throws CloudstackRESTException, HttpException, IOException {
        // Prepare
        final GetMethod gm = mock(GetMethod.class);
        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED).thenReturn(HttpStatus.SC_UNAUTHORIZED);

        final RESTValidationStrategy previousValidationStrategy = connector.validation;
        connector.validation = new RESTValidationStrategy(){
            @Override
            protected void login(final String protocol, final HttpClient client)
                    throws CloudstackRESTException {
                // Do nothing
            }
        };
        connector.setAdminCredentials("admin", "adminpass");
        connector.setControllerAddress("localhost");

        // Execute
        connector.executeMethod(gm);
        // Leave mock object as is was
        connector.validation = previousValidationStrategy;

        // Assert/verify
        verify(gm, times(2)).getStatusCode();
    }

    @Test
    public void testExecuteCreateObject() throws CloudstackRESTException, IOException {
        JsonEntity ls = new JsonEntity();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        ls = connector.executeCreateObject(ls, JsonEntity.class, "/", Collections.<String, String> emptyMap());
        assertTrue(UUID.equals(ls.getUuid()));
        verify(method, times(1)).releaseConnection();

    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteCreateObjectFailure() throws CloudstackRESTException, IOException {
        JsonEntity ls = new JsonEntity();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            ls = connector.executeCreateObject(ls, JsonEntity.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteCreateObjectException() throws CloudstackRESTException, IOException {
        JsonEntity ls = new JsonEntity();
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        try {
            ls = connector.executeCreateObject(ls, JsonEntity.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteUpdateObject() throws CloudstackRESTException, IOException {
        final JsonEntity ls = new JsonEntity();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        connector.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteUpdateObjectFailure() throws CloudstackRESTException, IOException {
        final JsonEntity ls = new JsonEntity();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            connector.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteUpdateObjectException() throws CloudstackRESTException, IOException {
        final JsonEntity ls = new JsonEntity();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new IOException());
        try {
            connector.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteDeleteObject() throws CloudstackRESTException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        connector.executeDeleteObject("/");
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteDeleteObjectFailure() throws CloudstackRESTException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            connector.executeDeleteObject("/");
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteDeleteObjectException() throws CloudstackRESTException, IOException {
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            connector.executeDeleteObject("/");
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testExecuteRetrieveObject() throws CloudstackRESTException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        connector.executeRetrieveObject(JsonEntity.class, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteRetrieveObjectFailure() throws CloudstackRESTException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        final Header header = mock(Header.class);
        when(header.getValue()).thenReturn("text/html");
        when(method.getResponseHeader("Content-Type")).thenReturn(header);
        when(method.getResponseBodyAsString()).thenReturn("Off to timbuktu, won't be back later.");
        when(method.isRequestSent()).thenReturn(true);
        try {
            connector.executeRetrieveObject(JsonEntity.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test(expected = CloudstackRESTException.class)
    public void testExecuteRetrieveObjectException() throws CloudstackRESTException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            connector.executeRetrieveObject(JsonEntity.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

}

class JsonEntity {
    private String displayName;
    private String uuid;
    private String href;
    private String schema;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }
}