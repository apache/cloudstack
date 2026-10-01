/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal client for the parts of the RGW admin ops API that radosgw-admin4j does not cover:
 * the account endpoint ({@code /admin/account}, Ceph Squid v19+), the {@code /admin/info}
 * probe and the caller's own capabilities. Requests are signed the same way the library signs
 * its admin calls (AWS signature version 2 over the canonical resource path), so the same
 * admin credentials work for both.
 *
 * Version notes, verified against 18.2.7 (Reef), 19.2.3 (Squid) and 20.2.4 (Tentacle):
 * Reef answers 405 on the account endpoint and does not serve {@code /admin/info}; Squid serves
 * both but its account GET (and DELETE) answer 403 regardless of capabilities, while POST works
 * and a duplicate POST answers 409 {@code AccountAlreadyExists}. Everything here therefore avoids
 * account GETs: the account id is chosen by the caller and creation is idempotent on 409.
 */
public class RgwAccountClient {

    private static final Logger LOGGER = LogManager.getLogger(RgwAccountClient.class);
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final int TIMEOUT_MILLIS = 30000;
    private static final BigInteger ACCOUNT_ID_SPACE = BigInteger.TEN.pow(17);

    private final String adminEndpoint;
    private final String accessKey;
    private final String secretKey;

    public static class RgwAccount {
        private final String id;
        private final String name;

        public RgwAccount(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * @param adminEndpoint the admin base URL, for example {@code http://rgw:8000/admin}
     */
    public RgwAccountClient(String adminEndpoint, String accessKey, String secretKey) {
        this.adminEndpoint = adminEndpoint.endsWith("/") ? adminEndpoint.substring(0, adminEndpoint.length() - 1) : adminEndpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * RGW account ids must be {@code RGW} followed by 17 digits. Deriving them from the
     * CloudStack account UUID makes account creation idempotent without any lookup.
     */
    public static String accountIdFor(String cloudStackAccountUuid) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cloudStackAccountUuid.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, digest).mod(ACCOUNT_ID_SPACE);
            return String.format("RGW%017d", number);
        } catch (GeneralSecurityException e) {
            throw new CloudRuntimeException("Unable to derive an RGW account id", e);
        }
    }

    /**
     * Whether this gateway serves the account API: {@code /admin/info} exists from Squid on,
     * and a gateway without accounts answers 405 on the account endpoint.
     */
    public boolean isAvailable() {
        if (call("GET", "info", new LinkedHashMap<>()).status != 200) {
            return false;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "cloudstack-probe");
        return call("GET", "account", params).status != 405;
    }

    /**
     * Why this gateway and credential cannot drive per-bucket credentials, or {@code null} when
     * they can. The three causes look identical from the outside, and an operator who reads
     * "unsupported" tends to blame the Ceph version when the real cause is a missing capability
     * on the store's admin credential, so they are told apart here.
     */
    public String unsupportedReason() {
        Response info = call("GET", "info", new LinkedHashMap<>());
        if (info.status == 403) {
            return "the object store's admin credential is missing the 'info' capability";
        }
        if (info.status == 404 || info.status == 405) {
            return "this gateway does not serve /admin/info, so it predates RGW accounts (Ceph Squid or later is required)";
        }
        if (info.status != 200) {
            return "the gateway answered HTTP " + info.status + " on /admin/info";
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "cloudstack-probe");
        if (call("GET", "account", params).status == 405) {
            return "this gateway does not implement RGW accounts (Ceph Squid or later is required)";
        }
        if (!hasAccountsWriteCapability()) {
            return "the object store's admin credential is missing the 'accounts' capability";
        }
        return null;
    }

