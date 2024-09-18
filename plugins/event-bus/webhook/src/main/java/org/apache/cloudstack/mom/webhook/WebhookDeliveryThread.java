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

package org.apache.cloudstack.mom.webhook;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebhookDeliveryThread implements Runnable {
    protected static Logger LOGGER = LogManager.getLogger(WebhookDeliveryThread.class);

    private static final String HEADER_X_CS_EVENT_ID = "X-CS-Event-ID";
    private static final String HEADER_X_CS_EVENT = "X-CS-Event";
    private static final String HEADER_X_CS_SIGNATURE = "X-CS-Signature";
    private static final String PREFIX_HEADER_USER_AGENT = "CS-Hookshot/";
    private final Webhook webhook;
    private final Event event;
    private CloseableHttpClient httpClient;
    private String headers;
    private String payload;
    private String response;
    private Date startTime;
    private int deliveryTries = 3;
    private int deliveryTimeout = 10;

    AsyncCompletionCallback<WebhookDeliveryResult> callback;

    protected boolean isValidJson(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException ex) {
            try {
                new JSONArray(json);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    protected void setHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (webhook.isSslVerification()) {
            httpClient = HttpClients.createDefault();
            return;
        }
        httpClient = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null,
                        TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
    }

    protected HttpPost getBasicHttpPostRequest() throws URISyntaxException {
        final URI uri = new URI(webhook.getPayloadUrl());
        HttpPost request = new HttpPost();
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(deliveryTimeout * 1000);
        requestConfig.setConnectionRequestTimeout(deliveryTimeout * 1000);
        requestConfig.setSocketTimeout(deliveryTimeout * 1000);
        request.setConfig(requestConfig.build());
        request.setURI(uri);
        return request;
    }

    protected void updateRequestHeaders(HttpPost request) throws DecoderException, NoSuchAlgorithmException,
            InvalidKeyException {
        request.addHeader(HEADER_X_CS_EVENT_ID, event.getEventUuid());
        request.addHeader(HEADER_X_CS_EVENT, event.getEventType());
        request.setHeader(HttpHeaders.USER_AGENT, String.format("%s%s", PREFIX_HEADER_USER_AGENT,
                event.getResourceAccountUuid()));
        if (StringUtils.isNotBlank(webhook.getSecretKey())) {
            request.addHeader(HEADER_X_CS_SIGNATURE, generateHMACSignature(payload, webhook.getSecretKey()));
        }
        List<Header> headers = new ArrayList<>(Arrays.asList(request.getAllHeaders()));
        HttpEntity entity = request.getEntity();
        if (entity.getContentLength() > 0 && !request.containsHeader(HttpHeaders.CONTENT_LENGTH)) {
            headers.add(new BasicHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(entity.getContentLength())));
        }
        if (entity.getContentType() != null && !request.containsHeader(HttpHeaders.CONTENT_TYPE)) {
            headers.add(entity.getContentType());
        }
        if (entity.getContentEncoding() != null && !request.containsHeader(HttpHeaders.CONTENT_ENCODING)) {
            headers.add(entity.getContentEncoding());
        }
        this.headers = StringUtils.join(headers, "\n");
    }

    public WebhookDeliveryThread(Webhook webhook, Event event,
                                 AsyncCompletionCallback<WebhookDeliveryResult> callback) {
        this.webhook = webhook;
        this.event = event;
        this.callback = callback;
    }

    public void setDeliveryTries(int deliveryTries) {
        this.deliveryTries = deliveryTries;
    }

    public void setDeliveryTimeout(int deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
    }

    @Override
    public void run() {
        LOGGER.debug("Delivering event: {} for {}", event.getEventType(), webhook);
        if (event == null) {
            LOGGER.warn("Invalid event received for delivering to {}", webhook);
            return;
        }
        payload = event.getDescription();
        LOGGER.trace("Payload: {}", payload);
        int attempt = 0;
        boolean success = false;
        try {
            setHttpClient();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            response = String.format("Failed to initiate delivery due to : %s", e.getMessage());
            callback.complete(new WebhookDeliveryResult(headers, payload, success, response, new Date()));
            return;
        }
        while (attempt < deliveryTries) {
            attempt++;
            if (delivery(attempt)) {
                success = true;
                break;
            }
        }
        callback.complete(new WebhookDeliveryResult(headers, payload, success, response, startTime));
    }

    protected void updateResponseFromRequest(HttpEntity entity) {
        try {
            this.response =  EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to parse response for event: {} for {}",
                    event.getEventType(), webhook);
            this.response = "";
        }
    }

    protected boolean delivery(int attempt) {
        startTime = new Date();
        try {
            HttpPost request = getBasicHttpPostRequest();
            StringEntity input = new StringEntity(payload,
                    isValidJson(payload) ? ContentType.APPLICATION_JSON : ContentType.TEXT_PLAIN);
            request.setEntity(input);
            updateRequestHeaders(request);
            LOGGER.trace("Delivering event: {} for {} with timeout: {}, " +
                            "attempt #{}", event.getEventType(), webhook,
                    deliveryTimeout, attempt);
            final CloseableHttpResponse response = httpClient.execute(request);
            updateResponseFromRequest(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOGGER.trace("Successfully delivered event: {} for {}",
                        event.getEventType(), webhook);
                return true;
            }
        } catch (URISyntaxException | IOException | DecoderException | NoSuchAlgorithmException |
                 InvalidKeyException e) {
            LOGGER.warn("Failed to deliver {}, in attempt #{} due to: {}",
                    webhook, attempt, e.getMessage());
            response = String.format("Failed due to : %s", e.getMessage());
        }
        return false;
    }

    public static String generateHMACSignature(String data,  String key)
            throws InvalidKeyException, NoSuchAlgorithmException, DecoderException {
        Mac mac = Mac.getInstance("HMACSHA256");
        SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
        mac.init(secretKey);
        byte[] dataAsBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encodedText = mac.doFinal(dataAsBytes);
        return new String(Base64.encodeBase64(encodedText)).trim();
    }

    public static class WebhookDeliveryContext<T> extends AsyncRpcContext<T> {
        private final Long eventId;
        private final Long ruleId;

        public WebhookDeliveryContext(AsyncCompletionCallback<T> callback, Long eventId, Long ruleId) {
            super(callback);
            this.eventId = eventId;
            this.ruleId = ruleId;
        }

        public Long getEventId() {
            return eventId;
        }

        public Long getRuleId() {
            return ruleId;
        }
    }

    public static class WebhookDeliveryResult extends CommandResult {
        private final String headers;
        private final String payload;
        private final Date starTime;
        private final Date endTime;

        public WebhookDeliveryResult(String headers, String payload, boolean success, String response, Date starTime) {
            super();
            this.headers = headers;
            this.payload = payload;
            this.setResult(response);
            this.setSuccess(success);
            this.starTime = starTime;
            this.endTime = new Date();
        }

        public String getHeaders() {
            return headers;
        }

        public String getPayload() {
            return payload;
        }

        public Date getStarTime() {
            return starTime;
        }

        public Date getEndTime() {
            return endTime;
        }
    }
}
