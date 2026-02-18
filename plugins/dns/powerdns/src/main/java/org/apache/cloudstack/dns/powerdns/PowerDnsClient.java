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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.utils.StringUtils;
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
        String checkUrl = buildApiUrl(baseUrl, "/servers");
        HttpGet request = new HttpGet(checkUrl);
        request.addHeader("X-API-Key", apiKey);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

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

    public String createZone(String baseUrl, String apiKey, String zoneName, String nameServers) {
        String normalizedZone = formatZoneName(zoneName);
        try {
            String url = buildApiUrl(baseUrl, "/servers/localhost/zones");
            ObjectNode json = MAPPER.createObjectNode();
            json.put("name", normalizedZone);
            json.put("kind", "Native");
            json.put("dnssec", false);

            if (StringUtils.isNotEmpty(nameServers)) {
                List<String> nsNames = new ArrayList<>(Arrays.asList(nameServers.split(",")));
                if (!CollectionUtils.isEmpty(nsNames)) {
                    ArrayNode nsArray = json.putArray("nameservers");
                    for (String ns : nsNames) {
                        nsArray.add(ns.endsWith(".") ? ns : ns + ".");
                    }
                }
            }
            HttpPost request = new HttpPost(url);
            request.addHeader("X-API-Key", apiKey);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.setEntity(new StringEntity(json.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

                if (statusCode == HttpStatus.SC_CREATED) {
                    JsonNode root = MAPPER.readTree(body);
                    String zoneId = root.path("id").asText();
                    if (StringUtils.isBlank(zoneId)) {
                        throw new CloudRuntimeException("PowerDNS returned empty zone id");
                    }
                    return zoneId;
                }

                if (statusCode == HttpStatus.SC_CONFLICT) {
                    throw new CloudRuntimeException("Zone already exists: " + zoneName);
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                logger.debug("Unexpected PowerDNS response: HTTP {} Body: {}", statusCode, body);
                throw new CloudRuntimeException(String.format("Failed to create zone %s (HTTP %d)", zoneName, statusCode));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("Error while creating PowerDNS zone " + zoneName, e);
        }
    }

    public void deleteZone(String baseUrl, String apiKey, String zoneName) {
        String normalizedZone = formatZoneName(zoneName);
        try {
            String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
            String url = buildApiUrl(baseUrl, "/servers/localhost/zones/" + encodedZone);
            HttpDelete request = new HttpDelete(url);
            request.addHeader("X-API-Key", apiKey);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    logger.debug("Zone {} deleted successfully", normalizedZone);
                    return;
                }

                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    logger.debug("Zone {} not found in PowerDNS", normalizedZone);
                    return;
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                logger.debug("Unexpected PowerDNS response while deleting zone: HTTP {} Body: {}", statusCode, body);
                throw new CloudRuntimeException(String.format("Failed to delete zone %s (HTTP %d)", normalizedZone, statusCode));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("Error while deleting PowerDNS zone " + zoneName, e);
        }
    }

    public void modifyRecord(String baseUrl, String apiKey, String zoneName, String recordName, String type, long ttl, List<String> contents, String changeType) {
        String normalizedZone = formatZoneName(zoneName);
        String normalizedRecord = formatRecordName(recordName, zoneName);

        try {
            String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
            String url = buildApiUrl(baseUrl, "/servers/localhost/zones/" + encodedZone);

            ObjectNode root = MAPPER.createObjectNode();
            ArrayNode rrsets = root.putArray("rrsets");
            ObjectNode rrset = rrsets.addObject();

            rrset.put("name", normalizedRecord);
            rrset.put("type", type.toUpperCase());
            rrset.put("ttl", ttl);
            rrset.put("changetype", changeType);

            ArrayNode records = rrset.putArray("records");
            if (!CollectionUtils.isEmpty(contents)) {
                for (String content : contents) {
                    ObjectNode record = records.addObject();
                    record.put("content", content);
                    record.put("disabled", false);
                }
            }

            HttpPatch request = new HttpPatch(url);
            request.addHeader("X-API-Key", apiKey);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.setEntity(new StringEntity(root.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    logger.debug("Record {} {} added/updated in zone {}", normalizedRecord, type, normalizedZone);
                    return;
                }

                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    throw new CloudRuntimeException("Zone not found: " + normalizedZone);
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                logger.debug("Unexpected PowerDNS response: HTTP {} Body: {}", statusCode, body);
                throw new CloudRuntimeException("Failed to add/update record " + normalizedRecord);
            }

        } catch (IOException e) {
            throw new CloudRuntimeException("Error while adding PowerDNS record", e);
        }
    }

    public void deleteRecord(String baseUrl, String apiKey, String zoneName, String recordName, String type) {

        String normalizedZone = formatZoneName(zoneName);
        String normalizedRecord = formatRecordName(recordName, zoneName);

        try {
            String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
            String url = buildApiUrl(baseUrl, "/servers/localhost/zones/" + encodedZone);

            ObjectNode root = MAPPER.createObjectNode();
            ArrayNode rrsets = root.putArray("rrsets");
            ObjectNode rrset = rrsets.addObject();

            rrset.put("name", normalizedRecord);
            rrset.put("type", type.toUpperCase());
            rrset.put("changetype", "DELETE");

            HttpPatch request = new HttpPatch(url);
            request.addHeader("X-API-Key", apiKey);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.setEntity(new StringEntity(root.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    logger.debug("Record {} {} deleted", normalizedRecord, type);
                    return;
                }

                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    logger.debug("Record {} {} not found (idempotent delete)", normalizedRecord, type);
                    return;
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                logger.debug("Unexpected PowerDNS response: HTTP {} Body: {}", statusCode, body);
                throw new CloudRuntimeException("Failed to delete record " + normalizedRecord);
            }

        } catch (IOException e) {
            throw new CloudRuntimeException("Error while deleting PowerDNS record", e);
        }
    }

    public Iterable<JsonNode> listRecords(String baseUrl, String apiKey, String zoneName) {
        String normalizedZone = formatZoneName(zoneName);
        try {
            String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
            String url = buildApiUrl(baseUrl, "/servers/localhost/zones/" + encodedZone);

            HttpGet request = new HttpGet(url);
            request.addHeader("X-API-Key", apiKey);
            request.addHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

                if (statusCode == HttpStatus.SC_OK) {
                    JsonNode zone = MAPPER.readTree(body);
                    JsonNode rrsets = zone.path("rrsets");

                    if (rrsets.isArray()) {
                        return rrsets;
                    }

                    return Collections.emptyList();
                }

                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    throw new CloudRuntimeException("Zone not found: " + normalizedZone);
                }

                if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                    throw new CloudRuntimeException("Invalid PowerDNS API key");
                }

                throw new CloudRuntimeException("Failed to list records for zone " + normalizedZone);
            }

        } catch (IOException e) {
            throw new CloudRuntimeException("Error while listing PowerDNS records", e);
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

    private String formatZoneName(String zoneName) {
        String zone = zoneName.trim().toLowerCase();
        if (!zone.endsWith(".")) {
            zone += ".";
        }
        return zone;
    }

    private String formatRecordName(String recordName, String zoneName) {
        if (recordName == null) {
            throw new IllegalArgumentException("Record name cannot be null");
        }
        String normalizedZone = formatZoneName(zoneName);
        String zoneWithoutDot = normalizedZone.substring(0, normalizedZone.length() - 1);

        String name = recordName.trim().toLowerCase();

        // Root record
        if (name.equals("@") || name.isEmpty()) {
            return normalizedZone;
        }

        // Already absolute
        if (name.endsWith(".")) {
            return name;
        }

        // Fully qualified but missing trailing dot
        if (name.equals(zoneWithoutDot) || name.endsWith("." + zoneWithoutDot)) {
            return name + ".";
        }

        // Relative name
        return name + "." + normalizedZone;
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
