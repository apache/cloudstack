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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.cloud.utils.rest.HttpClientHelper;
import com.cloud.utils.rest.HttpUriRequestMethodMatcher;
import com.cloud.utils.rest.HttpUriRequestPathMatcher;
import com.cloud.utils.rest.HttpUriRequestQueryMatcher;

public class NiciraNvpApiTest {
    private static final StatusLine HTTP_200_REPSONSE = new BasicStatusLine(new ProtocolVersion("HTTPS", 1, 1), HttpStatus.SC_OK, "OK");
    private static final StatusLine HTTP_201_REPSONSE = new BasicStatusLine(new ProtocolVersion("HTTPS", 1, 1), HttpStatus.SC_CREATED, "Created");

    protected static final String UUID = "aaaa";
    protected static final String UUID2 = "bbbb";
    protected static final String UUID_SEC_PROFILE_URI = NiciraConstants.SEC_PROFILE_URI_PREFIX + "/aaaa";
    protected static final String SCHEMA = "myTestSchema";
    protected static final String SCHEMA2 = "myTestSchema2";
    protected static final String HREF = "myTestHref";
    protected static final String HREF2 = "myTestHref2";
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

    private static NiciraNvpApi buildApi(final CloseableHttpClient httpClient) {
        return NiciraNvpApi.create()
            .host("localhost")
            .username("admin")
            .password("adminpassword")
            .httpClient(httpClient)
            .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindSecurityProfile() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_REPSONSE);
        when(response.getEntity()).thenReturn(new StringEntity(SEC_PROFILE_LIST_JSON_RESPONSE));
        final CloseableHttpClient httpClient = spy(HttpClientHelper.createHttpClient(2));
        doReturn(response).when(httpClient).execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class));
        final NiciraNvpApi api = buildApi(httpClient);

        final List<SecurityProfile> actualProfiles = api.findSecurityProfile();

        assertThat("Wrong number of results", actualProfiles, hasSize(2));
        assertThat("Wrong Uuid in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("uuid", equalTo(UUID)),
                        hasProperty("uuid", equalTo(UUID2))));
        assertThat("Wrong HREF in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("href", equalTo(HREF)),
                        hasProperty("href", equalTo(HREF2))));
        assertThat("Wrong Schema in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("schema", equalTo(SCHEMA)),
                        hasProperty("schema", equalTo(SCHEMA2))));
        verify(response, times(1)).close();
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("GET"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQuery("fields=*"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPathMatcher.aPath(NiciraConstants.SEC_PROFILE_URI_PREFIX), any(HttpClientContext.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindSecurityProfileByUuid() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_REPSONSE);
        when(response.getEntity()).thenReturn(new StringEntity(SEC_PROFILE_LIST_JSON_RESPONSE));
        final CloseableHttpClient httpClient = spy(HttpClientHelper.createHttpClient(2));
        doReturn(response).when(httpClient).execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class));
        final NiciraNvpApi api = buildApi(httpClient);

        final List<SecurityProfile> actualProfiles = api.findSecurityProfile(UUID);

        assertThat("Wrong number of results", actualProfiles, hasSize(2));
        assertThat("Wrong Uuid in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("uuid", equalTo(UUID)),
                        hasProperty("uuid", equalTo(UUID2))));
        assertThat("Wrong HREF in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("href", equalTo(HREF)),
                        hasProperty("href", equalTo(HREF2))));
        assertThat("Wrong Schema in the newly created SecurityProfile", actualProfiles, Matchers.<SecurityProfile> contains(
                        hasProperty("schema", equalTo(SCHEMA)),
                        hasProperty("schema", equalTo(SCHEMA2))));
        verify(response, times(1)).close();
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("GET"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("uuid=" + UUID), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("fields=*"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPathMatcher.aPath(NiciraConstants.SEC_PROFILE_URI_PREFIX), any(HttpClientContext.class));
    }

    @Test
    public void testCreateSecurityProfile() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_201_REPSONSE);
        when(response.getEntity()).thenReturn(new StringEntity(SEC_PROFILE_JSON_RESPONSE));
        final CloseableHttpClient httpClient = spy(HttpClientHelper.createHttpClient(2));
        doReturn(response).when(httpClient).execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class));
        final NiciraNvpApi api = buildApi(httpClient);

        final SecurityProfile actualSecProfile = api.createSecurityProfile(new SecurityProfile());

        assertThat("Wrong Uuid in the newly created SecurityProfile", actualSecProfile, hasProperty("uuid", equalTo(UUID)));
        assertThat("Wrong Href in the newly created SecurityProfile", actualSecProfile, hasProperty("href", equalTo(HREF)));
        assertThat("Wrong Schema in the newly created SecurityProfile", actualSecProfile, hasProperty("schema", equalTo(SCHEMA)));
        verify(response, times(1)).close();
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("POST"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPathMatcher.aPath(NiciraConstants.SEC_PROFILE_URI_PREFIX), any(HttpClientContext.class));
    }

    @Test
    public void testUpdateSecurityProfile() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_201_REPSONSE);
        when(response.getEntity()).thenReturn(new StringEntity(SEC_PROFILE_JSON_RESPONSE));
        final CloseableHttpClient httpClient = spy(HttpClientHelper.createHttpClient(2));
        doReturn(response).when(httpClient).execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class));
        final NiciraNvpApi api = buildApi(httpClient);

        api.updateSecurityProfile(new SecurityProfile(), UUID);

        verify(response, times(1)).close();
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("PUT"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPathMatcher.aPath(NiciraConstants.SEC_PROFILE_URI_PREFIX + "/" + UUID), any(HttpClientContext.class));
    }

    @Test
    public void testDeleteSecurityProfile() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_201_REPSONSE);
        when(response.getEntity()).thenReturn(new StringEntity(SEC_PROFILE_JSON_RESPONSE));
        final CloseableHttpClient httpClient = spy(HttpClientHelper.createHttpClient(2));
        doReturn(response).when(httpClient).execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class));
        final NiciraNvpApi api = buildApi(httpClient);

        api.deleteSecurityProfile(UUID);

        verify(response, times(1)).close();
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("DELETE"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPathMatcher.aPath(NiciraConstants.SEC_PROFILE_URI_PREFIX + "/" + UUID), any(HttpClientContext.class));
    }

}
