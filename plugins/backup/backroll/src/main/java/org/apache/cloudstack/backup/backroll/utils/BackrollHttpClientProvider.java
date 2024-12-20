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
package org.apache.cloudstack.backup.backroll.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.api.LoginApiResponse;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
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
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackrollHttpClientProvider {
    private URI apiURI;;
    private String backrollToken = null;
    private String appname = null;
    private String password = null;
    private RequestConfig config = null;
    private boolean validateCertificate = false;

    private Logger logger = LogManager.getLogger(BackrollClient.class);

    public static BackrollHttpClientProvider createProvider(BackrollHttpClientProvider backrollHttpClientProvider, final String url, final String appname, final String password,
        final boolean validateCertificate, final int timeout,
        final int restoreTimeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        backrollHttpClientProvider.apiURI = new URI(url);
        backrollHttpClientProvider.appname = appname;
        backrollHttpClientProvider.password = password;
        backrollHttpClientProvider.validateCertificate = validateCertificate;

        backrollHttpClientProvider.config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000)
            .build();

        return backrollHttpClientProvider;
    }

    protected CloseableHttpClient createHttpClient() throws BackrollApiException {
        if(!validateCertificate) {
            SSLContext sslContext;
            try {
                sslContext = SSLUtils.getSSLContext();
                sslContext.init(null, new X509TrustManager[] { new TrustAllManager() }, new SecureRandom());
                final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                return HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error(e);
                e.printStackTrace();
                throw new BackrollApiException();
            }
        } else {
            return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
        }
    }

    public <T> T post(final String path, final JSONObject json, Class<T> classOfT) throws BackrollApiException, IOException {

        loginIfAuthenticationFailed();

        try (CloseableHttpClient httpClient = createHttpClient()) {

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

            String result = okBody(response);

            T requestResponse = new ObjectMapper().readValue(result, classOfT);
            //response.close();

            return requestResponse;
        } catch (ParseException | NotOkBodyException e) {
            logger.error(e);
            e.printStackTrace();
            throw new BackrollApiException();
        }
    }

    public <T> T get(String path, Class<T> classOfT) throws IOException, BackrollApiException{
        logger.debug("Backroll Get Auth ");
        loginIfAuthenticationFailed();
        logger.debug("Backroll Get Auth ok");
        try (CloseableHttpClient httpClient = createHttpClient()) {

            String url = apiURI.toString() + path;
            logger.debug("Backroll URL {}", url);

            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
            request.setHeader("Content-type", "application/json");
            CloseableHttpResponse  response = httpClient.execute(request);
            logger.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);

            String result = okBody(response);
            T requestResponse = new ObjectMapper().readValue(result, classOfT);
            //response.close();

            return requestResponse;
        } catch (NotOkBodyException e) {
            logger.error(e);
            e.printStackTrace();
            throw new BackrollApiException();
        }
    }

    public String getWithoutParseResponse(String path) throws IOException, BackrollApiException{
        logger.debug("Backroll Get Auth ");
        loginIfAuthenticationFailed();
        logger.debug("Backroll Get Auth ok");
        try (CloseableHttpClient httpClient = createHttpClient()) {

            String url = apiURI.toString() + path;
            logger.debug("Backroll URL {}", url);

            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
            request.setHeader("Content-type", "application/json");
            CloseableHttpResponse  response = httpClient.execute(request);
            logger.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);

            String result = okBody(response);
            //response.close();

            return result;
        } catch (NotOkBodyException e) {
            logger.error(e);
            e.printStackTrace();
            throw new BackrollApiException();
        }
    }

    public <T> T delete(String path, Class<T> classOfT) throws IOException, BackrollApiException {

        loginIfAuthenticationFailed();

        try (CloseableHttpClient httpClient = createHttpClient()) {

            String url = apiURI.toString() + path;
            final HttpDelete request = new HttpDelete(url);
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
            request.setHeader("Content-type", "application/json");
            final CloseableHttpResponse response = httpClient.execute(request);
            logger.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);

            String result = okBody(response);
            T requestResponse = new ObjectMapper().readValue(result, classOfT);
            //response.close();

            return requestResponse;
        } catch (NotOkBodyException e) {
            logger.error(e);
            e.printStackTrace();
            throw new BackrollApiException();
        }
    }

    public class NotOkBodyException extends Exception {
    }

    public String okBody(final CloseableHttpResponse response) throws NotOkBodyException {
        String result = "";
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_ACCEPTED:
                HttpEntity bodyEntity = response.getEntity();
                try {
                    logger.debug("bodyentity : {}", bodyEntity);
                    result = EntityUtils.toString(bodyEntity);
                    logger.debug("bodyentity : result {}", result);
                    EntityUtils.consumeQuietly(bodyEntity);
                    return result;
                } catch (ParseException | IOException e) {
                    e.printStackTrace();
                    throw new NotOkBodyException();
                } finally {
                    EntityUtils.consumeQuietly(bodyEntity);
                }
            default:
                try {
                    closeConnection(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                throw new NotOkBodyException();
        }
    }

    public  <T> T waitGet(String url, Class<T> classOfT)  throws IOException, BackrollApiException {
        // int threshold = 30; // 5 minutes
        int maxAttempts = 12; // 2 minutes

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String body = getWithoutParseResponse(url);
            if(!StringUtils.isEmpty(body)){
                if (!body.contains(TaskState.PENDING)) {
                    T result = new ObjectMapper().readValue(body, classOfT);
                    return result;
                }
            }

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.error(e);
                e.printStackTrace();
                throw new BackrollApiException();
            }
        }

        return null;
    }

    public String waitGetWithoutParseResponse(String url)
            throws IOException, BackrollApiException {
        // int threshold = 30; // 5 minutes
        int maxAttempts = 12; // 2 minutes

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            String body = getWithoutParseResponse(url);
            if (!body.contains(TaskState.PENDING)) {
                logger.debug("waitGetWithoutParseResponse : result {}", body);
                return body;
            }

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.error(e);
                e.printStackTrace();
                throw new BackrollApiException();
            }
        }
        return null;
    }

    private boolean isAuthenticated() throws BackrollApiException, IOException {
        boolean result = false;

        if(StringUtils.isEmpty(backrollToken)) {
            logger.debug("Backroll Tocken empty : " + backrollToken);
            return result;
        }

        try (CloseableHttpClient httpClient = createHttpClient()) {
            final HttpPost request = new HttpPost(apiURI.toString() + "/auth");
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            logger.debug("Backroll Auth response : " + EntityUtils.toString(httpResponse.getEntity()));
            logger.debug("Backroll Auth response status : " + httpResponse.getStatusLine().getStatusCode());
            String response = okBody(httpResponse);
            logger.debug("Backroll Auth response 2 : " + response);
            logger.debug("Backroll Auth ok");
            result = true;
        }catch (NotOkBodyException e) {
            logger.error(e);
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private void closeConnection(CloseableHttpResponse closeableHttpResponse) throws IOException {
        closeableHttpResponse.close();
    }

    public void loginIfAuthenticationFailed() throws BackrollApiException, IOException {
        if (!isAuthenticated()) {
            login(appname, password);
        }
    }

    private void login(final String appname, final String appsecret) throws BackrollApiException, IOException {
        logger.debug("Backroll client -  start login");

        CloseableHttpClient httpClient = createHttpClient();
        CloseableHttpResponse httpResponse = null;

        final HttpPost request = new HttpPost(apiURI.toString() + "/login");
        request.addHeader("content-type", "application/json");

        JSONObject jsonBody = new JSONObject();
        StringEntity params;

        try {
            jsonBody.put("app_id", appname);
            jsonBody.put("app_secret", appsecret);
            params = new StringEntity(jsonBody.toString());
            request.setEntity(params);

            httpResponse = httpClient.execute(request);
            try {
                String response = okBody(httpResponse);
                ObjectMapper objectMapper = new ObjectMapper();
                logger.info("BACKROLL:     " + response);
                LoginApiResponse loginResponse = objectMapper.readValue(response, LoginApiResponse.class);
                logger.info("ok");
                this.backrollToken = loginResponse.accessToken;
                logger.debug("Backroll client -  Token : {}", backrollToken);

                if (StringUtils.isEmpty(loginResponse.accessToken)) {
                    throw new CloudRuntimeException("Backroll token is not available to perform API requests");
                }
            } catch (final NotOkBodyException e) {
                logger.error(e);
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                    throw new CloudRuntimeException("Failed to create and authenticate Backroll client, please check the settings.");
                } else {
                    throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "Backroll API call unauthorized, please ask your administrator to fix integration issues.");
                }
            }
        } catch (final IOException e) {
            logger.error(e);
            throw new CloudRuntimeException("Failed to authenticate Backroll API service due to:" + e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error(e);
            throw new CloudRuntimeException("Failed to authenticate Backroll API service due to:" + e.getMessage());
        }
        finally {
            closeConnection(httpResponse);
        }
        logger.debug("Backroll client -  end login");
    }
}
