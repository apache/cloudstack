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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.rest.BasicRestClient;
import com.cloud.utils.rest.CloudstackRESTException;
import com.cloud.utils.rest.HttpConstants;
import com.cloud.utils.rest.HttpMethods;
import com.cloud.utils.rest.HttpStatusCodeHelper;
import com.cloud.utils.rest.HttpUriRequestBuilder;

public class NiciraRestClient extends BasicRestClient {

    private static final Logger s_logger = Logger.getLogger(NiciraRestClient.class);

    private static final String CONTENT_TYPE = HttpConstants.CONTENT_TYPE;
    private static final String TEXT_HTML_CONTENT_TYPE = HttpConstants.TEXT_HTML_CONTENT_TYPE;

    private static final int DEFAULT_BODY_RESP_MAX_LEN = 1024;
    private static final int DEFAULT_EXECUTION_LIMIT = 5;

    private final ExecutionCounter counter;
    private final int maxResponseErrorMesageLength;
    private final int executionLimit;

    private final String username;
    private final String password;
    private final String loginUrl;

    private NiciraRestClient(final Builder builder) {
        super(builder.client, builder.clientContext, builder.hostname);
        executionLimit = builder.executionLimit;
        counter = new ExecutionCounter(executionLimit);
        maxResponseErrorMesageLength = builder.maxResponseErrorMesageLength;
        username = builder.username;
        password = builder.password;
        loginUrl = builder.loginUrl;
    }

    public static Builder create() {
        return new Builder();
    }

    @Override
    public CloseableHttpResponse execute(final HttpUriRequest request) throws CloudstackRESTException {
        return execute(request, 0);
    }

    private CloseableHttpResponse execute(final HttpUriRequest request, final int previousStatusCode) throws CloudstackRESTException {
        if (counter.hasReachedExecutionLimit()) {
            throw new CloudstackRESTException("Reached max executions limit of " + executionLimit);
        }
        counter.incrementExecutionCounter();
        s_logger.debug("Executing " + request.getMethod() + " request [execution count = " + counter.getValue() + "]");
        final CloseableHttpResponse response = super.execute(request);

        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();
        s_logger.debug("Status of last request: " + statusLine.toString());
        if (HttpStatusCodeHelper.isUnauthorized(statusCode)) {
            return handleUnauthorizedResponse(request, previousStatusCode, response, statusCode);
        } else if (HttpStatusCodeHelper.isSuccess(statusCode)) {
            return handleSuccessResponse(response);
        } else {
            throw new CloudstackRESTException("Unexpecetd status code: " + statusCode);
        }
    }

    private CloseableHttpResponse handleUnauthorizedResponse(final HttpUriRequest request, final int previousStatusCode, final CloseableHttpResponse response, final int statusCode)
                    throws CloudstackRESTException {
        super.closeResponse(response);
        if (HttpStatusCodeHelper.isUnauthorized(previousStatusCode)) {
            s_logger.error(responseToErrorMessage(response));
            throw new CloudstackRESTException("Two consecutive failed attempts to authenticate against REST server");
        }
        final HttpUriRequest authenticateRequest = createAuthenticationRequest();
        final CloseableHttpResponse loginResponse = execute(authenticateRequest, statusCode);
        final int loginStatusCode = loginResponse.getStatusLine().getStatusCode();
        super.closeResponse(loginResponse);
        return execute(request, loginStatusCode);
    }

    private CloseableHttpResponse handleSuccessResponse(final CloseableHttpResponse response) {
        counter.resetExecutionCounter();
        return response;
    }

    private HttpUriRequest createAuthenticationRequest() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("password", password);
        return HttpUriRequestBuilder.create()
            .method(HttpMethods.POST)
            .methodParameters(parameters)
            .path(loginUrl)
            .build();
    }

    private String responseToErrorMessage(final CloseableHttpResponse response) {
        String errorMessage = response.getStatusLine().toString();
        if (response.containsHeader(CONTENT_TYPE) && TEXT_HTML_CONTENT_TYPE.equals(response.getFirstHeader(CONTENT_TYPE).getValue())) {
            try {
                final HttpEntity entity = response.getEntity();
                final String respobnseBody = EntityUtils.toString(entity);
                errorMessage = respobnseBody.subSequence(0, maxResponseErrorMesageLength).toString();
            } catch (final IOException e) {
                s_logger.debug("Could not read repsonse body. Response: " + response, e);
            }
        }

        return errorMessage;
    }

    protected static class Builder extends BasicRestClient.Builder<Builder> {
        private CloseableHttpClient client;
        private HttpClientContext clientContext;
        private String hostname;
        private String username;
        private String password;
        private String loginUrl;
        private int executionLimit = DEFAULT_EXECUTION_LIMIT;
        private int maxResponseErrorMesageLength = DEFAULT_BODY_RESP_MAX_LEN;

        public Builder hostname(final String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder loginUrl(final String loginUrl) {
            this.loginUrl = loginUrl;
            return this;
        }

        @Override
        public Builder client(final CloseableHttpClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder clientContext(final HttpClientContext clientContext) {
            this.clientContext = clientContext;
            return this;
        }

        public Builder executionLimit(final int executionLimit) {
            this.executionLimit = executionLimit;
            return this;
        }

        public Builder maxResponseErrorMesageLength(final int maxResponseErrorMesageLength) {
            this.maxResponseErrorMesageLength = maxResponseErrorMesageLength;
            return this;
        }

        @Override
        public NiciraRestClient build() {
            return new NiciraRestClient(this);
        }

    }
}