    /**
     * Whether the admin credential holds write permission on the {@code accounts} capability,
     * which the account endpoint requires.
     */
    public boolean hasAccountsWriteCapability() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("access-key", accessKey);
        Response response = call("GET", "user", params);
        if (response.status != 200) {
            LOGGER.debug("Unable to read the admin user's capabilities: HTTP {}", response.status);
            return false;
        }
        JsonElement caps = JsonParser.parseString(response.body).getAsJsonObject().get("caps");
        if (caps == null || !caps.isJsonArray()) {
            return false;
        }
        for (JsonElement cap : caps.getAsJsonArray()) {
            JsonObject entry = cap.getAsJsonObject();
            String type = entry.has("type") ? entry.get("type").getAsString() : "";
            String perm = entry.has("perm") ? entry.get("perm").getAsString() : "";
            if ("accounts".equals(type) && (perm.contains("write") || perm.contains("*"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the RGW user is the root user of an RGW account (rather than a plain, legacy user).
     * Read from the gateway itself, so it stays correct even if CloudStack's records are stale.
     * Answers false only when the gateway is certain (200 for a plain user, 404 for no such user);
     * when it cannot be asked this throws rather than reporting a migrated account as legacy.
     */
    public boolean isAccountRootUser(String uid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("uid", uid);
        Response response = call("GET", "user", params);
        if (response.status == 404) {
            // the gateway is certain: there is no such user, so it is certainly not an account root
            return false;
        }
        if (response.status != 200) {
            // anything else (403 on a capability problem, a gateway error) means we cannot tell.
            // Saying "not a root" here would downgrade a migrated account, so refuse to answer.
            throw new CloudRuntimeException("Unable to read RGW user " + uid + ": HTTP " + response.status + " " + response.body);
        }
        JsonObject user = JsonParser.parseString(response.body).getAsJsonObject();
        JsonElement accountId = user.get("account_id");
        JsonElement type = user.get("type");
        return accountId != null && !accountId.isJsonNull() && !accountId.getAsString().isEmpty()
                && type != null && !type.isJsonNull() && "root".equals(type.getAsString());
    }

    /**
     * Create the account, or return it unchanged if an account with this id or name exists.
     */
    public RgwAccount createAccount(String id, String name) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("name", name);
        Response response = call("POST", "account", params);
        if (response.status == 409) {
            // Either our id already exists (a retry) or the name is taken by an account with
            // another id. Resolve the id by name where the gateway allows it (Squid answers 403
            // to account GETs, in which case the id we chose is the only sensible answer).
            Map<String, String> lookup = new LinkedHashMap<>();
            lookup.put("name", name);
            Response existing = call("GET", "account", lookup);
            if (existing.status == 200) {
                JsonObject json = JsonParser.parseString(existing.body).getAsJsonObject();
                String existingId = json.get("id").getAsString();
                if (!existingId.equals(id)) {
                    LOGGER.info("RGW account named {} already exists with id {}; using it", name, existingId);
                }
                return new RgwAccount(existingId, name);
            }
            LOGGER.debug("RGW account {} ({}) already exists", id, name);
            return new RgwAccount(id, name);
        }
        response.ensureOk("create RGW account " + name);
        JsonObject json = JsonParser.parseString(response.body).getAsJsonObject();
        return new RgwAccount(json.get("id").getAsString(), json.has("name") ? json.get("name").getAsString() : name);
    }

    private Response call(String method, String resource, Map<String, String> params) {
        String path = adminEndpoint.replaceFirst("^https?://[^/]+", "") + "/" + resource;
        StringBuilder query = new StringBuilder("?format=json");
        for (Map.Entry<String, String> param : params.entrySet()) {
            query.append('&').append(param.getKey()).append('=').append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }
        String date = RFC_1123.format(ZonedDateTime.now(ZoneId.of("GMT")));
        // HttpURLConnection adds a form content type to any request with a body; the content
        // type is part of the SigV2 string to sign, so set it explicitly and sign with it.
        String contentType = "POST".equals(method) ? "application/x-www-form-urlencoded" : "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(adminEndpoint + "/" + resource + query).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Date", date);
            connection.setRequestProperty("Authorization", "AWS " + accessKey + ":" + sign(method + "\n\n" + contentType + "\n" + date + "\n" + path));
            if ("POST".equals(method)) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(0);
                connection.getOutputStream().close();
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            // a user record carries the user's S3 secret keys: never let it reach a log
            LOGGER.trace("RGW admin API {} {} -> {} {}", method, path + query, status,
                    "user".equals(resource) && status == 200 ? "(body withheld: contains credentials)" : body);
            return new Response(status, body);
        } catch (IOException e) {
            throw new CloudRuntimeException("RGW admin API request failed: " + method + " " + path, e);
        }
    }

    private String sign(String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new CloudRuntimeException("Unable to sign RGW admin request", e);
        }
    }

    private static class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        void ensureOk(String operation) {
            if (status < 200 || status >= 300) {
                throw new CloudRuntimeException("Unable to " + operation + ": HTTP " + status + " " + body);
            }
        }
    }
}
