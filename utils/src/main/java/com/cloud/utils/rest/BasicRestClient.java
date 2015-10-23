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

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

public class BasicRestClient implements RestClient {

    private static final Logger s_logger = Logger.getLogger(BasicRestClient.class);

    private static final String HTTPS = HttpConstants.HTTPS;
    private static final int HTTPS_PORT = HttpConstants.HTTPS_PORT;

    private final CloseableHttpClient client;
    private final HttpClientContext clientContext;

    private BasicRestClient(final Builder<?> builder) {
        client = builder.client;
        clientContext = builder.clientContext;
        clientContext.setTargetHost(buildHttpHost(builder.host));
    }

    protected BasicRestClient(final CloseableHttpClient client, final HttpClientContext clientContex, final String host) {
        this.client = client;
        clientContext = clientContex;
        clientContext.setTargetHost(buildHttpHost(host));
    }

    private static HttpHost buildHttpHost(final String host) {
        return new HttpHost(host, HTTPS_PORT, HTTPS);
    }

    @SuppressWarnings("rawtypes")
    public static Builder create() {
        return new Builder();
    }

    @Override
    public CloseableHttpResponse execute(final HttpUriRequest request) throws CloudstackRESTException {
        logRequestExecution(request);
        try {
            return client.execute(clientContext.getTargetHost(), request, clientContext);
        } catch (final IOException e) {
            throw new CloudstackRESTException("Could not execute request " + request, e);
        }
    }

    private void logRequestExecution(final HttpUriRequest request) {
        final URI uri = request.getURI();
        String query = uri.getQuery();
        query = query != null ? "?" + query : "";
        s_logger.debug("Executig " + request.getMethod() + " request on " + clientContext.getTargetHost() + uri.getPath() + query);
    }

    @Override
    public void closeResponse(final CloseableHttpResponse response) throws CloudstackRESTException {
        try {
            s_logger.debug("Closing HTTP connection");
            response.close();
        } catch (final IOException e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Failed to close response object for request.\nResponse: ").append(response);
            throw new CloudstackRESTException(sb.toString(), e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static class Builder<T extends Builder> {
        private CloseableHttpClient client;
        private HttpClientContext clientContext = HttpClientContext.create();
        private String host;

        public T client(final CloseableHttpClient client) {
            this.client = client;
            return (T) this;
        }

        public T clientContext(final HttpClientContext clientContext) {
            this.clientContext = clientContext;
            return (T) this;
        }

        public T host(final String host) {
            this.host = host;
            return (T) this;
        }

        public BasicRestClient build() {
            return new BasicRestClient(this);
        }
    }

}
