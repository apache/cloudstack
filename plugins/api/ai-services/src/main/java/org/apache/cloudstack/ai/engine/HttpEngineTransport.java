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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * The mTLS transport to the engine (SPEC.md section 11).
 *
 * The engine trusts the caller context this plugin asserts, so the client
 * certificate is what earns that trust. A missing certificate is a
 * configuration error rather than a reason to fall back to plain TLS:
 * falling back would let anything that can reach the engine claim to be
 * any tenant.
 */
public class HttpEngineTransport implements EngineTransport {

    private final HttpClient client;
    private final Duration timeout;

    public HttpEngineTransport(
            Integer timeoutSeconds,
            String clientCert,
            String clientKey,
            String trustStore,
            String trustStorePassword) {
        this.timeout = Duration.ofSeconds(timeoutSeconds == null ? 30 : timeoutSeconds);
        this.client = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .sslContext(MutualTls.contextFor(
                        clientCert, clientKey, trustStore, trustStorePassword))
                .build();
    }

    /** For tests, which supply their own client. */
    HttpEngineTransport(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    @Override
    public EngineResponse send(
            String method, String url, Map<String, String> headers, String body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .method(method, publisher);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.header(header.getKey(), header.getValue());
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }

        try {
            HttpResponse<String> response =
                    client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new EngineResponse(response.statusCode(), response.body());
        } catch (IOException problem) {
            // Status 0: unreachable, so EngineClient tries the next replica.
            throw new EngineException(
                    "could not reach engine at " + url + ": " + describe(problem), problem);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new EngineException("interrupted calling engine at " + url, interrupted);
        }
    }

    /**
     * What to tell an operator about a failure that may say nothing.
     *
     * The IOException behind a refused connection can carry no
     * message, and an operator who killed both replicas was told
     * "could not reach engine at https://…/v1/models: null". The
     * class name is not much, but it is a fact about what happened,
     * and the cause is attached to the exception either way.
     */
    static String describe(Exception problem) {
        String said = problem.getMessage();
        if (said == null || said.trim().isEmpty()) {
            return problem.getClass().getSimpleName();
        }
        return said;
    }
}
