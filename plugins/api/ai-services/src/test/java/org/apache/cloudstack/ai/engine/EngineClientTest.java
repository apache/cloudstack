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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Failover and caller context (SPEC.md sections 7 and 10).
 *
 * "The Java plugin accepts multiple engine URLs and fails over between
 * them" — the rule being that only an unreachable replica is worth
 * retrying elsewhere.
 */
class EngineClientTest {

    private static final CallerContext CALLER =
            new CallerContext("u-1", "acme", "/Customers/Acme", "User");

    /** Records every call and answers from a script. */
    private static final class RecordingTransport implements EngineTransport {
        final List<String> urls = new ArrayList<>();
        final List<Map<String, String>> headers = new ArrayList<>();
        final Map<String, EngineResponse> answers;
        final List<String> unreachable;

        RecordingTransport(Map<String, EngineResponse> answers, List<String> unreachable) {
            this.answers = answers;
            this.unreachable = unreachable;
        }

        @Override
        public EngineResponse send(
                String method, String url, Map<String, String> requestHeaders, String body) {
            urls.add(url);
            headers.add(requestHeaders);
            for (String prefix : unreachable) {
                if (url.startsWith(prefix)) {
                    throw new EngineException("connection refused to " + url);
                }
            }
            EngineResponse answer = answers.get(url);
            return answer == null ? new EngineResponse(200, "{}") : answer;
        }
    }

    private static RecordingTransport reachable() {
        return new RecordingTransport(Collections.emptyMap(), Collections.emptyList());
    }

    @Test
    void parsesACommaSeparatedSetting() {
        List<String> urls = EngineClient.parseUrls(
                "https://a.internal:8443, https://b.internal:8443/ ,");

        assertEquals(Arrays.asList("https://a.internal:8443", "https://b.internal:8443"), urls);
    }

    @Test
    void parsingNullYieldsNoUrls() {
        assertTrue(EngineClient.parseUrls(null).isEmpty());
    }

    @Test
    void refusesToBuildWithoutAnyUrl() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new EngineClient(Collections.emptyList(), reachable()));

        assertTrue(failure.getMessage().contains("ai.services.engine.urls"));
    }

    @Test
    void blankUrlsAreIgnored() {
        EngineClient client = new EngineClient(
                Arrays.asList("", "  ", "https://a.internal"), reachable());

        assertEquals(Collections.singletonList("https://a.internal"), client.getBaseUrls());
    }

    @Test
    void callsTheFirstReplica() {
        RecordingTransport transport = reachable();
        EngineClient client = new EngineClient(
                Arrays.asList("https://a.internal", "https://b.internal"), transport);

        client.get("/v1/models", CALLER);

        assertEquals(Collections.singletonList("https://a.internal/v1/models"), transport.urls);
    }

    @Test
    void failsOverWhenTheFirstReplicaCannotBeReached() {
        RecordingTransport transport = new RecordingTransport(
                Collections.emptyMap(), Collections.singletonList("https://a.internal"));
        EngineClient client = new EngineClient(
                Arrays.asList("https://a.internal", "https://b.internal"), transport);

        EngineResponse answer = client.get("/v1/models", CALLER);

        assertTrue(answer.isSuccess());
        assertEquals(
                Arrays.asList("https://a.internal/v1/models", "https://b.internal/v1/models"),
                transport.urls);
    }

    @Test
    void anAnsweringReplicaIsNotRetriedElsewhere() {
        // Retrying a POST against the next replica could do the work twice.
        RecordingTransport transport = new RecordingTransport(
                Collections.singletonMap(
                        "https://a.internal/v1/deployments", new EngineResponse(500, "boom")),
                Collections.emptyList());
        EngineClient client = new EngineClient(
                Arrays.asList("https://a.internal", "https://b.internal"), transport);

        EngineResponse answer = client.post("/v1/deployments", CALLER, "{}");

        assertEquals(500, answer.getStatusCode());
        assertEquals(1, transport.urls.size());
    }

    @Test
    void anEngineErrorFromAReachableReplicaPropagates() {
        EngineTransport failing = (method, url, headers, body) -> {
            throw new EngineException("forbidden", 403, "forbidden");
        };
        EngineClient client = new EngineClient(
                Arrays.asList("https://a.internal", "https://b.internal"), failing);

        EngineException failure = assertThrows(
                EngineException.class, () -> client.get("/v1/runs", CALLER));

        assertEquals(403, failure.getStatusCode());
    }

    @Test
    void everyReplicaUnreachableIsReportedOnce() {
        RecordingTransport transport = new RecordingTransport(
                Collections.emptyMap(), Arrays.asList("https://a.internal", "https://b.internal"));
        EngineClient client = new EngineClient(
                Arrays.asList("https://a.internal", "https://b.internal"), transport);

        EngineException failure = assertThrows(
                EngineException.class, () -> client.get("/v1/models", CALLER));

        assertTrue(failure.isUnreachable());
        assertTrue(failure.getMessage().contains("tried 2"));
        assertEquals(2, transport.urls.size());
    }

    @Test
    void everyRequestCarriesTheCallerContext() {
        RecordingTransport transport = reachable();
        EngineClient client =
                new EngineClient(Collections.singletonList("https://a.internal"), transport);

        client.get("/v1/models", CALLER);

        Map<String, String> sent = transport.headers.get(0);
        assertEquals("u-1", sent.get(CallerContext.USER_HEADER));
        assertEquals("acme", sent.get(CallerContext.ACCOUNT_HEADER));
        assertEquals("/Customers/Acme", sent.get(CallerContext.DOMAIN_HEADER));
        assertEquals("User", sent.get(CallerContext.ROLE_HEADER));
    }

    @Test
    void trailingSlashesDoNotDoubleUp() {
        RecordingTransport transport = reachable();
        EngineClient client =
                new EngineClient(Collections.singletonList("https://a.internal/"), transport);

        client.get("/v1/models", CALLER);

        assertEquals("https://a.internal/v1/models", transport.urls.get(0));
    }

    @Test
    void everyVerbReachesTheTransport() {
        RecordingTransport transport = reachable();
        EngineClient client =
                new EngineClient(Collections.singletonList("https://a.internal"), transport);

        client.get("/v1/a", CALLER);
        client.post("/v1/b", CALLER, "{}");
        client.patch("/v1/c", CALLER, "{}");
        client.delete("/v1/d", CALLER);

        assertEquals(4, transport.urls.size());
    }

    @Test
    void acceptedIsRecognisedForAsyncCommands() {
        assertTrue(new EngineResponse(202, "{\"jobid\":\"x\"}").isAccepted());
        assertFalse(new EngineResponse(200, "{}").isAccepted());
    }
}
