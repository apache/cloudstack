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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.exception.DnsAuthenticationException;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.cloudstack.dns.exception.DnsOperationException;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.apache.cloudstack.dns.exception.DnsTransportException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PowerDnsClient implements AutoCloseable {
    public static final Logger logger = LoggerFactory.getLogger(PowerDnsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MS = 10_000;
    private static final int MAX_CONNECTIONS_TOTAL = 50;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final String API_PREFIX = "/api/v1";
    private static final String DEFAULT_SERVER = "localhost";

    private final CloseableHttpClient httpClient;

    public PowerDnsClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_CONNECTIONS_TOTAL);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();

        this.httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .disableCookieManagement()
                .build();
    }

    public void validate(String baseUrl, String apiKey) throws DnsProviderException {
        String url = buildUrl(baseUrl, "/servers");
        HttpGet request = new HttpGet(url);
        JsonNode servers = execute(request, apiKey, 200);
        if (servers == null || !servers.isArray() || servers.isEmpty()) {
            throw new DnsOperationException("No servers returned by PowerDNS API");
        }
        boolean authoritativeFound = false;
        for (JsonNode server : servers) {
            if (ApiConstants.AUTHORITATIVE.equalsIgnoreCase(server.path("daemon_type").asText(null))) {
                authoritativeFound = true;
                break;
            }
        }
        if (!authoritativeFound) {
            throw new DnsOperationException("No authoritative PowerDNS server found");
        }
    }

    public String createZone(String baseUrl, String apiKey, String zoneName, String zoneKind, boolean dnsSecFlag,
                             List<String> nameServers) throws DnsProviderException {

        validate(baseUrl, apiKey);

        String normalizedZone = normalizeZone(zoneName);
        ObjectNode json = MAPPER.createObjectNode();
        json.put(ApiConstants.NAME, normalizedZone);
        json.put(ApiConstants.KIND, zoneKind);
        json.put(ApiConstants.DNS_SEC, dnsSecFlag);
        if (!CollectionUtils.isEmpty(nameServers)) {
            ArrayNode nsArray = json.putArray(ApiConstants.NAME_SERVERS);
            for (String ns : nameServers) {
                nsArray.add(ns.endsWith(".") ? ns : ns + ".");
            }
        }
        HttpPost request = new HttpPost(buildUrl(baseUrl, "/servers/" + DEFAULT_SERVER + "/zones"));
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
        JsonNode response = execute(request, apiKey, 201);
        if (response == null) {
            throw new DnsOperationException("Empty response from DNS server");
        }
        String zoneId = response.path(ApiConstants.ID).asText();
        if (StringUtils.isBlank(zoneId)) {
            throw new DnsOperationException("PowerDNS returned empty zone id");
        }
        return zoneId;
    }

    public void updateZone(String baseUrl, String apiKey, String zoneName, String zoneKind, Boolean dnsSecFlag,
                           List<String> nameServers) throws DnsProviderException {

        String normalizedZone = normalizeZone(zoneName);
        String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
        String url = buildUrl(baseUrl, "/servers/" + DEFAULT_SERVER + "/zones/" + encodedZone);

        ObjectNode json = MAPPER.createObjectNode();

        if (dnsSecFlag != null) {
            json.put(ApiConstants.DNS_SEC, dnsSecFlag);
        }
        if (StringUtils.isNotBlank(zoneKind)) {
            json.put(ApiConstants.KIND, zoneKind);
        }
        if (!CollectionUtils.isEmpty(nameServers)) {
            ArrayNode nsArray = json.putArray(ApiConstants.NAME_SERVERS);
            for (String ns : nameServers) {
                nsArray.add(ns.endsWith(".") ? ns : ns + ".");
            }
        }
        HttpPatch request = new HttpPatch(url);
        request.setEntity(new org.apache.http.entity.StringEntity(json.toString(), StandardCharsets.UTF_8));
        execute(request, apiKey, 204);
    }

    public void deleteZone(String baseUrl, String apiKey, String zoneName) throws DnsProviderException {
        validate(baseUrl, apiKey);
        String normalizedZone = normalizeZone(zoneName);
        String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
        HttpDelete request = new HttpDelete(buildUrl(baseUrl, "/servers/" + DEFAULT_SERVER + "/zones/" + encodedZone));
        execute(request, apiKey, 204, 404);
    }

    public String modifyRecord(String baseUrl, String apiKey, String zoneName, String recordName, String type, long ttl,
                             List<String> contents, String changeType) throws DnsProviderException {

        validate(baseUrl, apiKey);
        String normalizedZone = normalizeZone(zoneName);
        String normalizedRecord = normalizeRecordName(recordName, normalizedZone);
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode rrsets = root.putArray(ApiConstants.RR_SETS);
        ObjectNode rrset = rrsets.addObject();
        rrset.put(ApiConstants.NAME, normalizedRecord);
        rrset.put(ApiConstants.TYPE, type.toUpperCase());
        rrset.put(ApiConstants.TTL, ttl);
        rrset.put(ApiConstants.CHANGE_TYPE, changeType);
        ArrayNode records = rrset.putArray(ApiConstants.RECORDS);
        if (!CollectionUtils.isEmpty(contents)) {
            for (String content : contents) {
                ObjectNode record = records.addObject();
                record.put(ApiConstants.CONTENT, content);
                record.put(ApiConstants.DISABLED, false);
            }
        }
        String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
        HttpPatch request = new HttpPatch(buildUrl(baseUrl, "/servers/" + DEFAULT_SERVER + "/zones/" + encodedZone));
        request.setEntity(new org.apache.http.entity.StringEntity(root.toString(), StandardCharsets.UTF_8));
        execute(request, apiKey, 204);
        return normalizedRecord;
    }

    public Iterable<JsonNode> listRecords(String baseUrl, String apiKey, String zoneName) throws DnsProviderException {
        validate(baseUrl, apiKey);
        String normalizedZone = normalizeZone(zoneName);
        String encodedZone = URLEncoder.encode(normalizedZone, StandardCharsets.UTF_8);
        HttpGet request = new HttpGet(buildUrl(baseUrl, "/servers/" + DEFAULT_SERVER + "/zones/" + encodedZone));
        JsonNode zoneNode = execute(request, apiKey, 200);
        if (zoneNode == null || !zoneNode.has(ApiConstants.RR_SETS)) {
            return Collections.emptyList();
        }
        JsonNode rrsets = zoneNode.path(ApiConstants.RR_SETS);
        return rrsets.isArray() ? rrsets : Collections.emptyList();
    }

    private JsonNode execute(HttpUriRequest request, String apiKey, int... expectedStatus) throws DnsProviderException {
        request.addHeader(ApiConstants.X_API_KEY, apiKey);
        request.addHeader("Accept", "application/json");
        request.addHeader(ApiConstants.CONTENT_TYPE, "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;

            for (int expected : expectedStatus) {
                if (status == expected) {
                    if (body != null && !body.isEmpty()) {
                        return MAPPER.readTree(body);
                    } else {
                        return null;
                    }
                }
            }
            if (status == 404) {
                throw new DnsNotFoundException("Resource not found: " + body);
            } else if (status == 401 || status == 403) {
                throw new DnsAuthenticationException("Invalid API key");
            } else if (status == 409) {
                throw new DnsConflictException("Conflict: " + body);
            }
            throw new DnsOperationException("Unexpected PowerDNS response: HTTP " + status + " Body: " + body);
        } catch (IOException ex) {
            throw new DnsTransportException("Error communicating with PowerDNS", ex);
        }
    }

    private String buildUrl(String baseUrl, String path) {
        return normalizeBaseUrl(baseUrl) + API_PREFIX + path;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL cannot be null");
        }
        String url = baseUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String normalizeZone(String zoneName) {
        if (StringUtils.isBlank(zoneName)) {
            throw new IllegalArgumentException("Zone name must not be null or empty");
        }
        String zone = zoneName.trim().toLowerCase();
        if (!zone.endsWith(".")) {
            zone = zone + ".";
        }
        if (zone.length() < 2) {
            throw new IllegalArgumentException("Zone name is too short");
        }
        return zone;
    }

    String normalizeRecordName(String recordName, String zoneName) {
        if (recordName == null) {
            throw new IllegalArgumentException("Record name must not be null");
        }
        String normalizedZone = normalizeZone(zoneName);
        String name = recordName.trim().toLowerCase();
        // Apex of the zone
        if (name.equals("@") || name.isEmpty()) {
            return normalizedZone;
        }

        String zoneWithoutDot = normalizedZone.substring(0, normalizedZone.length() - 1);
        // Already absolute (ends with dot)
        if (name.endsWith(".")) {
            // Check if the record belongs to the zone
            if (!name.equals(normalizedZone) && !name.endsWith("." + zoneWithoutDot + ".")) {
                throw new IllegalArgumentException(
                        String.format("Record '%s' does not belong to zone '%s'", recordName, zoneName)
                );
            }
            return name;
        }
        if (name.contains(".")) {
            return name + ".";
        }
        // Relative name → append zone
        return name + "." + normalizedZone;
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
