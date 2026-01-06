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

package org.apache.cloudstack.cloudian.client;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class CloudianClient {
    protected Logger logger = LogManager.getLogger(getClass());

    private final HttpClient httpClient;
    private final HttpClientContext httpContext;
    private final String adminApiUrl;

    public CloudianClient(final String host, final Integer port, final String scheme, final String username, final String password, final boolean validateSSlCertificate, final int timeout) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        final HttpHost adminHost = new HttpHost(host, port, scheme);
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(adminHost, new BasicScheme());

        this.adminApiUrl = adminHost.toURI();
        this.httpContext = HttpClientContext.create();
        this.httpContext.setCredentialsProvider(provider);
        this.httpContext.setAuthCache(authCache);

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        if (!validateSSlCertificate) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultRequestConfig(config)
                    .build();
        }
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            final Credentials credentials = httpContext.getCredentialsProvider().getCredentials(AuthScope.ANY);
            logger.error("Cloudian admin API authentication failed, please check Cloudian configuration. Admin auth principal=" + credentials.getUserPrincipal() + ", password=" + credentials.getPassword() + ", API url=" + adminApiUrl);
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "Cloudian backend API call unauthorized, please ask your administrator to fix integration issues.");
        }
    }

    private void checkResponseOK(final HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            logger.debug("Requested Cloudian resource does not exist");
            return;
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to find the requested resource and get valid response from Cloudian backend API call, please ask your administrator to diagnose and fix issues.");
        }
    }

    private boolean checkEmptyResponse(final HttpResponse response) throws IOException {
        return response != null && (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT ||
                response.getEntity() == null ||
                response.getEntity().getContent() == null);
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Operation timed out, please try again.");
        }
    }

    private HttpResponse delete(final String path) throws IOException {
        final HttpResponse response = httpClient.execute(new HttpDelete(adminApiUrl + path), httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse get(final String path) throws IOException {
        final HttpResponse response = httpClient.execute(new HttpGet(adminApiUrl + path), httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse post(final String path, final Object item) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(item);
        final StringEntity entity = new StringEntity(json);
        final HttpPost request = new HttpPost(adminApiUrl + path);
        request.setHeader("content-type", "application/json");
        request.setEntity(entity);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse put(final String path, final Object item) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(item);
        final StringEntity entity = new StringEntity(json);
        final HttpPut request = new HttpPut(adminApiUrl + path);
        request.setHeader("content-type", "application/json");
        request.setEntity(entity);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    ////////////////////////////////////////////////////////
    //////////////// Public APIs: User /////////////////////
    ////////////////////////////////////////////////////////

    public boolean addUser(final CloudianUser user) {
        if (user == null) {
            return false;
        }
        logger.debug("Adding Cloudian user: " + user);
        try {
            final HttpResponse response = put("/user", user);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to add Cloudian user due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public CloudianUser listUser(final String userId, final String groupId) {
        if (StringUtils.isAnyEmpty(userId, groupId)) {
            return null;
        }
        logger.debug("Trying to find Cloudian user with id=" + userId + " and group id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            checkResponseOK(response);
            if (checkEmptyResponse(response)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudianUser.class);
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian user due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }

    public List<CloudianUser> listUsers(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return new ArrayList<>();
        }
        logger.debug("Trying to list Cloudian users in group id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/user/list?groupId=%s&userType=all&userStatus=active", groupId));
            checkResponseOK(response);
            if (checkEmptyResponse(response)) {
                return new ArrayList<>();
            }
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), CloudianUser[].class));
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian users due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

    public boolean updateUser(final CloudianUser user) {
        if (user == null) {
            return false;
        }
        logger.debug("Updating Cloudian user: " + user);
        try {
            final HttpResponse response = post("/user", user);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to update Cloudian user due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean removeUser(final String userId, final String groupId) {
        if (StringUtils.isAnyEmpty(userId, groupId)) {
            return false;
        }
        logger.debug("Removing Cloudian user with user id=" + userId + " in group id=" + groupId);
        try {
            final HttpResponse response = delete(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to remove Cloudian user due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    /////////////////////////////////////////////////////////
    //////////////// Public APIs: Group /////////////////////
    /////////////////////////////////////////////////////////

    public boolean addGroup(final CloudianGroup group) {
        if (group == null) {
            return false;
        }
        logger.debug("Adding Cloudian group: " + group);
        try {
            final HttpResponse response = put("/group", group);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to add Cloudian group due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public CloudianGroup listGroup(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return null;
        }
        logger.debug("Trying to find Cloudian group with id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/group?groupId=%s", groupId));
            checkResponseOK(response);
            if (checkEmptyResponse(response)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudianGroup.class);
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian group due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }

    public List<CloudianGroup> listGroups() {
        logger.debug("Trying to list Cloudian groups");
        try {
            final HttpResponse response = get("/group/list");
            checkResponseOK(response);
            if (checkEmptyResponse(response)) {
                return new ArrayList<>();
            }
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), CloudianGroup[].class));
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian groups due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

    public boolean updateGroup(final CloudianGroup group) {
        if (group == null) {
            return false;
        }
        logger.debug("Updating Cloudian group: " + group);
        try {
            final HttpResponse response = post("/group", group);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to update group due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean removeGroup(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return false;
        }
        logger.debug("Removing Cloudian group id=" + groupId);
        try {
            final HttpResponse response = delete(String.format("/group?groupId=%s", groupId));
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to remove group due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }
}
