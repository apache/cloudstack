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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import com.cloud.event.EventCategory;
import com.google.gson.Gson;

public class WebhookDispatchThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(WebhookDispatchThread.class);

    private final CloseableHttpClient httpClient;
    private WebhookRule rule;
    private Event event;
    private String payload;
    private String response;
    private Date startTime;
    private int dispatchRetries = 3;
    private int deliveryTimeout = 10;

    AsyncCompletionCallback<WebhookDispatchResult> callback;

    public WebhookDispatchThread(CloseableHttpClient httpClient, WebhookRule rule, Event event,AsyncCompletionCallback<WebhookDispatchResult> callback) {
        this.httpClient = httpClient;
        this.rule = rule;
        this.event = event;
        this.callback = callback;
    }

    public void setDispatchRetries(int dispatchRetries) {
        this.dispatchRetries = dispatchRetries;
    }

    public void setDeliveryTimeout(int deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
    }

    @Override
    public void run() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Dispatching event: %s for webhook: %s", event.getEventType(), rule.getName()));
        }
        if (event == null) {
            LOGGER.warn(String.format("Invalid event received for dispatching webhook: %s", rule.getName()));
            return;
        }
        Gson gson = new Gson();
        payload = gson.toJson(event);
        int attempt = 0;
        boolean success = false;
        while (attempt < dispatchRetries) {
            attempt++;
            if (dispatch(attempt)) {
                success = true;
                break;
            }
        }
        callback.complete(new WebhookDispatchResult(payload, success, response, startTime));
    }

    protected void updateResponseFromRequest(InputStream is) {
        try {
            this.response =  IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to parse response for event: %s, webhook: %s having URL: %s", event.getEventType(), rule.getName(), rule.getPayloadUrl()));
        }
    }

    protected boolean dispatch(int attempt) {
        startTime = new Date();
        try {
            final URI uri = new URI(rule.getPayloadUrl());
            HttpPost request = new HttpPost();
            RequestConfig.Builder requestConfig = RequestConfig.custom();
            requestConfig.setConnectTimeout(deliveryTimeout * 1000);
            requestConfig.setConnectionRequestTimeout(deliveryTimeout * 1000);
            requestConfig.setSocketTimeout(deliveryTimeout * 1000);
            request.setConfig(requestConfig.build());
            request.setURI(uri);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Dispatching event: %s for webhook: %s on URL: %s with timeout: %d, attempt #%d", event.getEventType(), rule.getName(), rule.getPayloadUrl(), deliveryTimeout, attempt));
            }
            StringEntity input = new StringEntity(payload, ContentType.APPLICATION_JSON);
            request.setEntity(input);

            if (!"RANDOM".equals(this.event.getEventCategory())) { //ToDo: Remove this debug
                if (EventCategory.ACTION_EVENT.getName().equals(this.event.getEventCategory())) {
                    LOGGER.info(String.format("Successfully dispatched event: %s for webhook: %s", payload, rule.getName()));
                    response = "Success";
                    return true;
                }
                response = "DEBUG";
                return false;
            }

            final CloseableHttpResponse response = httpClient.execute(request);
            updateResponseFromRequest(response.getEntity().getContent());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Successfully dispatched event: %s for webhook: %s", event.getEventType(), rule.getName()));
                }
                return true;
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.warn(String.format("Failed to dispatch webhook: %s having URL: %s, in attempt #%d due to: %s",
                    rule.getName(), rule.getPayloadUrl(), attempt, e.getMessage()));
            response = String.format("Failed due to : %s", e.getMessage());
        }
        return false;
    }

    public static class WebhookDispatchContext<T> extends AsyncRpcContext<T> {
        private final WebhookRule rule;

        public WebhookDispatchContext(AsyncCompletionCallback<T> callback, WebhookRule rule) {
            super(callback);
            this.rule = rule;
        }

        public WebhookRule getRule() {
            return rule;
        }
    }

    public class WebhookDispatchResult extends CommandResult {
        private String payload;
        private Date starTime;
        private Date endTime;

        public WebhookDispatchResult(String payload, boolean success, String response, Date starTime) {
            super();
            this.payload = payload;
            this.setResult(response);
            this.setSuccess(success);
            this.starTime = starTime;
            this.endTime = new Date();
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
