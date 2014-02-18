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
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Basic authentication strategy. This strategy needs user and password for authentication.
 *
 * A login URL is needed which will be used for login and getting the cookie to be
 * used in next requests. If an executeMethod request fails due to authorization it will try
 * to login, get the cookie and repeat the attempt to execute the method.
 */
public class RESTValidationStrategy {

    private static final Logger s_logger = Logger.getLogger(RESTValidationStrategy.class);

    protected String host;
    protected String user;
    protected String password;
    protected String serverVersion;
    protected String loginUrl;

    public RESTValidationStrategy(final String host, final String user, final String password,
            final String serverVersion, final String loginUrl) {
        super();
        this.host = host;
        this.user = user;
        this.password = password;
        this.serverVersion = serverVersion;
        this.loginUrl = loginUrl;
    }

    public RESTValidationStrategy(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public RESTValidationStrategy() {
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void executeMethod(final HttpMethodBase method, final HttpClient client,
            final String protocol)
                    throws CloudstackRESTException, HttpException, IOException {
        if (host == null || host.isEmpty() || user == null || user.isEmpty() || password == null || password.isEmpty()) {
            throw new CloudstackRESTException("Hostname/credentials are null or empty");
        }

        client.executeMethod(method);
        if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            method.releaseConnection();
            // login and try again
            login(protocol, client);
            client.executeMethod(method);
        }
    }

    /**
     * Logs against the REST server. The cookie is stored in the <code>_authcookie<code> variable.
     * <p>
     * The method returns false if the login failed or the connection could not be made.
     *
     */
    protected void login(final String protocol,
            final HttpClient client)
                    throws CloudstackRESTException {
        String url;

        if (host == null || host.isEmpty() || user == null || user.isEmpty() || password == null || password.isEmpty()) {
            throw new CloudstackRESTException("Hostname/credentials are null or empty");
        }

        try {
            url = new URL(protocol, host, loginUrl).toString();
        } catch (final MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new CloudstackRESTException("Unable to build Nicira API URL", e);
        }

        final PostMethod pm = new PostMethod(url);
        pm.addParameter("username", user);
        pm.addParameter("password", password);

        try {
            client.executeMethod(pm);
        } catch (final HttpException e) {
            throw new CloudstackRESTException("REST Service API login failed ", e);
        } catch (final IOException e) {
            throw new CloudstackRESTException("REST Service API login failed ", e);
        } finally {
            pm.releaseConnection();
        }

        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            s_logger.error("REST Service API login failed : " + pm.getStatusText());
            throw new CloudstackRESTException("REST Service API login failed " + pm.getStatusText());
        }

        // Extract the version for later use
        if (pm.getResponseHeader("Server") != null) {
            serverVersion = pm.getResponseHeader("Server").getValue();
            s_logger.debug("Server reports version " + serverVersion);
        }

        // Success; the cookie required for login is kept in _client
    }

}
