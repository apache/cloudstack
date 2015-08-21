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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import com.google.common.base.Optional;

public class HttpUriRequestBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithNullMethod() throws Exception {
        HttpUriRequestBuilder.create().path("/path").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithNullPath() throws Exception {
        HttpUriRequestBuilder.create().method(HttpMethods.GET).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithEmptyPath() throws Exception {
        HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("")
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIlegalPath() throws Exception {
        HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("path")
            .build();
    }

    @Test
    public void testBuildSimpleRequest() throws Exception {
        final HttpUriRequest request = HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("/path")
            .build();

        assertThat(request, notNullValue());
        assertThat(request.getURI().getPath(), equalTo("/path"));
        assertThat(request.getURI().getScheme(), nullValue());
        assertThat(request.getURI().getQuery(), nullValue());
        assertThat(request.getURI().getHost(), nullValue());
        assertThat(request.getMethod(), equalTo(HttpGet.METHOD_NAME));
    }

    @Test
    public void testBuildRequestWithParameters() throws Exception {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        final HttpUriRequest request = HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("/path")
            .parameters(parameters)
            .build();

        assertThat(request, notNullValue());
        assertThat(request.getURI().getPath(), equalTo("/path"));
        assertThat(request.getURI().getQuery(), equalTo("key1=value1"));
        assertThat(request.getURI().getScheme(), nullValue());
        assertThat(request.getURI().getHost(), nullValue());
        assertThat(request.getMethod(), equalTo(HttpGet.METHOD_NAME));
    }

    @Test
    public void testBuildRequestWithJsonPayload() throws Exception {
        final HttpUriRequest request = HttpUriRequestBuilder.create()
            .method(HttpMethods.GET)
            .path("/path")
            .jsonPayload(Optional.of("{'key1':'value1'}"))
            .build();

        assertThat(request, notNullValue());
        assertThat(request.getURI().getPath(), equalTo("/path"));
        assertThat(request.getURI().getScheme(), nullValue());
        assertThat(request.getURI().getQuery(), nullValue());
        assertThat(request.getURI().getHost(), nullValue());
        assertThat(request.getMethod(), equalTo(HttpGet.METHOD_NAME));
        assertThat(request.containsHeader(HttpConstants.CONTENT_TYPE), equalTo(true));
        assertThat(request.getFirstHeader(HttpConstants.CONTENT_TYPE).getValue(), equalTo(HttpConstants.JSON_CONTENT_TYPE));
        assertThat(request, HttpUriRequestPayloadMatcher.hasPayload("{'key1':'value1'}"));
    }

}
