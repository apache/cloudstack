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
import static org.junit.Assert.assertEquals;
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
import org.apache.commons.httpclient.NameValuePair;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class NiciraNvpApiTest {
    protected static final String UUID = "aaaa";
    protected static final String UUID2 = "bbbb";
    protected static final String UUID_SEC_PROFILE_URI = NiciraNvpApi.SEC_PROFILE_URI_PREFIX + "/aaaa";
    protected static final String SCHEMA = "myTestSchema";
    protected static final String SCHEMA2 = "myTestSchema2";
    protected static final String HREF = "myTestHref";
    protected static final String HREF2 = "myTestHref2";
    protected static final String DISPLAY_NAME = "myTestName";
    protected static final String UUID_JSON_RESPONSE = "{\"uuid\" : \"aaaa\"}";
    protected static final String SEC_PROFILE_JSON_RESPONSE =
            "{\"uuid\" : \"aaaa\","
            + "\"display_name\" : \"myTestName\","
            + "\"href\" : \"myTestHref\","
            + "\"schema\" : \"myTestSchema\"}";

    protected static final String SEC_PROFILE_LIST_JSON_RESPONSE = "{\"results\" : [{\"uuid\" : \"aaaa\","
            + "\"display_name\" : \"myTestName\","
            + "\"href\" : \"myTestHref\","
            + "\"schema\" : \"myTestSchema\"},"
            + "{ \"uuid\" : \"bbbb\","
            + "\"display_name\" : \"myTestName2\","
            + "\"href\" : \"myTestHref2\","
            + "\"schema\" : \"myTestSchema2\"}],"
            + "\"result_count\": 2}";

    NiciraNvpApi api;
    HttpClient client = mock(HttpClient.class);
    HttpMethod method;
    String type;
    String uri;

    @Before
    public void setUp() {
        final HttpClientParams hmp = mock(HttpClientParams.class);
        when(client.getParams()).thenReturn(hmp);
        api = new NiciraNvpApi() {
            @Override
            protected HttpClient createHttpClient() {
                return client;
            }

            @Override
            protected HttpMethod createMethod(final String newType, final String newUri) {
                type = newType;
                uri = newUri;
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
        final GetMethod gm = mock(GetMethod.class);

        when(gm.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        api.executeMethod(gm);
        verify(gm, times(1)).getStatusCode();
    }

    /* Bit of a roundabout way to ensure that login is called after an un authorized result
     * It not possible to properly mock login()
     */
    @Test(expected = NiciraNvpApiException.class)
    public void executeMethodTestWithLogin() throws NiciraNvpApiException, HttpException, IOException {
        final GetMethod gm = mock(GetMethod.class);
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
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        ls = api.executeCreateObject(ls, LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        assertTrue(UUID.equals(ls.getUuid()));
        verify(method, times(1)).releaseConnection();

    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteCreateObjectFailure() throws NiciraNvpApiException, IOException {
        LogicalSwitch ls = new LogicalSwitch();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
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
        final Header header = mock(Header.class);
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
        final LogicalSwitch ls = new LogicalSwitch();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        api.executeUpdateObject(ls, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteUpdateObjectFailure() throws NiciraNvpApiException, IOException {
        final LogicalSwitch ls = new LogicalSwitch();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        final Header header = mock(Header.class);
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
        final LogicalSwitch ls = new LogicalSwitch();
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
        final Header header = mock(Header.class);
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
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        api.executeRetrieveObject(LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        verify(method, times(1)).releaseConnection();
        verify(client, times(1)).executeMethod(method);
    }

    @Test(expected = NiciraNvpApiException.class)
    public void testExecuteRetrieveObjectFailure() throws NiciraNvpApiException, IOException {
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        final Header header = mock(Header.class);
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
        when(method.getResponseBodyAsString()).thenReturn(UUID_JSON_RESPONSE);
        when(client.executeMethod((HttpMethod)any())).thenThrow(new HttpException());
        try {
            api.executeRetrieveObject(LogicalSwitch.class, "/", Collections.<String, String> emptyMap());
        } finally {
            verify(method, times(1)).releaseConnection();
        }
    }

    @Test
    public void testFindSecurityProfile() throws NiciraNvpApiException, IOException {
        // Prepare
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn(SEC_PROFILE_LIST_JSON_RESPONSE);
        final NameValuePair[] queryString = new NameValuePair[]{
                new NameValuePair("fields","*")};

        // Execute
        final NiciraNvpList<SecurityProfile> actualProfiles = api.findSecurityProfile();

        // Assert
        verify(method, times(1)).releaseConnection();
        verify(method, times(1)).setQueryString(queryString);
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                UUID, actualProfiles.getResults().get(0).getUuid());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                HREF, actualProfiles.getResults().get(0).getHref());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                SCHEMA, actualProfiles.getResults().get(0).getSchema());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                UUID2, actualProfiles.getResults().get(1).getUuid());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                HREF2, actualProfiles.getResults().get(1).getHref());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                SCHEMA2, actualProfiles.getResults().get(1).getSchema());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                2, actualProfiles.getResultCount());
        assertEquals("Wrong URI for SecurityProfile creation REST service",
                NiciraNvpApi.SEC_PROFILE_URI_PREFIX, uri);
        assertEquals("Wrong URI for SecurityProfile creation REST service",
                NiciraNvpApi.GET_METHOD_TYPE, type);
    }

    @Test
    public void testFindSecurityProfileByUuid() throws NiciraNvpApiException, IOException {
        // Prepare
        method = mock(GetMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(method.getResponseBodyAsString()).thenReturn(SEC_PROFILE_LIST_JSON_RESPONSE);
        final NameValuePair[] queryString = new NameValuePair[]{
                new NameValuePair("uuid", UUID),
                new NameValuePair("fields","*")
        };

        // Execute
        final NiciraNvpList<SecurityProfile> actualProfiles = api.findSecurityProfile(UUID);

        // Assert
        verify(method, times(1)).releaseConnection();
        verify(method, times(1)).setQueryString(queryString);
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                UUID, actualProfiles.getResults().get(0).getUuid());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                HREF, actualProfiles.getResults().get(0).getHref());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                SCHEMA, actualProfiles.getResults().get(0).getSchema());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                UUID2, actualProfiles.getResults().get(1).getUuid());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                HREF2, actualProfiles.getResults().get(1).getHref());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                SCHEMA2, actualProfiles.getResults().get(1).getSchema());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                2, actualProfiles.getResultCount());
        assertEquals("Wrong URI for SecurityProfile creation REST service",
                NiciraNvpApi.SEC_PROFILE_URI_PREFIX, uri);
        assertEquals("Wrong HTTP method for SecurityProfile creation REST service",
                NiciraNvpApi.GET_METHOD_TYPE, type);
    }

    @Test
    public void testCreateSecurityProfile() throws NiciraNvpApiException, IOException {
        // Prepare
        final SecurityProfile inputSecProfile = new SecurityProfile();
        method = mock(PostMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(method.getResponseBodyAsString()).thenReturn(SEC_PROFILE_JSON_RESPONSE);

        // Execute
        final SecurityProfile actualSecProfile = api.createSecurityProfile(inputSecProfile);

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                UUID, actualSecProfile.getUuid());
        assertEquals("Wrong Uuid in the newly created SecurityProfile",
                HREF, actualSecProfile.getHref());
        assertEquals("Wrong Schema in the newly created SecurityProfile",
                SCHEMA, actualSecProfile.getSchema());
        assertEquals("Wrong URI for SecurityProfile creation REST service",
                NiciraNvpApi.SEC_PROFILE_URI_PREFIX, uri);
        assertEquals("Wrong HTTP method for SecurityProfile creation REST service",
                NiciraNvpApi.POST_METHOD_TYPE, type);
    }

    @Test
    public void testUpdateSecurityProfile() throws NiciraNvpApiException, IOException {
        // Prepare
        final SecurityProfile inputSecProfile = new SecurityProfile();
        method = mock(PutMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        // Execute
        api.updateSecurityProfile(inputSecProfile, UUID);

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong URI for SecurityProfile creation REST service",
                UUID_SEC_PROFILE_URI, uri);
        assertEquals("Wrong HTTP method for SecurityProfile creation REST service",
                NiciraNvpApi.PUT_METHOD_TYPE, type);
    }

    @Test
    public void testDeleteSecurityProfile() throws NiciraNvpApiException, IOException {
        // Prepare
        method = mock(DeleteMethod.class);
        when(method.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        // Execute
        api.deleteSecurityProfile(UUID);

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong URI for SecurityProfile deletion REST service",
                UUID_SEC_PROFILE_URI, uri);
        assertEquals("Wrong HTTP method for SecurityProfile deletion REST service",
                NiciraNvpApi.DELETE_METHOD_TYPE, type);
    }
}
