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
package org.apache.cloudstack.backup.backroll;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.utils.security.SSLUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONObject;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;

public class BackrollService {

    protected Logger logger = LogManager.getLogger(BackrollService.class);

    private String backrollToken = null;
    private boolean validateCertificate = false;
    private RequestConfig config = null;

    protected CloseableHttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException {
        if(!validateCertificate) {
            SSLContext sslContext = SSLUtils.getSSLContext();
            sslContext.init(null, new X509TrustManager[] { new TrustAllManager() }, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setSSLSocketFactory(factory)
                .build();
        } else {
            return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
        }
    }

    public void closeConnection(CloseableHttpResponse closeableHttpResponse) throws IOException {
        closeableHttpResponse.close();
    }

    protected CloseableHttpResponse post(final URI apiURI, final String path, final JSONObject json) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = createHttpClient();

        String xml = null;
        StringEntity params = null;
        if (json != null) {
            logger.debug("JSON {}", json.toString());
            params = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
        }

        String url = apiURI.toString() + path;
        final HttpPost request = new HttpPost(url);

        if (params != null) {
            request.setEntity(params);
        }

        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");

        final CloseableHttpResponse response = httpClient.execute(request);

        logger.debug("Response received in POST request with body {} is: {} for URL {}.", xml, response.toString(), url);

        return response;
    }

    protected CloseableHttpResponse get(final URI apiURI, String path) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = createHttpClient();

        String url = apiURI.toString() + path;
        logger.debug("Backroll URL {}", url);

        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");
        CloseableHttpResponse  response = httpClient.execute(request);
        logger.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);
        return response;
    }

    protected CloseableHttpResponse delete(final URI apiURI, String path) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = createHttpClient();

        String url = apiURI.toString() + path;
        final HttpDelete request = new HttpDelete(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");
        final CloseableHttpResponse response = httpClient.execute(request);
        logger.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);
        return response;
    }

    protected boolean isResponseAuthorized(final HttpResponse response) {
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                || response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED;
    }

    protected class NotOkBodyException extends Exception {
    }

    protected String okBody(final CloseableHttpResponse response)
            throws ParseException, IOException, NotOkBodyException {
        String result = "";
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_ACCEPTED:
                HttpEntity bodyEntity = response.getEntity();
                try {
                    result = EntityUtils.toString(bodyEntity);
                    EntityUtils.consumeQuietly(bodyEntity);
                    closeConnection(response);
                    return result;
                } finally {
                    EntityUtils.consumeQuietly(bodyEntity);
                }
            default:
                closeConnection(response);
                throw new NotOkBodyException();
        }
    }

    protected String waitGet(final URI apiURI, String url)
            throws IOException, InterruptedException, KeyManagementException, ParseException, NoSuchAlgorithmException {
        // int threshold = 30; // 5 minutes
        int maxAttempts = 12; // 2 minutes

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String body = okBody(get(apiURI, url));
                if (!body.contains(TaskState.PENDING)) {
                    return body;
                }
            } catch (final NotOkBodyException e) {
                throw new CloudRuntimeException("An error occured with Backroll");
            }
            TimeUnit.SECONDS.sleep(10);
        }

        return null;
    }
}