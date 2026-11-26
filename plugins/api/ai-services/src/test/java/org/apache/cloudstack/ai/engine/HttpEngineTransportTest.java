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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The HTTP transport, against a real socket.
 *
 * Driven over plain HTTP with the JDK's own server: mTLS is configured in
 * {@link MutualTls} and tested there, while what matters here is that the
 * request is built correctly and that an unreachable replica raises the
 * unreachable form of {@link EngineException} — which is the only thing
 * {@link EngineClient} fails over on.
 */
class HttpEngineTransportTest {

    private HttpServer server;
    private HttpEngineTransport transport;
    private final List<String> methods = new ArrayList<>();
    private final List<String> paths = new ArrayList<>();
    private final List<Headers> headers = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private String responseBody = "{\"ok\": true}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::record);
        server.start();
        transport = new HttpEngineTransport(HttpClient.newHttpClient(), Duration.ofSeconds(5));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void record(HttpExchange exchange) throws IOException {
        methods.add(exchange.getRequestMethod());
        paths.add(exchange.getRequestURI().getPath());
        headers.add(exchange.getRequestHeaders());
        bodies.add(new String(
                exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status.get(), payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static Map<String, String> caller() {
        return new CallerContext("u-1", "acme", "/Customers/Acme", "User").asHeaders();
    }

    @Test
    void sendsTheMethodAndPath() {
        transport.send("GET", url("/v1/models"), caller(), null);

        assertEquals("GET", methods.get(0));
        assertEquals("/v1/models", paths.get(0));
    }

    @Test
    void sendsTheCallerContextHeaders() {
        transport.send("GET", url("/v1/models"), caller(), null);

        assertEquals("acme", headers.get(0).getFirst(CallerContext.ACCOUNT_HEADER));
        assertEquals("User", headers.get(0).getFirst(CallerContext.ROLE_HEADER));
    }

    @Test
    void returnsTheStatusAndBody() {
        status.set(202);
        responseBody = "{\"jobid\": \"abc\"}";

        EngineResponse answer = transport.send("POST", url("/v1/models/mirror"), caller(), "{}");

        assertEquals(202, answer.getStatusCode());
        assertTrue(answer.isAccepted());
        assertTrue(answer.getBody().contains("abc"));
    }

    @Test
    void sendsABodyWithAJsonContentType() {
        transport.send("POST", url("/v1/deployments"), caller(), "{\"model\": \"m\"}");

        assertEquals("{\"model\": \"m\"}", bodies.get(0));
        assertEquals("application/json", headers.get(0).getFirst("Content-Type"));
    }

    @Test
    void sendsNoContentTypeWithoutABody() {
        transport.send("GET", url("/v1/models"), caller(), null);

        assertEquals(null, headers.get(0).getFirst("Content-Type"));
    }

    @Test
    void anErrorStatusIsReturnedRatherThanThrown() {
        // The engine answered; EngineClient must not fail over for this.
        status.set(503);

        EngineResponse answer = transport.send("GET", url("/v1/models"), caller(), null);

        assertEquals(503, answer.getStatusCode());
        assertTrue(!answer.isSuccess());
    }

    @Test
    void anUnreachableReplicaRaisesTheUnreachableForm() {
        // Port 1 on loopback refuses connections.
        EngineException failure = assertThrows(
                EngineException.class,
                () -> transport.send("GET", "http://127.0.0.1:1/v1/models", caller(), null));

        assertTrue(failure.isUnreachable(), "must be unreachable so the client fails over");
        assertTrue(failure.getMessage().contains("could not reach engine"));
    }

    @Test
    void failoverWorksEndToEndThroughTheRealTransport() {
        EngineClient client = new EngineClient(
                java.util.Arrays.asList(
                        "http://127.0.0.1:1",
                        "http://127.0.0.1:" + server.getAddress().getPort()),
                transport);

        EngineResponse answer = client.get(
                "/v1/models", new CallerContext("u", "a", "/d", "Admin"));

        assertTrue(answer.isSuccess());
        assertEquals(Collections.singletonList("/v1/models"), paths);
    }

    @Test
    void everyVerbReachesTheServer() {
        transport.send("GET", url("/v1/a"), caller(), null);
        transport.send("POST", url("/v1/b"), caller(), "{}");
        transport.send("PATCH", url("/v1/c"), caller(), "{}");
        transport.send("DELETE", url("/v1/d"), caller(), null);

        assertEquals(java.util.Arrays.asList("GET", "POST", "PATCH", "DELETE"), methods);
    }

    @Test
    void theTransportIsConstructibleFromConfiguredTimeouts() {
        assertNotNull(new HttpEngineTransport(HttpClient.newHttpClient(), Duration.ofSeconds(1)));
    }

    @Test
    void anUnreachableEngineSaysSomethingWhenTheCauseWillNot() {
        // Measured by killing both replicas: an operator was told
        //
        //   no AI Services engine replica answered (tried 2): could
        //   not reach engine at https://…/v1/models: null
        //
        // because the IOException behind a refused connection can
        // carry no message at all, and "null" is what that renders
        // as. The class name is not much, but it is a fact about
        // what happened.
        IOException silent = new IOException();

        String described = HttpEngineTransport.describe(silent);

        assertFalse(described.contains("null"), described);
        assertTrue(described.contains("IOException"), described);
    }

    @Test
    void aCauseThatSpeaksForItselfIsLeftAlone() {
        IOException refused = new IOException("Connection refused");

        assertEquals("Connection refused", HttpEngineTransport.describe(refused));
    }
}
