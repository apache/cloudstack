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

import static org.mockito.Matchers.argThat;

import java.io.IOException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.mockito.ArgumentMatcher;

public class HttpRequestMatcher extends ArgumentMatcher<HttpRequest> {
    private final HttpRequest wanted;

    public HttpRequestMatcher(final HttpRequest wanted) {
        this.wanted = wanted;
    }

    public static HttpRequest eq(final HttpRequest request) {
        return argThat(new HttpRequestMatcher(request));
    }

    @Override
    public boolean matches(final Object actual) {
        if (actual instanceof HttpUriRequest) {
            final HttpUriRequest converted = (HttpUriRequest) actual;
            return checkMethod(converted) && checkUri(converted) && checkPayload(converted);
        } else {
            return wanted == actual;
        }
    }

    private boolean checkPayload(final HttpUriRequest actual) {
        final String wantedPayload = getPayload(wanted);
        final String actualPayload = getPayload(actual);
        return equalsString(wantedPayload, actualPayload);
    }

    private static String getPayload(final HttpRequest request) {
        String payload = "";
        if (request instanceof HttpEntityEnclosingRequest) {
            try {
                payload = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
            } catch (final ParseException e) {
                throw new IllegalArgumentException("Couldn't read request's entity payload.", e);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Couldn't read request's entity payload.", e);
            }
        }
        return payload;
    }

    private boolean checkUri(final HttpUriRequest actual) {
        if (wanted instanceof HttpUriRequest) {
            final String wantedQuery = ((HttpUriRequest) wanted).getURI().getQuery();
            final String actualQuery = actual.getURI().getQuery();
            return equalsString(wantedQuery, actualQuery);
        } else {
            return wanted == actual;
        }
    }

    private boolean checkMethod(final HttpUriRequest actual) {
        if (wanted instanceof HttpUriRequest) {
            final String wantedMethod = ((HttpUriRequest) wanted).getMethod();
            final String actualMethod = actual.getMethod();
            return equalsString(wantedMethod, actualMethod);
        } else {
            return wanted == actual;
        }
    }

    private static boolean equalsString(final String a, final String b) {
        return a == b || a != null && a.equals(b);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(describe(wanted));
    }

    public String describe(final HttpRequest object) {
        final StringBuilder sb = new StringBuilder();
        if (object instanceof HttpUriRequest) {
            final HttpUriRequest converted = (HttpUriRequest) object;
            sb.append("method = ").append(converted.getMethod());
            sb.append(", query = ").append(converted.getURI().getQuery());
            sb.append(", payload = ").append(getPayload(object));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public SelfDescribing withExtraTypeInfo() {
        return new SelfDescribing() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("(" + wanted.getClass().getSimpleName() + ") ").appendText(describe(wanted));
            }
        };
    }

    public boolean typeMatches(final Object object) {
        return wanted != null && object != null && object.getClass() == wanted.getClass();
    }

}
