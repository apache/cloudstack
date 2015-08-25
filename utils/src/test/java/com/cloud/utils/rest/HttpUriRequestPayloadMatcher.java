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

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class HttpUriRequestPayloadMatcher extends FeatureMatcher<HttpUriRequest, String> {

    public static HttpUriRequest aPayload(final String payload) {
        return argThat(hasPayload(payload));
    }

    public static HttpUriRequestPayloadMatcher hasPayload(final String payload) {
        return new HttpUriRequestPayloadMatcher(equalTo(payload), "payload", "payload");
    }

    public HttpUriRequestPayloadMatcher(final Matcher<? super String> subMatcher, final String featureDescription, final String featureName) {
        super(subMatcher, featureDescription, featureName);
    }

    @Override
    protected String featureValueOf(final HttpUriRequest actual) {
        String payload = "";
        if (actual instanceof HttpEntityEnclosingRequest) {
            try {
                payload = EntityUtils.toString(((HttpEntityEnclosingRequest) actual).getEntity());
            } catch (final ParseException e) {
                throw new IllegalArgumentException("Couldn't read request's entity payload.", e);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Couldn't read request's entity payload.", e);
            }
        }
        return payload;
    }
}
