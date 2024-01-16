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

import org.apache.cloudstack.framework.events.Event;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class WebhookDispatchThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(WebhookDispatchThread.class);

    private final CloseableHttpClient httpClient;
    private WebhookRule rule;
    private Event event;
    private int dispatchRetries = 3;
    private int deliveryTimeout = 10;

    public WebhookDispatchThread(CloseableHttpClient httpClient, WebhookRule rule, Event event) {
        this.httpClient = httpClient;
        this.rule = rule;
        this.event = event;
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
        int attempt = 0;
        while (attempt < dispatchRetries) {
            attempt++;
            if (dispatch(attempt)) {
                break;
            }
        }
    }

    private boolean dispatch(int attempt) {
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
                LOGGER.debug(String.format("Dispatching event: %s for webhook: %s on URL: %s with timeout: %d, attempt #%d", event.getEventType(), rule.getName(), rule.getPayloadUrl(), deliveryTimeout, attempt));
            }
            if (event != null) {
                Gson gson = new Gson();
                String js = gson.toJson(event);
                StringEntity input = new StringEntity(js, ContentType.APPLICATION_JSON);
                request.setEntity(input);
            }
            final CloseableHttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.debug(String.format("Successfully dispatched event: %s for webhook: %s", event.getEventType(), rule.getName()));
                }
                return true;
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.warn(String.format("Failed to dispatch webhook: %s having URL: %s", rule.getName(), rule.getPayloadUrl()));
        }
        return false;
    }
}
