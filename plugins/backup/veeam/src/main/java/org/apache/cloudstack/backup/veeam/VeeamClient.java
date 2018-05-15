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

package org.apache.cloudstack.backup.veeam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
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
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;

public class VeeamClient {
    private static final Logger LOG = Logger.getLogger(VeeamClient.class);

    private final URI apiURI;
    private final HttpClient httpClient;
    private final HttpClientContext httpContext = HttpClientContext.create();
    private final CookieStore httpCookieStore = new BasicCookieStore();

    public VeeamClient(final String url, final String username, final String password, final boolean validateCertificate, final int timeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        this.apiURI = new URI(url);

        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        final HttpHost adminHost = new HttpHost(this.apiURI.getHost(), this.apiURI.getPort(), this.apiURI.getScheme());
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(adminHost, new BasicScheme());

        this.httpContext.setCredentialsProvider(provider);
        this.httpContext.setAuthCache(authCache);

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        if (!validateCertificate) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultCookieStore(httpCookieStore)
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultCookieStore(httpCookieStore)
                    .setDefaultRequestConfig(config)
                    .build();
        }

        try {
            final HttpResponse response = post("/sessionMngr/", null);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CloudRuntimeException("Failed to create and authenticate Veeam API client, please check the settings.");
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to authenticate Veeam API service due to:" + e.getMessage());
        }
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            final Credentials credentials = httpContext.getCredentialsProvider().getCredentials(AuthScope.ANY);
            LOG.error("Veeam API authentication failed, please check Veeam configuration. Admin auth principal=" + credentials.getUserPrincipal() + ", password=" + credentials.getPassword() + ", API url=" + apiURI.toString());
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "Veeam B&R API call unauthorized, please ask your administrator to fix integration issues.");
        }
    }

    private void checkResponseOK(final HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            LOG.debug("Requested Veeam resource does not exist");
            return;
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to find the requested resource and get valid response from Veeam B&R API call, please ask your administrator to diagnose and fix issues.");
        }
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Veeam API operation timed out, please try again.");
        }
    }

    private HttpResponse get(final String path) throws IOException {
        final HttpGet request = new HttpGet(apiURI.toString() + path);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse post(final String path, final Object item) throws IOException {
        final HttpPost request = new HttpPost(apiURI.toString() + path);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    //////////////////////////////////////////////////////////
    //////////////// Public APIs: Backup /////////////////////
    //////////////////////////////////////////////////////////

    public List<VeeamBackup> listAllBackups() {
        LOG.debug("Trying to list Veeam backups");
        try {
            final HttpResponse response = get("/backups");
            checkResponseOK(response);
            // FIXME: parse XML to object
            ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            response.getEntity().writeTo(outstream);
            byte [] responseBody = outstream.toByteArray();

            System.out.println(new String(responseBody));
            LOG.debug("Response received = " + response.getEntity().getContent());
            return null;
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam backups due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

    public List<VeeamBackup> listJobs() {
        LOG.debug("Trying to list Veeam jobs");
        try {
            final HttpResponse response = get("/jobs");
            checkResponseOK(response);

            // FIXME: parse XML to object

            return null;
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

}
