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

package com.cloud.utils.net;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;

public final class HTTPUtils {

    protected static Logger LOGGER = LogManager.getLogger(HTTPUtils.class);

    // The connection manager.
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private HTTPUtils() {}

    public static HttpClient getHTTPClient() {
        return new HttpClient(s_httpClientManager);
    }

    /**
     * @return A HttpMethodRetryHandler with given number of retries.
     */
    public static HttpMethodRetryHandler getHttpMethodRetryHandler(final int retryCount) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initializing new HttpMethodRetryHandler with retry count " + retryCount);
        }

        return new HttpMethodRetryHandler() {
            @Override
            public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                if (executionCount >= retryCount) {
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // Retry if the server dropped connection on us
                    return true;
                }
                if (!method.isRequestSent()) {
                    // Retry if the request has not been sent fully or
                    // if it's OK to retry methods that have been sent
                    return true;
                }
                // otherwise do not retry
                return false;
            }
        };
    }

    /**
     * @param proxy
     * @param httpClient
     */
    public static void setProxy(Proxy proxy, HttpClient httpClient) {
        if (proxy != null && httpClient != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting proxy with host " + proxy.getHost() + " and port " + proxy.getPort() + " for host " + httpClient.getHostConfiguration().getHost() + ":" + httpClient.getHostConfiguration().getPort());
            }

            httpClient.getHostConfiguration().setProxy(proxy.getHost(), proxy.getPort());
            if (proxy.getUserName() != null && proxy.getPassword() != null) {
                httpClient.getState().setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword()));
            }
        }
    }

    /**
     * @param username
     * @param password
     * @param httpClient
     */
    public static void setCredentials(String username, String password, HttpClient httpClient) {
        if (username != null && password != null && httpClient != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting credentials with username " + username + " for host " + httpClient.getHostConfiguration().getHost() + ":" + httpClient.getHostConfiguration().getPort());
            }

            httpClient.getParams().setAuthenticationPreemptive(true);
            httpClient.getState().setCredentials(
                    new AuthScope(httpClient.getHostConfiguration().getHost(), httpClient.getHostConfiguration().getPort(), AuthScope.ANY_REALM), new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * @param httpClient
     * @param httpMethod
     * @return
     *          Returns the HTTP Status Code or -1 if an exception occurred.
     */
    public static int executeMethod(HttpClient httpClient, HttpMethod httpMethod) {
        // Execute GetMethod
        try {
            return httpClient.executeMethod(httpMethod);
        } catch (IOException e) {
            LOGGER.warn("Exception while executing HttpMethod " + httpMethod.getName() + " on URL " + httpMethod.getPath());
            return  -1;
        }
    }

    /**
     * @param responseCode
     * @return
     */
    public static boolean verifyResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_MOVED_TEMPORARILY:
                return true;
            default:
                return false;

        }
    }
}
