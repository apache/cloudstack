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

package org.apache.cloudstack.dns.bunny;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.dns.exception.DnsAuthenticationException;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.cloudstack.dns.exception.DnsOperationException;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.apache.cloudstack.dns.exception.DnsTransportException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Thin client for the Bunny.net DNS API (https://api.bunny.net).
 *
 * Unlike PowerDNS, Bunny has no rrset concept: every value (e.g. every A record under the same
 * name) is its own record with its own numeric id, and zones/records are only addressable by that
 * id. Since CloudStack's DnsZone/DnsRecord abstractions carry neither Bunny's zone id nor its
 * record ids, this client resolves them by name on every call (mirroring how the PowerDNS plugin
 * re-resolves its server id per call).
 */
public class BunnyDnsClient implements AutoCloseable {
    public static final Logger logger = LoggerFactory.getLogger(BunnyDnsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MS = 10_000;
    private static final int MAX_CONNECTIONS_TOTAL = 50;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final int PAGE_SIZE = 1000;

    public static final String DEFAULT_BASE_URL = "https://api.bunny.net";
    private static final String ACCESS_KEY_HEADER = "AccessKey";

    /** Bunny's integer DnsRecordTypes, limited to the types DnsRecord.RecordType models. */
    private static final Map<String, Integer> TYPE_TO_CODE = new HashMap<>();
    private static final Map<Integer, String> CODE_TO_TYPE = new HashMap<>();
    static {
        putType("A", 0);
        putType("AAAA", 1);
        putType("CNAME", 2);
        putType("TXT", 3);
        putType("MX", 4);
        putType("SRV", 8);
        putType("PTR", 10);
        putType("NS", 12);
    }

    private static void putType(String name, int code) {
        TYPE_TO_CODE.put(name, code);
        CODE_TO_TYPE.put(code, name);
    }

    private final CloseableHttpClient httpClient;

    public BunnyDnsClient() {
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

    public static int typeCode(String type) {
        Integer code = TYPE_TO_CODE.get(type.toUpperCase());
        if (code == null) {
            throw new IllegalArgumentException("Unsupported Bunny DNS record type: " + type);
        }
        return code;
    }

    public static String typeName(int code) {
        return CODE_TO_TYPE.get(code);
    }

    public void validateAccess(String baseUrl, Integer port, String apiKey) throws DnsProviderException {
        HttpGet request = new HttpGet(buildUrl(baseUrl, port, "/dnszone?perPage=5&page=1"));
        execute(request, apiKey, 200);
    }

    public long resolveZoneId(String baseUrl, Integer port, String apiKey, String domain) throws DnsProviderException {
        String normalizedDomain = normalizeDomain(domain);
        int page = 1;
        while (true) {
            String urlPath = "/dnszone?search=" + URLEncoder.encode(normalizedDomain, StandardCharsets.UTF_8)
                    + "&perPage=" + PAGE_SIZE + "&page=" + page;
            HttpGet request = new HttpGet(buildUrl(baseUrl, port, urlPath));
            JsonNode response = execute(request, apiKey, 200);
            JsonNode items = response != null ? response.path("Items") : null;
            if (items != null && items.isArray()) {
                for (JsonNode zone : items) {
                    if (normalizedDomain.equalsIgnoreCase(zone.path("Domain").asText(null))) {
                        return zone.path("Id").asLong();
                    }
                }
            }
            if (response == null || !response.path("HasMoreItems").asBoolean(false)) {
                break;
            }
            page++;
        }
        throw new DnsNotFoundException("Bunny DNS zone not found for domain: " + domain);
    }

    public boolean zoneExists(String baseUrl, Integer port, String apiKey, String domain) {
        try {
            resolveZoneId(baseUrl, port, apiKey, domain);
            return true;
        } catch (DnsProviderException | IllegalArgumentException e) {
            return false;
        }
    }

    /** The response body of Bunny's Add DNS Zone call is undocumented, hence the lookup when it carries no id. */
    public long createZone(String baseUrl, Integer port, String apiKey, String domain) throws DnsProviderException {
        String normalizedDomain = normalizeDomain(domain);
        ObjectNode json = MAPPER.createObjectNode();
        json.put("Domain", normalizedDomain);
        HttpPost request = new HttpPost(buildUrl(baseUrl, port, "/dnszone"));
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
        JsonNode response = execute(request, apiKey, 201);
        if (response != null && response.hasNonNull("Id")) {
            return response.path("Id").asLong();
        }
        return resolveZoneId(baseUrl, port, apiKey, normalizedDomain);
    }

    public void deleteZone(String baseUrl, Integer port, String apiKey, String domain) throws DnsProviderException {
        long zoneId;
        try {
            zoneId = resolveZoneId(baseUrl, port, apiKey, domain);
        } catch (DnsNotFoundException e) {
            return;
        }
        HttpDelete request = new HttpDelete(buildUrl(baseUrl, port, "/dnszone/" + zoneId));
        execute(request, apiKey, 204, 404);
    }

    public String addRecord(String baseUrl, Integer port, String apiKey, long zoneId, String zoneDomain,
                            String recordName, String type, int ttl, String content) throws DnsProviderException {
        String bunnyName = toBunnyRecordName(recordName, zoneDomain);
        ObjectNode body = buildRecordBody(bunnyName, type, ttl, content);
        HttpPut request = new HttpPut(buildUrl(baseUrl, port, "/dnszone/" + zoneId + "/records"));
        request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
        execute(request, apiKey, 201);
        return toFqdn(bunnyName, zoneDomain);
    }

    /**
     * Makes the records of a name and type match the given contents, like a PowerDNS rrset REPLACE.
     * Records which already match are left untouched.
     */
    public String replaceRecords(String baseUrl, Integer port, String apiKey, long zoneId, String zoneDomain,
                                 String recordName, String type, int ttl, List<String> contents) throws DnsProviderException {
        List<String> missing = new ArrayList<>(contents);
        for (JsonNode record : findRecordsByNameAndType(baseUrl, port, apiKey, zoneId, zoneDomain, recordName, type)) {
            if (record.path("Ttl").asInt() != ttl || !missing.remove(formatContent(type, record))) {
                deleteRecord(baseUrl, port, apiKey, zoneId, record.path("Id").asLong());
            }
        }
        for (String content : missing) {
            addRecord(baseUrl, port, apiKey, zoneId, zoneDomain, recordName, type, ttl, content);
        }
        return toFqdn(toBunnyRecordName(recordName, zoneDomain), zoneDomain);
    }

    private void deleteRecord(String baseUrl, Integer port, String apiKey, long zoneId, long recordId) throws DnsProviderException {
        execute(new HttpDelete(buildUrl(baseUrl, port, "/dnszone/" + zoneId + "/records/" + recordId)), apiKey, 204, 404);
    }

    public List<JsonNode> findRecordsByNameAndType(String baseUrl, Integer port, String apiKey, long zoneId,
                                                   String zoneDomain, String recordName, String type) throws DnsProviderException {
        String bunnyName = toBunnyRecordName(recordName, zoneDomain);
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode record : listRecordsRaw(baseUrl, port, apiKey, zoneId, typeCode(type))) {
            if (bunnyName.equalsIgnoreCase(record.path("Name").asText(""))) {
                matches.add(record);
            }
        }
        return matches;
    }

    public String deleteRecordsByNameAndType(String baseUrl, Integer port, String apiKey, long zoneId,
                                             String zoneDomain, String recordName, String type) throws DnsProviderException {
        for (JsonNode record : findRecordsByNameAndType(baseUrl, port, apiKey, zoneId, zoneDomain, recordName, type)) {
            deleteRecord(baseUrl, port, apiKey, zoneId, record.path("Id").asLong());
        }
        return toFqdn(toBunnyRecordName(recordName, zoneDomain), zoneDomain);
    }

    public List<JsonNode> listRecords(String baseUrl, Integer port, String apiKey, long zoneId) throws DnsProviderException {
        return listRecordsRaw(baseUrl, port, apiKey, zoneId, null);
    }

    private List<JsonNode> listRecordsRaw(String baseUrl, Integer port, String apiKey, long zoneId, Integer typeFilter) throws DnsProviderException {
        List<JsonNode> results = new ArrayList<>();
        int page = 1;
        while (true) {
            StringBuilder urlPath = new StringBuilder("/dnszone/" + zoneId + "/records?perPage=" + PAGE_SIZE + "&page=" + page);
            if (typeFilter != null) {
                urlPath.append("&type=").append(typeFilter);
            }
            HttpGet request = new HttpGet(buildUrl(baseUrl, port, urlPath.toString()));
            JsonNode response = execute(request, apiKey, 200);
            JsonNode items = response != null ? response.path("Items") : null;
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    results.add(item);
                }
            }
            if (response == null || !response.path("HasMoreItems").asBoolean(false)) {
                break;
            }
            page++;
        }
        return results;
    }

    private ObjectNode buildRecordBody(String bunnyName, String type, int ttl, String content) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("Name", bunnyName);
        body.put("Type", typeCode(type));
        body.put("Ttl", ttl);
        ContentFields fields = parseContent(type, content);
        body.put("Value", fields.value);
        if (fields.priority != null) {
            body.put("Priority", fields.priority);
        }
        if (fields.weight != null) {
            body.put("Weight", fields.weight);
        }
        if (fields.port != null) {
            body.put("Port", fields.port);
        }
        return body;
    }

    /**
     * MX/SRV contents arrive as PowerDNS-style zonefile text (e.g. "10 mail.example.com" or
     * "10 20 5061 sip.example.com") from DnsProviderUtil.normalizeDnsRecordValue, but Bunny's API
     * wants priority/weight/port as discrete fields with the target alone in Value. Hostnames
     * arrive with a trailing dot, which Bunny does not use.
     */
    static ContentFields parseContent(String type, String content) {
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("DNS record content cannot be empty");
        }
        String trimmed = content.trim();
        switch (type.toUpperCase()) {
            case "MX": {
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid MX content, expected '<priority> <exchange>': " + trimmed);
                }
                return new ContentFields(stripTrailingDot(parts[1]), Integer.parseInt(parts[0]), null, null);
            }
            case "SRV": {
                String[] parts = trimmed.split("\\s+", 4);
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Invalid SRV content, expected '<priority> <weight> <port> <target>': " + trimmed);
                }
                return new ContentFields(stripTrailingDot(parts[3]), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
            case "CNAME":
            case "NS":
            case "PTR":
                return new ContentFields(stripTrailingDot(trimmed), null, null, null);
            default:
                return new ContentFields(trimmed, null, null, null);
        }
    }

    static String formatContent(String type, JsonNode record) {
        String value = record.path("Value").asText("");
        switch (type.toUpperCase()) {
            case "MX":
                return record.path("Priority").asInt(0) + " " + stripTrailingDot(value) + ".";
            case "SRV":
                return record.path("Priority").asInt(0) + " " + record.path("Weight").asInt(0) + " " + record.path("Port").asInt(0) + " " + stripTrailingDot(value) + ".";
            case "CNAME":
            case "NS":
            case "PTR":
                return stripTrailingDot(value) + ".";
            default:
                return value;
        }
    }

    private static String stripTrailingDot(String value) {
        return StringUtils.removeEnd(value, ".");
    }

    static final class ContentFields {
        final String value;
        final Integer priority;
        final Integer weight;
        final Integer port;

        ContentFields(String value, Integer priority, Integer weight, Integer port) {
            this.value = value;
            this.priority = priority;
            this.weight = weight;
            this.port = port;
        }
    }

    String normalizeDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("Domain must not be null or empty");
        }
        String normalized = domain.trim().toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Domain must not be empty");
        }
        return normalized;
    }

    /**
     * Bunny record names are relative to the zone (empty string for the apex), unlike PowerDNS's
     * fully-qualified rrset names. Absolute input (trailing dot, or equal to/suffixed by the zone)
     * has the zone suffix stripped; anything else is assumed already relative.
     */
    String toBunnyRecordName(String recordName, String zoneDomain) {
        if (recordName == null) {
            throw new IllegalArgumentException("Record name must not be null");
        }
        String normalizedZone = normalizeDomain(zoneDomain);
        String name = recordName.trim().toLowerCase();
        if (name.equals("@") || name.isEmpty()) {
            return "";
        }
        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.equals(normalizedZone)) {
            return "";
        }
        if (name.endsWith("." + normalizedZone)) {
            return name.substring(0, name.length() - normalizedZone.length() - 1);
        }
        return name;
    }

    String toFqdn(String bunnyName, String zoneDomain) {
        String normalizedZone = normalizeDomain(zoneDomain);
        if (StringUtils.isBlank(bunnyName)) {
            return normalizedZone;
        }
        return bunnyName + "." + normalizedZone;
    }

    private JsonNode execute(HttpUriRequest request, String apiKey, int... expectedStatus) throws DnsProviderException {
        request.addHeader(ACCESS_KEY_HEADER, apiKey);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");

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
            throw new DnsOperationException("Unexpected Bunny DNS response: HTTP " + status + " Body: " + body);
        } catch (IOException ex) {
            throw new DnsTransportException("Error communicating with Bunny DNS", ex);
        }
    }

    private String buildUrl(String baseUrl, Integer port, String path) {
        String fullUrl = normalizeBaseUrl(baseUrl);
        if (port != null && port > 0) {
            try {
                URI uri = new URI(fullUrl);
                if (uri.getPort() == -1) {
                    fullUrl = fullUrl + ":" + port;
                }
            } catch (URISyntaxException e) {
                fullUrl = fullUrl + ":" + port;
            }
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return fullUrl + path;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = StringUtils.isBlank(baseUrl) ? DEFAULT_BASE_URL : baseUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.warn("Failed to close Bunny DNS HTTP client", e);
        }
    }
}
