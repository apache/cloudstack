// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Calls the AI Services engine REST API (SPEC.md sections 7 and 10).
 *
 * Two behaviours matter and are the reason this is a class rather than a
 * bare HTTP call:
 *
 * <ul>
 *   <li><b>Failover.</b> "The Java plugin accepts multiple engine URLs and
 *       fails over between them" (section 7). A replica that cannot be
 *       reached is skipped; a replica that answers — even with an error —
 *       has answered, and its answer is returned rather than retried
 *       elsewhere, because retrying a POST against the next replica could
 *       do the work twice.</li>
 *   <li><b>Caller context.</b> Every request carries the CloudStack
 *       session's identity, which the engine filters responses by.</li>
 * </ul>
 *
 * The transport is injected so the failover logic can be tested without a
 * socket; {@link HttpEngineTransport} is the mTLS implementation.
 */
public class EngineClient {

    private final List<String> baseUrls;
    private final EngineTransport transport;

    public EngineClient(List<String> baseUrls, EngineTransport transport) {
        List<String> cleaned = new ArrayList<>();
        for (String url : baseUrls) {
            if (url != null && !url.trim().isEmpty()) {
                cleaned.add(stripTrailingSlash(url.trim()));
            }
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one engine URL is required; set ai.services.engine.urls");
        }
        this.baseUrls = Collections.unmodifiableList(cleaned);
        this.transport = transport;
    }

    /** Parses the comma-separated ai.services.engine.urls setting. */
    public static List<String> parseUrls(String setting) {
        List<String> urls = new ArrayList<>();
        if (setting == null) {
            return urls;
        }
        for (String part : setting.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                urls.add(stripTrailingSlash(trimmed));
            }
        }
        return urls;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public List<String> getBaseUrls() {
        return baseUrls;
    }

    public EngineResponse get(String path, CallerContext caller) {
        return send("GET", path, caller, null);
    }

    public EngineResponse post(String path, CallerContext caller, String body) {
        return send("POST", path, caller, body);
    }

    public EngineResponse patch(String path, CallerContext caller, String body) {
        return send("PATCH", path, caller, body);
    }

    public EngineResponse delete(String path, CallerContext caller) {
        return send("DELETE", path, caller, null);
    }

    /**
     * Try each replica in turn until one answers.
     *
     * An answer is final, whatever its status: only an unreachable replica
     * is worth trying the next one for. A 500 from the engine means the
     * engine decided something, and repeating a state-changing call
     * against another replica could apply it twice.
     */
    private EngineResponse send(
            String method, String path, CallerContext caller, String body) {
        Map<String, String> headers = caller.asHeaders();
        EngineException lastFailure = null;

        for (String baseUrl : baseUrls) {
            try {
                return transport.send(method, baseUrl + path, headers, body);
            } catch (EngineException failure) {
                if (!failure.isUnreachable()) {
                    throw failure;
                }
                lastFailure = failure;
            }
        }

        throw new EngineException(
                "no AI Services engine replica answered (tried " + baseUrls.size()
                        + "): " + (lastFailure == null ? "unknown" : lastFailure.getMessage()),
                lastFailure);
    }
}
