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

package org.apache.cloudstack.dns.powerdns;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PowerDnsClient implements AutoCloseable {
    public static final Logger logger = LoggerFactory.getLogger(PowerDnsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TIMEOUT_MS = 5000;
    private final CloseableHttpClient httpClient;

    public void validate(String baseUrl, String apiKey) {
        String checkUrl = buildApiUrl(baseUrl, "/api/v1/servers");
        HttpGet request = new HttpGet(checkUrl);
        request.addHeader("X-API-Key", apiKey);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity())
                    : null;

            if (statusCode == HttpStatus.SC_OK) {
                JsonNode root = MAPPER.readTree(body);

                if (!root.isArray() || root.isEmpty()) {
                    throw new CloudRuntimeException("No servers returned by PowerDNS API");
                }

                boolean authoritativeFound = false;
                for (JsonNode node : root) {
                    if ("authoritative".equalsIgnoreCase(node.path("daemon_type").asText(null))) {
                        authoritativeFound = true;
                        break;
                    }
                }

                if (!authoritativeFound) {
                    throw new CloudRuntimeException("No authoritative PowerDNS server found");
                }

            } else if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                throw new CloudRuntimeException("Invalid PowerDNS API key");
            } else {
                logger.debug("Unexpected PowerDNS response: HTTP {} Body: {}", statusCode, body);
                throw new CloudRuntimeException(String.format("PowerDNS validation failed with HTTP %d", statusCode));
            }

        } catch (IOException ex) {
            throw new CloudRuntimeException("Failed to connect to PowerDNS", ex);
        }
    }

    public void createZone(String baseUrl, String apiKey, String zoneName, List<String> nameservers) {
        String url = buildApiUrl(baseUrl, "/servers/localhost/zones");
        ObjectNode json = MAPPER.createObjectNode();
        json.put("name", zoneName.endsWith(".") ? zoneName : zoneName + ".");
        json.put("kind", "Native");
        json.put("dnssec", false);

        if (nameservers != null && !nameservers.isEmpty()) {
            ArrayNode nsArray = json.putArray("nameservers");
            for (String ns : nameservers) {
                nsArray.add(ns.endsWith(".") ? ns : ns + ".");
            }
        }

        logger.debug("Creating PowerDNS zone: {} using URL: {}", zoneName, url);

        HttpPost request = new HttpPost(url);
        request.addHeader("X-API-Key", apiKey);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");

        try {
            request.setEntity(new StringEntity(json.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : null;

                if (statusCode == HttpStatus.SC_CREATED) {
                    logger.debug("Zone {} created successfully", zoneName);
                    return;
                }

                if (statusCode == HttpStatus.SC_CONFLICT) {
                    throw new CloudRuntimeException("Zone already exists: " + zoneName);
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED ||
                        statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                logger.debug("Unexpected PowerDNS response: HTTP {} Body: {}", statusCode, body);

                throw new CloudRuntimeException(String.format("Failed to create zone %s (HTTP %d)", zoneName, statusCode));
            }

        } catch (IOException e) {
            throw new CloudRuntimeException("Error while creating PowerDNS zone " + zoneName, e);
        }
    }


    public PowerDnsClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MS)
                .setConnectionRequestTimeout(TIMEOUT_MS)
                .setSocketTimeout(TIMEOUT_MS)
                .build();

        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .disableCookieManagement()
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("PowerDNS base URL cannot be null");
        }
        String normalizedUrl = baseUrl.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            normalizedUrl = "http://" + normalizedUrl;
        }
        if (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        return normalizedUrl;
    }

    private String buildApiUrl(String baseUrl, String path) {
        return normalizeBaseUrl(baseUrl) + "/api/v1" + path;
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.warn("Failed to close PowerDNS HTTP client", e);
        }
    }
}
